import { QueryClient, queryOptions } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getAuthenticatedUser,
  getRoleHome,
  redirectAuthenticatedUser,
  requireAuthenticatedUser,
  requireRoleChoice,
} from '@/lib/auth-route-guards'

const meQueryFn = vi.fn()
const pendingQueryFn = vi.fn()
const privateOptions = queryOptions({ queryKey: ['private'], queryFn: async () => ({ data: true }) })

vi.mock('@/generated/@tanstack/react-query.gen', () => ({
  meOptions: () => ({
    queryKey: ['me'],
    queryFn: meQueryFn,
  }),
  sessionRoleSelectionOptions: () => ({ queryKey: ['pending-role'], queryFn: pendingQueryFn }),
}))

describe('auth route guards', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    meQueryFn.mockReset()
    pendingQueryFn.mockReset()
  })

  it('resuelve el inicio según el rol activo', () => {
    expect(getRoleHome('CIUDADANO')).toBe('/portal')
    expect(getRoleHome('OPERADOR')).toBe('/gestion')
    expect(getRoleHome(' admin ')).toBe('/gestion')
  })

  it('consulta el servidor incluso sin token en localStorage y acepta 401 como sesión ausente', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockRejectedValue({ status: 401 })

    await expect(getAuthenticatedUser(queryClient)).resolves.toBeNull()

    expect(meQueryFn).toHaveBeenCalledOnce()
  })

  it('devuelve el usuario autenticado cuando /auth/me responde una sesión válida', async () => {
    const queryClient = new QueryClient()
    const user = {
      username: 'ada.lovelace',
      activeRole: 'OPERADOR',
      roles: ['OPERADOR'],
    }

    meQueryFn.mockResolvedValue({ user })

    await expect(requireAuthenticatedUser(queryClient)).resolves.toEqual({ user })
  })

  it('redirige al login si /auth/me no devuelve un usuario', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockResolvedValue({ user: null })

    await expect(requireAuthenticatedUser(queryClient)).rejects.toMatchObject({
      options: { to: '/login', replace: true },
    })
  })

  it('limpia la sesión local cuando /auth/me falla', async () => {
    const queryClient = new QueryClient()

    localStorage.setItem('auth-token', 'jwt-vencido')
    meQueryFn.mockRejectedValue({ status: 401 })

    await expect(getAuthenticatedUser(queryClient)).resolves.toBeNull()

    expect(localStorage.getItem('auth-token')).toBeNull()
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
  })

  it('no convierte una falla de red en un cierre de sesión', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(privateOptions.queryKey, { data: true })
    const error = new TypeError('Failed to fetch')
    meQueryFn.mockRejectedValue(error)
    await expect(getAuthenticatedUser(queryClient)).rejects.toBe(error)
    expect(queryClient.getQueryData(privateOptions.queryKey)).toEqual({ data: true })
  })

  it('recupera la selección pendiente desde la cookie después de recargar', async () => {
    const queryClient = new QueryClient()
    const user = { username: 'test', roles: ['CIUDADANO', 'AUDITOR'] }
    meQueryFn.mockRejectedValue({ status: 401 })
    pendingQueryFn.mockResolvedValue({ requiresRoleSelection: true, user })
    await expect(requireRoleChoice(queryClient)).resolves.toEqual({ mode: 'select', pending: { user } })
    expect(sessionStorage.getItem('role-selection')).toBeNull()
  })

  it('permite cambiar de rol cuando el usuario ya tiene más de un rol', async () => {
    const queryClient = new QueryClient()
    const user = {
      username: 'test',
      activeRole: 'CIUDADANO',
      roles: ['CIUDADANO', 'AUDITOR'],
    }
    meQueryFn.mockResolvedValue({ user })

    await expect(requireRoleChoice(queryClient)).resolves.toEqual({ mode: 'switch', user })
  })

  it('redirige al inicio del rol activo si no hay otro rol para elegir', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockResolvedValue({
      user: { username: 'test', activeRole: 'CIUDADANO', roles: ['CIUDADANO'] },
    })

    await expect(requireRoleChoice(queryClient)).rejects.toMatchObject({
      options: { to: '/portal', replace: true },
    })
  })

  it('trata como rol único una sesión activa sin lista de roles', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockResolvedValue({ user: { username: 'test', activeRole: 'AUDITOR' } })

    await expect(requireRoleChoice(queryClient)).rejects.toMatchObject({
      options: { to: '/gestion', replace: true },
    })
  })

  it('redirige al login cuando no hay sesión ni selección de rol pendiente', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockRejectedValue({ status: 401 })
    pendingQueryFn.mockResolvedValue({ requiresRoleSelection: false })

    await expect(requireRoleChoice(queryClient)).rejects.toMatchObject({
      options: { to: '/login', replace: true },
    })
  })

  it('redirige una sesión activa según el rol que ya tiene seleccionado', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockResolvedValue({
      user: { username: 'test', activeRole: 'AUDITOR', roles: ['AUDITOR'] },
    })

    await expect(redirectAuthenticatedUser(queryClient)).rejects.toMatchObject({
      options: { to: '/gestion', replace: true },
    })
  })

  it('redirige a seleccionar rol si la sesión pendiente todavía no tiene un rol activo', async () => {
    const queryClient = new QueryClient()
    meQueryFn.mockRejectedValue({ status: 401 })
    pendingQueryFn.mockResolvedValue({
      requiresRoleSelection: true,
      user: { username: 'test', roles: ['CIUDADANO', 'AUDITOR'] },
    })

    await expect(redirectAuthenticatedUser(queryClient)).rejects.toMatchObject({
      options: { to: '/seleccionar-rol', replace: true },
    })
  })
})
