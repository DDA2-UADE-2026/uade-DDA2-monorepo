import { afterEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { clearLegacyAuthStorage, getPendingRoleSelection, isUnauthenticated } from './auth-session'

let api: ReturnType<typeof apiHarness> | undefined
afterEach(() => { api?.restore(); api = undefined; vi.restoreAllMocks(); localStorage.clear(); sessionStorage.clear() })

it.each([null, undefined, '401', 401, {}, { status: 403 }, { status: '401' }])('no confunde errores ajenos con una sesión no autenticada: %j', error => {
  expect(isUnauthenticated(error)).toBe(false)
})
it('reconoce el HTTP 401 y limpia solamente credenciales heredadas', () => {
  expect(isUnauthenticated({ status: 401, message: 'expired' })).toBe(true)
  localStorage.setItem('auth-token', 'legacy'); localStorage.setItem('theme', 'dark')
  sessionStorage.setItem('role-selection', 'legacy'); sessionStorage.setItem('draft', 'keep')
  clearLegacyAuthStorage()
  expect(localStorage.getItem('auth-token')).toBeNull()
  expect(sessionStorage.getItem('role-selection')).toBeNull()
  expect(localStorage.getItem('theme')).toBe('dark')
  expect(sessionStorage.getItem('draft')).toBe('keep')
})
it('tolera almacenamiento bloqueado sin impedir el cierre de sesión', () => {
  vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => { throw new DOMException('Blocked', 'SecurityError') })
  expect(clearLegacyAuthStorage).not.toThrow()
})
it.each([
  { requiresRoleSelection: true, user: { id: 3, username: 'ana' }, expected: { user: { id: 3, username: 'ana' } } },
  { requiresRoleSelection: false, user: { id: 3 }, expected: null },
  { requiresRoleSelection: true, user: null, expected: null },
])('solo devuelve un usuario con selección de rol pendiente: %j', async ({ expected, ...response }) => {
  api = apiHarness(() => Response.json(response))
  expect(await getPendingRoleSelection(api.queryClient)).toEqual(expected)
  expect(new URL(api.requests[0].url).pathname).toBe('/auth/session/role-selection')
  expect(api.requests[0].method).toBe('GET')
})
it('trata el 401 como ausencia de selección, sin reintentos', async () => {
  api = apiHarness(() => Response.json({ status: 401 }, { status: 401 }))
  expect(await getPendingRoleSelection(api.queryClient)).toBeNull()
  expect(api.requests).toHaveLength(1)
})
it('propaga la indisponibilidad del servidor en lugar de fingir que no hay sesión', async () => {
  api = apiHarness(() => Response.json({ status: 503, message: 'Unavailable' }, { status: 503 }))
  await expect(getPendingRoleSelection(api.queryClient)).rejects.toMatchObject({ status: 503, message: 'Unavailable' })
  expect(api.requests).toHaveLength(1)
})
