import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { client } from '@/generated/client.gen'
import { meQueryKey } from '@/generated/@tanstack/react-query.gen'
import { registerCookieAuthTransport } from '@/lib/cookie-auth-transport'
import { getAuthenticatedUser, requireRoleChoice } from '@/lib/auth-route-guards'
import { useLogin, useLogout, useSelectRole, useSwitchRole } from './use-auth'

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }))
vi.mock('@/lib/avatar', () => ({ clearCurrentUserAvatar: vi.fn(), saveCurrentUserAvatar: vi.fn() }))

let queryClient: QueryClient
let dispose: () => void
let requests: Request[]
let activeRole: string | undefined
let pendingSelection: boolean
let multipleRoles: boolean
let failLogout: boolean
let failCsrf: boolean

const roles = ['CIUDADANO', 'AUDITOR']
const user = () => ({ id: 42, username: 'test', name: 'Test', roles, activeRole, permissions: [] })
const json = (body: unknown, status = 200) => Response.json(body, { status })
const unauthorized = () => json({ status: 401, code: 'AUTH_UNAUTHENTICATED' }, 401)

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  requests = []
  activeRole = undefined
  pendingSelection = false
  multipleRoles = false
  failLogout = false
  failCsrf = false
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  client.setConfig({ baseUrl: 'http://localhost:8080', credentials: 'include' })
  dispose = registerCookieAuthTransport(client)
  vi.stubGlobal('fetch', vi.fn(async (request: Request) => {
    requests.push(request.clone())
    expect(request.credentials).toBe('include')
    expect(request.headers.has('Authorization')).toBe(false)
    const path = new URL(request.url).pathname
    if (path === '/auth/session/csrf') {
      if (failCsrf) throw new TypeError('Network unavailable')
      return json({ headerName: 'X-XSRF-TOKEN', token: 'csrf-current' })
    }
    if (request.method !== 'GET') expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-current')
    if (path === '/auth/session/login') {
      pendingSelection = multipleRoles
      activeRole = multipleRoles ? undefined : 'CIUDADANO'
      return json({ user: user(), requiresRoleSelection: multipleRoles, expiresIn: multipleRoles ? 300 : 28800 })
    }
    if (path === '/auth/session/role-selection') {
      return pendingSelection ? json({ user: user(), requiresRoleSelection: true }) : unauthorized()
    }
    if (path === '/auth/session/select-role' || path === '/auth/session/switch-role') {
      const body = await request.json()
      expect(Object.keys(body)).toEqual(['role'])
      activeRole = body.role
      pendingSelection = false
      return json({ user: user(), requiresRoleSelection: false, expiresIn: 28800 })
    }
    if (path === '/auth/me') return activeRole ? json({ user: user() }) : unauthorized()
    if (path === '/auth/session/logout') {
      if (failLogout) throw new TypeError('Network unavailable')
      activeRole = undefined
      pendingSelection = false
      return new Response(null, { status: 204 })
    }
    throw new Error(`Unexpected request: ${path}`)
  }))
})

afterEach(() => {
  dispose()
  queryClient.clear()
  vi.unstubAllGlobals()
})

describe('cookie authentication', () => {
  it('login, reload, switch role and logout work without any JWT in browser storage', async () => {
    localStorage.setItem('auth-token', 'legacy-jwt')
    sessionStorage.setItem('role-selection', 'legacy-selection')
    const login = renderHook(useLogin, { wrapper })
    await act(async () => { await login.result.current.mutateAsync({ body: { username: 'test', password: 'secret' } }) })
    expect(localStorage.getItem('auth-token')).toBeNull()
    expect(sessionStorage.getItem('role-selection')).toBeNull()
    expect(queryClient.getQueryData(meQueryKey())).toMatchObject({ user: { activeRole: 'CIUDADANO' } })

    queryClient.clear() // Reload: no local token and no query cache.
    await expect(getAuthenticatedUser(queryClient)).resolves.toMatchObject({ activeRole: 'CIUDADANO' })
    queryClient.setQueryData(['citizen-private'], { private: true })
    const switchRole = renderHook(useSwitchRole, { wrapper })
    await act(async () => { await switchRole.result.current.mutateAsync({ body: { role: 'AUDITOR' } }) })
    expect(queryClient.getQueryData(['citizen-private'])).toBeUndefined()
    expect(queryClient.getQueryData(meQueryKey())).toMatchObject({ user: { activeRole: 'AUDITOR' } })

    const logout = renderHook(useLogout, { wrapper })
    await act(async () => { await logout.result.current.logout() })
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
    await expect(getAuthenticatedUser(queryClient)).resolves.toBeNull()
    expect(requests.filter(r => r.method === 'POST').map(r => new URL(r.url).pathname)).toEqual([
      '/auth/session/login', '/auth/session/switch-role', '/auth/session/logout',
    ])
  })

  it('recovers pending role selection from the server and submits only the chosen role', async () => {
    multipleRoles = true
    const login = renderHook(useLogin, { wrapper })
    await act(async () => { await login.result.current.mutateAsync({ body: { username: 'test', password: 'secret' } }) })
    expect(queryClient.getQueryData(meQueryKey())).toBeUndefined()
    queryClient.clear()
    await expect(requireRoleChoice(queryClient)).resolves.toMatchObject({ mode: 'select', pending: { user: { roles } } })
    const select = renderHook(useSelectRole, { wrapper })
    await act(async () => { await select.result.current.mutateAsync({ body: { role: 'CIUDADANO' } }) })
    expect(queryClient.getQueryData(meQueryKey())).toMatchObject({ user: { activeRole: 'CIUDADANO' } })
    expect(sessionStorage.getItem('role-selection')).toBeNull()
  })

  it('keeps local session data if server logout fails, then permits retry', async () => {
    activeRole = 'CIUDADANO'
    queryClient.setQueryData(meQueryKey(), { user: user() })
    failLogout = true
    const logout = renderHook(useLogout, { wrapper })
    await act(async () => { await expect(logout.result.current.logout()).rejects.toThrow('Network unavailable') })
    expect(queryClient.getQueryData(meQueryKey())).toBeDefined()
    failLogout = false
    await act(async () => { await logout.result.current.logout() })
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
  })

  it('does not send credentials when the CSRF bootstrap fails and permits retry', async () => {
    failCsrf = true
    const login = renderHook(useLogin, { wrapper })
    await act(async () => {
      await expect(login.result.current.mutateAsync({ body: { username: 'test', password: 'secret' } })).rejects.toThrow('Network unavailable')
    })
    expect(requests.some(r => r.method === 'POST')).toBe(false)
    failCsrf = false
    await act(async () => { await login.result.current.mutateAsync({ body: { username: 'test', password: 'secret' } }) })
    await waitFor(() => expect(login.result.current.isSuccess).toBe(true))
  })
})
