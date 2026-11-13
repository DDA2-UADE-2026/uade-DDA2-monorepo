import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState, type ReactNode } from 'react'
import { IconHome } from '@tabler/icons-react'
import { afterEach, expect, it, vi } from 'vitest'
import { Sidebar, SidebarContent, SidebarProvider, SidebarTrigger, useSidebar } from '@/components/ui/sidebar'
import { SidebarNavMenu, type SidebarNavItem } from './SidebarNavMenu'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, to, disabled, ...props }: { children?: ReactNode; to: string; disabled?: boolean }) => <a {...props} href={disabled ? undefined : to} aria-disabled={disabled || undefined}>{children}</a>,
}))
const items: SidebarNavItem[] = [
  { title: 'Inicio', url: '/gestion', icon: IconHome },
  { title: 'Solicitudes', url: '/gestion/solicitudes', icon: IconHome, requiredPermissions: ['applications:view', 'applications:manage'], items: [
    { title: 'Asistida', url: '/gestion/solicitudes/asistida', requiredPermissions: ['applications:create'], icon: IconHome },
    { title: 'Listado', url: '/gestion/solicitudes/listado' },
  ] },
]
afterEach(() => { vi.restoreAllMocks(); document.cookie = 'sidebar_state=; path=/; max-age=0' })
const menu = (permissions: string[] = [], pathname = '/gestion/solicitudes/asistida') => <SidebarNavMenu items={items} pathname={pathname} permissions={permissions} />

it('marca la sección y subruta activas sin activar Inicio en todas las páginas', () => {
  render(<SidebarProvider>{menu(['applications:view', 'applications:create'])}</SidebarProvider>)
  expect(screen.getByRole('link', { name: 'Inicio' })).not.toHaveAttribute('data-active')
  expect(screen.getByRole('link', { name: 'Solicitudes' })).toHaveAttribute('data-active')
  expect(screen.getByRole('link', { name: 'Asistida' })).toHaveAttribute('data-active')
  expect(screen.getByRole('link', { name: 'Listado' })).not.toHaveAttribute('data-active')
})

it('bloquea accesos por separado y permite el padre con cualquiera de sus permisos', () => {
  render(<SidebarProvider>{menu(['applications:manage'])}</SidebarProvider>)
  expect(screen.getByRole('link', { name: 'Solicitudes' })).toHaveAttribute('href', '/gestion/solicitudes')
  const child = screen.getByText('Asistida').closest('a')!
  expect(child).toHaveAttribute('aria-disabled', 'true')
  expect(child).not.toHaveAttribute('href')
  expect(screen.getByRole('link', { name: 'Listado' })).toHaveAttribute('href', '/gestion/solicitudes/listado')
})

function State() {
  const sidebar = useSidebar()
  return <output aria-label="Estado del menú">{sidebar.state}:{String(sidebar.openMobile)}</output>
}

it('alterna el menú por botón y Ctrl+B y guarda la preferencia sin navegar', async () => {
  const user = userEvent.setup()
  render(<SidebarProvider><Sidebar><SidebarContent>{menu()}</SidebarContent></Sidebar><SidebarTrigger /><State /></SidebarProvider>)
  expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('expanded:false')
  await user.click(screen.getByRole('button', { name: 'Toggle Sidebar' }))
  expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('collapsed:false')
  expect(document.cookie).toContain('sidebar_state=false')
  fireEvent.keyDown(window, { key: 'b', ctrlKey: true })
  expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('expanded:false')
  expect(document.cookie).toContain('sidebar_state=true')
  fireEvent.keyDown(window, { key: 'b' })
  expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('expanded:false')
})

it('respeta el control externo del estado de apertura', async () => {
  function Controlled() {
    const [open, setOpen] = useState(false)
    return <SidebarProvider open={open} onOpenChange={setOpen}><SidebarTrigger /><State /></SidebarProvider>
  }
  render(<Controlled />)
  expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('collapsed')
  await userEvent.click(screen.getByRole('button', { name: 'Toggle Sidebar' }))
  expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('expanded')
})

it('abre el panel móvil y mantiene el estado de escritorio independiente', async () => {
  const descriptor = Object.getOwnPropertyDescriptor(window, 'innerWidth')!
  Object.defineProperty(window, 'innerWidth', { configurable: true, value: 500 })
  try {
    vi.spyOn(window, 'matchMedia').mockImplementation(query => ({ matches: true, media: query, onchange: null, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn() }))
    render(<SidebarProvider><Sidebar><SidebarContent>{menu()}</SidebarContent></Sidebar><SidebarTrigger /><State /></SidebarProvider>)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Toggle Sidebar' }))
    expect(await screen.findByRole('dialog', { name: 'Sidebar' })).toBeInTheDocument()
    expect(screen.getByLabelText('Estado del menú')).toHaveTextContent('expanded:true')
    expect(document.cookie).not.toContain('sidebar_state=false')
  } finally {
    Object.defineProperty(window, 'innerWidth', descriptor)
  }
})
