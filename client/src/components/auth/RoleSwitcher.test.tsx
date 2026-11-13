import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { meQueryKey } from '@/generated/@tanstack/react-query.gen'
import type { RoleChoice } from '@/lib/auth-route-guards'
import { RoleSwitcher } from './RoleSwitcher'

const navigation = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(), useNavigate: () => navigation,
  Link: ({ children, to, ...props }: { children?: ReactNode; to: string }) => <a {...props} href={to}>{children}</a>,
}))
let api: ReturnType<typeof apiHarness>
let fails: boolean
let logoutFails: boolean
beforeEach(() => {
  fails = false; logoutFails = false; navigation.mockReset(); localStorage.clear(); sessionStorage.clear()
  api = apiHarness(async request => {
    if (new URL(request.url).pathname === '/auth/session/logout') return logoutFails ? Response.json({ message: 'Logout unavailable' }, { status: 503 }) : Response.json({})
    const { role } = await request.json() as { role: string }
    return fails ? Response.json({ message: 'Rol no permitido' }, { status: 403 }) : Response.json({ user: { id: 2, username: 'ana', name: 'Ana', activeRole: role, roles: ['CIUDADANO', 'ADMIN'] } })
  })
})
afterEach(() => { api.restore(); localStorage.clear(); sessionStorage.clear() })
const user = { id: 2, name: 'Ana', roles: ['CIUDADANO', 'ADMIN'], activeRole: 'CIUDADANO' }
const initial: RoleChoice = { mode: 'select', pending: { user } }
const change: RoleChoice = { mode: 'switch', user }

it.each([{ choice: initial, label: /Portal ciudadano/, path: 'select-role', role: 'CIUDADANO', home: '/portal' }, { choice: change, label: /Administración/, path: 'switch-role', role: 'ADMIN', home: '/gestion' }])('elige $role con $path y limpia datos privados del rol anterior', async ({ choice, label, path, role, home }) => {
  api.queryClient.setQueryData(['private-old-role'], { secret: 'previous role' })
  localStorage.setItem('auth-token', 'legacy'); sessionStorage.setItem('role-selection', 'legacy')
  api.render(<RoleSwitcher roleChoice={choice} />)
  await userEvent.click(screen.getByRole('button', { name: label }))
  await waitFor(() => expect(navigation).toHaveBeenCalledWith({ to: home, replace: true }))
  expect(api.requests).toHaveLength(1)
  expect(api.requests[0].method).toBe('POST')
  expect(new URL(api.requests[0].url).pathname).toBe(`/auth/session/${path}`)
  expect(await api.requests[0].json()).toEqual({ role })
  expect(api.queryClient.getQueryData(['private-old-role'])).toBeUndefined()
  expect(api.queryClient.getQueryData(meQueryKey())).toMatchObject({ user: { activeRole: role } })
  expect(localStorage.getItem('auth-token')).toBeNull()
  expect(sessionStorage.getItem('role-selection')).toBeNull()
})

it('marca el rol activo y permite conservarlo sin pedir un nuevo cambio al backend', async () => {
  api.render(<RoleSwitcher roleChoice={change} />)
  expect(screen.getByRole('button', { name: /Portal ciudadano/ })).toBeDisabled()
  expect(screen.getByText('Actual')).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Conservar rol actual' }))
  expect(navigation).toHaveBeenCalledWith({ to: '/portal', replace: true })
  expect(api.requests).toHaveLength(0)
})

it('presenta el rechazo sin navegar ni borrar la caché y permite reintentar', async () => {
  fails = true
  api.queryClient.setQueryData(['private-old-role'], { retained: true })
  api.render(<RoleSwitcher roleChoice={change} />)
  await userEvent.click(screen.getByRole('button', { name: /Administración/ }))
  expect(await screen.findByText('Rol no permitido')).toBeInTheDocument()
  expect(navigation).not.toHaveBeenCalled()
  expect(api.queryClient.getQueryData(['private-old-role'])).toEqual({ retained: true })
  fails = false
  await userEvent.click(screen.getByRole('button', { name: /Administración/ }))
  await waitFor(() => expect(navigation).toHaveBeenCalledWith({ to: '/gestion', replace: true }))
})

it('cancela la selección inicial cerrando la sesión y limpiando datos privados', async () => {
  api.queryClient.setQueryData(['private-old-role'], { retained: true })
  api.render(<RoleSwitcher roleChoice={initial} />)
  await userEvent.click(screen.getByRole('button', { name: 'Cancelar y volver al inicio de sesión' }))
  await waitFor(() => expect(navigation).toHaveBeenCalledWith({ to: '/login', replace: true }))
  expect(new URL(api.requests[0].url).pathname).toBe('/auth/session/logout')
  expect(api.queryClient.getQueryData(['private-old-role'])).toBeUndefined()
})

it('mantiene la selección y la caché si falla el cierre de sesión', async () => {
  logoutFails = true
  api.queryClient.setQueryData(['private-old-role'], { retained: true })
  api.render(<RoleSwitcher roleChoice={initial} />)
  await userEvent.click(screen.getByRole('button', { name: 'Cancelar y volver al inicio de sesión' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Cancelar y volver al inicio de sesión' })).toBeEnabled())
  expect(navigation).not.toHaveBeenCalled()
  expect(api.queryClient.getQueryData(['private-old-role'])).toEqual({ retained: true })
})

it('muestra una etiqueta legible para roles nuevos y omite roles vacíos', () => {
  api.render(<RoleSwitcher roleChoice={{ mode: 'select', pending: { user: { name: 'Ana', roles: ['NUEVO_ROL', ''] } } }} />)
  expect(screen.getByRole('button', { name: /Nuevo Rol/ })).toBeInTheDocument()
  expect(screen.getAllByText(/Herramientas municipales habilitadas para el rol/)).toHaveLength(1)
})
