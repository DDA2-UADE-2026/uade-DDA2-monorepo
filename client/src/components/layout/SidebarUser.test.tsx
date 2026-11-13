import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { SidebarMenu, SidebarProvider } from '@/components/ui/sidebar'
import { ThemeProviderContext } from '@/context/theme-context'
import { SidebarUser } from './SidebarUser'

const state = vi.hoisted(() => ({ pathname: '/portal', user: { name: 'Ana', username: 'ana', email: 'ana@example.test', activeRole: 'CIUDADANO', roles: ['CIUDADANO', 'ADMIN'] }, pending: false, logout: vi.fn(), navigate: vi.fn() }))
vi.mock('@/hooks/use-auth', () => ({ useMe: () => ({ data: { user: state.user } }), useLogout: () => ({ logout: state.logout, isPending: state.pending }) }))
vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  useLocation: () => ({ pathname: state.pathname }), useNavigate: () => state.navigate,
  Link: ({ children, to, ...props }: { children?: ReactNode; to: string }) => <a {...props} href={to}>{children}</a>,
}))
const setTheme = vi.fn()
beforeEach(() => {
  state.pathname = '/portal'; state.pending = false
  state.user = { name: 'Ana', username: 'ana', email: 'ana@example.test', activeRole: 'CIUDADANO', roles: ['CIUDADANO', 'ADMIN'] }
  state.logout.mockReset().mockResolvedValue(undefined); state.navigate.mockReset(); setTheme.mockReset()
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
})
afterEach(() => vi.unstubAllGlobals())
async function open() {
  render(<ThemeProviderContext.Provider value={{ theme: 'system', setTheme }}><SidebarProvider><SidebarMenu><SidebarUser /></SidebarMenu></SidebarProvider></ThemeProviderContext.Provider>)
  screen.getByRole('button', { name: /Ana|ana|Usuario/ }).focus()
  await userEvent.keyboard('{ArrowDown}')
  await screen.findByRole('menu')
}

it.each([{ path: '/portal/programas', profile: '/portal/perfil' }, { path: '/gestion/solicitudes', profile: '/gestion/perfil' }])('en $path abre el perfil correcto y ofrece cambiar entre roles disponibles', async ({ path, profile }) => {
  state.pathname = path
  await open()
  expect(screen.getByRole('menuitem', { name: 'Mi perfil' })).toHaveAttribute('href', profile)
  expect(screen.getByRole('menuitem', { name: 'Cambiar de rol' })).toHaveAttribute('href', '/seleccionar-rol')
  expect(screen.getByText('ana@example.test')).toBeInTheDocument()
})

it('usa el usuario como nombre alternativo y oculta el cambio de rol cuando solo tiene uno', async () => {
  state.user.name = ''; state.user.roles = ['CIUDADANO']; state.user.activeRole = ''
  await open()
  expect(screen.queryByRole('menuitem', { name: 'Cambiar de rol' })).not.toBeInTheDocument()
  expect(screen.getByRole('menu')).toHaveTextContent('ana')
})

it('espera que cierre la sesión antes de navegar al inicio de sesión', async () => {
  let finish!: () => void
  state.logout.mockImplementation(() => new Promise<void>(resolve => { finish = resolve }))
  await open()
  await userEvent.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }))
  expect(state.logout).toHaveBeenCalledOnce()
  expect(state.navigate).not.toHaveBeenCalled()
  finish()
  await waitFor(() => expect(state.navigate).toHaveBeenCalledWith({ to: '/login', replace: true }))
})

it('conserva la pantalla actual cuando falla el cierre de sesión', async () => {
  state.logout.mockRejectedValue(new Error('Offline'))
  await open()
  await userEvent.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }))
  await waitFor(() => expect(state.logout).toHaveBeenCalledOnce())
  expect(state.navigate).not.toHaveBeenCalled()
})

it('no repite el cierre mientras el hook informa una operación pendiente', async () => {
  state.pending = true
  await open()
  const logout = screen.getByRole('menuitem', { name: 'Cerrar sesión' })
  expect(logout).toHaveAttribute('aria-disabled', 'true')
  await userEvent.click(logout)
  expect(state.logout).not.toHaveBeenCalled()
})

it('permite cambiar el tema mediante el submenú de preferencias', async () => {
  await open()
  await userEvent.hover(screen.getByRole('menuitem', { name: 'Tema' }))
  await userEvent.click(await screen.findByRole('menuitemradio', { name: 'Oscuro' }))
  expect(setTheme).toHaveBeenCalledWith('dark')
})
