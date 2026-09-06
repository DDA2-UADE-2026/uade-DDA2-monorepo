import { QueryClient } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { client } from '@/generated/client.gen'
import {
  getAuthenticatedUser,
  getRoleHome,
  requireAuthenticatedUser,
} from '@/lib/auth-route-guards'

const meQueryFn = vi.fn()

vi.mock('@/generated/@tanstack/react-query.gen', () => ({
  meOptions: () => ({
    queryKey: ['me'],
    queryFn: meQueryFn,
  }),
}))

vi.mock('@/generated/client.gen', () => ({
  client: {
    setConfig: vi.fn(),
  },
}))

describe('auth route guards', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    meQueryFn.mockReset()
    vi.mocked(client.setConfig).mockReset()
  })

  it('resuelve el inicio según el rol activo', () => {
    expect(getRoleHome('CIUDADANO')).toBe('/portal')
    expect(getRoleHome('OPERADOR')).toBe('/gestion')
    expect(getRoleHome(' admin ')).toBe('/gestion')
  })

  it('devuelve null si no hay token guardado', async () => {
    const queryClient = new QueryClient()

    await expect(getAuthenticatedUser(queryClient)).resolves.toBeNull()

    expect(meQueryFn).not.toHaveBeenCalled()
  })

  it('devuelve el usuario autenticado cuando /auth/me responde una sesión válida', async () => {
    const queryClient = new QueryClient()
    const user = {
      username: 'ada.lovelace',
      activeRole: 'OPERADOR',
      roles: ['OPERADOR'],
    }

    localStorage.setItem('auth-token', 'jwt-operativo')
    meQueryFn.mockResolvedValue({ user })

    await expect(requireAuthenticatedUser(queryClient)).resolves.toEqual({ user })
  })

  it('limpia la sesión local cuando /auth/me falla', async () => {
    const queryClient = new QueryClient()

    localStorage.setItem('auth-token', 'jwt-vencido')
    meQueryFn.mockRejectedValue(new Error('Unauthorized'))

    await expect(getAuthenticatedUser(queryClient)).resolves.toBeNull()

    expect(localStorage.getItem('auth-token')).toBeNull()
    expect(client.setConfig).toHaveBeenCalledWith({ headers: { Authorization: null } })
  })
})
