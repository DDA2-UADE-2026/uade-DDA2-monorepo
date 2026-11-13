import { afterEach, beforeEach, expect, it } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { citizenVisitError, citizenVisitQueries, citizenVisits } from './citizen-visits'

let api: ReturnType<typeof apiHarness>
beforeEach(() => { api = apiHarness(() => Response.json({ content: [], page: 2, size: 5, totalElements: 0, totalPages: 0 })) })
afterEach(() => api.restore())

it('consulta próximas visitas con paginación y separa su caché del historial', async () => {
  await api.queryClient.fetchQuery(citizenVisitQueries.listAll(true, 2, 5))
  await api.queryClient.fetchQuery(citizenVisitQueries.listAll(false, 2, 5))
  expect(api.requests).toHaveLength(2)
  expect(new URL(api.requests[0].url).searchParams.get('upcoming')).toBe('true')
  expect(new URL(api.requests[1].url).searchParams.get('upcoming')).toBe('false')
  expect(new URL(api.requests[0].url).searchParams.get('page')).toBe('2')
  expect(new URL(api.requests[0].url).searchParams.get('size')).toBe('5')
})

it('codifica los identificadores y distingue visitas de pedidos de reprogramación', async () => {
  await api.queryClient.fetchQuery(citizenVisitQueries.list('app/a', 0, 10))
  await api.queryClient.fetchQuery(citizenVisitQueries.detail('app/a', 'visit/b'))
  await api.queryClient.fetchQuery(citizenVisitQueries.requests('app/a', 'visit/b', 1, 3))
  await api.queryClient.fetchQuery(citizenVisitQueries.request('app/a', 'visit/b', 'request/c'))
  expect(api.requests.map(r => new URL(r.url).pathname)).toEqual([
    '/api/applications/app%2Fa/visits', '/api/applications/app%2Fa/visits/visit%2Fb',
    '/api/applications/app%2Fa/visits/visit%2Fb/reschedule-requests',
    '/api/applications/app%2Fa/visits/visit%2Fb/reschedule-requests/request%2Fc',
  ])
})

it('envía el motivo por JSON al crear un pedido y conserva los errores para el usuario', async () => {
  await citizenVisits.createRequest('app', 'visit', 'Necesito otro horario')
  expect(api.requests[0].method).toBe('POST')
  expect(api.requests[0].headers.get('Content-Type')).toBe('application/json')
  expect(await api.requests[0].json()).toEqual({ reason: 'Necesito otro horario' })
  api.restore()
  api = apiHarness(() => Response.json({ status: 409, message: 'Ya existe un pedido pendiente' }, { status: 409 }))
  await expect(citizenVisits.createRequest('app', 'visit', 'Otro motivo')).rejects.toMatchObject({ status: 409 })
  expect(citizenVisitError({ message: 'Ya existe un pedido pendiente' })).toBe('Ya existe un pedido pendiente')
  expect(citizenVisitError(null)).toContain('Intentá nuevamente')
  expect(citizenVisitError({ message: 4 })).toContain('Intentá nuevamente')
})
