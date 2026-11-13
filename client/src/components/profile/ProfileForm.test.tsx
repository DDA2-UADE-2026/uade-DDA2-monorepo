import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, expect, it, vi } from 'vitest'
import type { UserResponse } from '@/generated/types.gen'
import { ProfileForm } from './ProfileForm'

const state = vi.hoisted(() => ({ data: undefined as { user: UserResponse } | undefined, isLoading: false, isError: false }))
vi.mock('@/hooks/use-auth', () => ({ useMe: () => state }))
vi.mock('@tanstack/react-router', () => ({ Link: ({ to, children, ...props }: { to: string; children?: ReactNode }) => <a href={to} {...props}>{children}</a> }))
beforeEach(() => { state.data = undefined; state.isLoading = false; state.isError = false })

it.each(['portal', 'gestion'] as const)('informa carga sin exponer datos anteriores en %s', displayType => {
  state.isLoading = true
  render(<ProfileForm displayType={displayType} />)
  expect(screen.getByLabelText('Cargando perfil')).toBeInTheDocument()
  expect(screen.queryByLabelText('Nombre completo')).not.toBeInTheDocument()
})

it.each([true, false])('ofrece iniciar sesión cuando falla o falta el usuario (error=%s)', isError => {
  state.isError = isError
  render(<ProfileForm displayType="portal" />)
  expect(screen.getByText('No pudimos cargar tu perfil')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Ir al inicio de sesión' })).toHaveAttribute('href', '/login')
})

it('muestra identidad, roles y permisos actuales con campos de solo lectura', async () => {
  state.data = { user: { id: 4, name: 'Ana Prueba', username: 'ana', email: 'ana@example.test', roles: ['ADMIN', 'CIUDADANO'], permissions: ['users:view', 'programs:management:view'] } }
  render(<ProfileForm displayType="gestion" />)
  const name = screen.getByLabelText('Nombre completo')
  expect(name).toHaveValue('Ana Prueba')
  expect(name).toHaveAttribute('readonly')
  await userEvent.setup().type(name, 'Cambio')
  expect(name).toHaveValue('Ana Prueba')
  expect(screen.getByLabelText('Correo electrónico')).toHaveValue('ana@example.test')
  expect(within(screen.getByRole('list', { name: 'Roles' })).getByText('ADMIN')).toBeInTheDocument()
  expect(within(screen.getByRole('list', { name: 'Permisos' })).getByText('users:view')).toBeInTheDocument()
})

it('usa identificadores alternativos y aclara accesos ausentes', () => {
  state.data = { user: { username: 'usuario-sin-nombre' } }
  const view = render(<ProfileForm displayType="gestion" />)
  expect(screen.getByLabelText('Nombre completo')).toHaveValue('usuario-sin-nombre')
  expect(screen.getByLabelText('Correo electrónico')).toHaveValue('')
  expect(screen.getByRole('list', { name: 'Permisos' })).toHaveTextContent('Sin permisos asignados')
  view.unmount()
  state.data = { user: {} }
  render(<ProfileForm displayType="portal" />)
  expect(screen.getByLabelText('Nombre completo')).toHaveValue('Usuario')
  expect(screen.getByText('Sin nombre de usuario')).toBeInTheDocument()
  expect(screen.queryByRole('list', { name: 'Permisos' })).not.toBeInTheDocument()
})
