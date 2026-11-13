import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ThemeProviderContext } from '@/context/theme-context'
import { commandActions } from '@/lib/command-menu'
import AppCommandShortcut from './AppCommandShortcut'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, to, ...props }: { children?: ReactNode; to: string }) => <a {...props} href={to}>{children}</a>,
}))
const setTheme = vi.fn()
const originalScroll = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollIntoView')
beforeEach(() => {
  setTheme.mockReset()
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { configurable: true, value: vi.fn() })
})
afterEach(() => {
  commandActions.length = 0
  vi.unstubAllGlobals()
  if (originalScroll) Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', originalScroll)
  else Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView')
})
const open = () => {
  render(<ThemeProviderContext.Provider value={{ theme: 'system', setTheme }}><AppCommandShortcut /></ThemeProviderContext.Provider>)
  fireEvent.keyDown(document, { key: 'k', ctrlKey: true })
}

it('abre con Ctrl+K, filtra acciones y cambia el tema elegido cerrando el buscador', async () => {
  open()
  await userEvent.type(screen.getByPlaceholderText('Buscar una página o acción...'), 'Tema oscuro')
  await userEvent.click(await screen.findByRole('option', { name: 'Tema oscuro' }))
  expect(setTheme).toHaveBeenCalledWith('dark')
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
})

it('informa búsquedas sin resultados y permite encontrar rutas de autenticación', async () => {
  open()
  const search = screen.getByPlaceholderText('Buscar una página o acción...')
  await userEvent.type(search, 'zzzzzzinexistente')
  expect(await screen.findByText('No se encontraron resultados.')).toBeInTheDocument()
  await userEvent.clear(search)
  await userEvent.type(search, 'Iniciar sesión')
  expect(await screen.findByRole('option', { name: 'Iniciar sesión' })).toHaveAttribute('href', '/login')
})

it('ejecuta una acción registrada por otro módulo usando sus palabras clave', async () => {
  const action = vi.fn()
  commandActions.push({ title: 'Ayuda del equipo', keywords: ['soporte'], onSelect: action })
  open()
  await userEvent.type(screen.getByPlaceholderText('Buscar una página o acción...'), 'soporte')
  await userEvent.click(await screen.findByRole('option', { name: 'Ayuda del equipo' }))
  expect(action).toHaveBeenCalledOnce()
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
})

it('ignora K sin modificador y retira su atajo al desmontar', () => {
  const remove = vi.spyOn(document, 'removeEventListener')
  const view = render(<AppCommandShortcut />)
  fireEvent.keyDown(document, { key: 'k' })
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  view.unmount()
  expect(remove).toHaveBeenCalledWith('keydown', expect.any(Function))
  remove.mockRestore()
})
