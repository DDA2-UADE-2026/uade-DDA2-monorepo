import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { client } from '@/generated/client.gen'
import { meQueryKey } from '@/generated/@tanstack/react-query.gen'
import { queryClient } from '@/router'
import { apiHarness } from '@/test/api-harness'
import { registerAuthInterceptor } from './auth-interceptor'

const state = vi.hoisted(() => ({ navigate: vi.fn() }))
vi.mock('@/router', async () => {
  const { QueryClient } = await import('@tanstack/react-query')
  return { queryClient: new QueryClient(), router: { navigate: state.navigate } }
})
let api: ReturnType<typeof apiHarness>
let responseStatus: number
beforeEach(() => {
  responseStatus = 401
  api = apiHarness(() => Response.json({ status: responseStatus }, { status: responseStatus }))
  state.navigate.mockReset()
  vi.spyOn(toast, 'error').mockImplementation(() => 'session-expired')
  localStorage.setItem('auth-token', 'legacy')
  localStorage.setItem('current-user-avatar', 'old-avatar')
  sessionStorage.setItem('role-selection', 'legacy')
  queryClient.setQueryData(meQueryKey(), { user: { id: 2 } })
  registerAuthInterceptor()
})
afterEach(() => {
  api.restore(); queryClient.clear(); client.interceptors.response.clear()
  localStorage.clear(); sessionStorage.clear(); vi.restoreAllMocks()
})

it.each(['/api/programs', '/auth/session/switch-role'])('redirige una sesión vencida en %s, limpia datos heredados y conserva la caché hasta que la ruta gestione el cambio', async url => {
  const response = await client.get({ url })
  expect(response.response?.status).toBe(401)
  expect(state.navigate).toHaveBeenCalledWith({ to: '/login', replace: true })
  expect(localStorage.getItem('auth-token')).toBeNull()
  expect(localStorage.getItem('current-user-avatar')).toBeNull()
  expect(sessionStorage.getItem('role-selection')).toBeNull()
  expect(queryClient.getQueryData(meQueryKey())).toEqual({ user: { id: 2 } })
  expect(toast.error).toHaveBeenCalledWith('Tu sesión expiró', expect.objectContaining({ id: 'session-expired' }))
})

it.each(['/auth/session/login', '/auth/session/select-role', '/auth/session/role-selection'])('no confunde un rechazo de ingreso %s con una sesión expirada', async url => {
  await client.get({ url })
  expect(state.navigate).not.toHaveBeenCalled()
  expect(toast.error).not.toHaveBeenCalled()
  expect(localStorage.getItem('auth-token')).toBe('legacy')
})

it('no redirige al visitante anónimo por un 401 si no tenía sesión en caché', async () => {
  queryClient.clear()
  await client.get({ url: '/api/programs' })
  expect(state.navigate).not.toHaveBeenCalled()
  expect(toast.error).not.toHaveBeenCalled()
})

it.each([200, 403, 500])('no cierra la sesión por una respuesta HTTP %s', async status => {
  responseStatus = status
  await client.get({ url: '/api/programs' })
  expect(state.navigate).not.toHaveBeenCalled()
  expect(localStorage.getItem('current-user-avatar')).toBe('old-avatar')
})
