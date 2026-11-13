import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { CoreEventsTable } from './CoreEventsTable'

let api: ReturnType<typeof apiHarness>
let listFails: boolean
let detailFails: boolean
let empty: boolean
const base = '/api/admin/integrations/core/events'
const event = { id: 'row-1', eventId: 'event-1', eventType: 'solicitud.aprobada', sourceModule: 'social', eventVersion: '1', status: 'ROUTED', processingMs: 12, deliveryCount: 2, deliveredCount: 1, correlationId: '12345678-1234-1234-1234-123456789abc' }

beforeEach(() => {
  listFails = false; detailFails = false; empty = false
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  api = apiHarness(request => {
    const url = new URL(request.url)
    if (url.pathname === base) return listFails
      ? Response.json({ message: 'Core no disponible' }, { status: 503 })
      : Response.json({ items: empty ? [] : [event], total: empty ? 0 : 40, pages: empty ? 1 : 2 })
    if (url.pathname === `${base}/event-1`) return detailFails
      ? Response.json({ message: 'Detalle temporalmente inaccesible' }, { status: 503 })
      : Response.json({ status: 'ROUTED', envelope: { operation: 'approve', applicationId: 'app-42' }, deliveries: [{ targetModule: 'salud', queueName: 'salud.events', status: 'PENDING_RETRY', attempts: 2, maxAttempts: 5 }] })
    throw new Error(`Unexpected request ${request.method} ${url.pathname}`)
  })
})
afterEach(() => { api.restore(); vi.unstubAllGlobals() })
const queries = () => api.requests.filter(request => new URL(request.url).pathname === base).map(request => new URL(request.url).searchParams)

it('muestra dirección, estado y entregas, y pagina mediante el contrato HTTP', async () => {
  const user = userEvent.setup()
  api.render(<CoreEventsTable module="social" />)
  expect(await screen.findByText('solicitud.aprobada')).toBeInTheDocument()
  expect(screen.getByText('Enviado')).toBeInTheDocument()
  expect(screen.getByText('Ruteado')).toBeInTheDocument()
  expect(screen.getByText('1/2')).toBeInTheDocument()
  expect(screen.getByText('12 ms')).toBeInTheDocument()
  expect(queries()[0].get('page')).toBe('1')
  await user.click(screen.getByRole('button', { name: /next page/i }))
  await waitFor(() => expect(queries().at(-1)?.get('page')).toBe('2'))
  expect(queries().at(-1)?.get('size')).toBe('25')
})

it('valida filtros antes de consultar y permite aplicar y limpiar filtros', async () => {
  const user = userEvent.setup()
  api.render(<CoreEventsTable />)
  await screen.findByText('solicitud.aprobada')
  const correlation = screen.getByLabelText('Correlación')
  await user.type(correlation, 'incorrecto')
  await user.click(screen.getByRole('button', { name: 'Buscar' }))
  expect(screen.getByText('La correlación debe ser un UUID.')).toBeInTheDocument()
  expect(queries()).toHaveLength(1)
  await user.clear(correlation)
  fireEvent.change(screen.getByLabelText('Tipo de evento'), { target: { value: 'x'.repeat(121) } })
  await user.click(screen.getByRole('button', { name: 'Buscar' }))
  expect(screen.getByText('El tipo de evento admite hasta 120 caracteres.')).toBeInTheDocument()
  expect(queries()).toHaveLength(1)
  fireEvent.change(screen.getByLabelText('Tipo de evento'), { target: { value: '  solicitud.aprobada  ' } })
  await user.type(correlation, event.correlationId)
  await user.click(screen.getByRole('combobox'))
  await user.click(await screen.findByRole('option', { name: 'Duplicado' }))
  await user.click(screen.getByRole('button', { name: 'Buscar' }))
  await waitFor(() => expect(queries().at(-1)?.get('status')).toBe('DUPLICATE'))
  expect(queries().at(-1)?.get('eventType')).toBe('solicitud.aprobada')
  expect(queries().at(-1)?.get('correlationId')).toBe(event.correlationId)
  await user.click(screen.getByRole('button', { name: 'Limpiar' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: 'Limpiar' })).not.toBeInTheDocument())
  expect(correlation).toHaveValue('')
  expect(screen.getByLabelText('Tipo de evento')).toHaveValue('')
})

it('recupera un listado fallido y distingue vacío general de filtros sin coincidencias', async () => {
  listFails = true
  const user = userEvent.setup()
  api.render(<CoreEventsTable />)
  expect(await screen.findByText(/No se pudieron cargar los eventos/)).toHaveTextContent('Core no disponible')
  listFails = false; empty = true
  await user.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText('Todavía no hay eventos visibles para el módulo.')).toBeInTheDocument()
  await user.type(screen.getByLabelText('Tipo de evento'), 'desconocido')
  await user.click(screen.getByRole('button', { name: 'Buscar' }))
  expect(await screen.findByText('No hay eventos para estos filtros.')).toBeInTheDocument()
})

it('abre el detalle con teclado y relaciona sobre, destino y reintentos con el evento elegido', async () => {
  api.render(<CoreEventsTable module="otro" />)
  const row = (await screen.findByText('solicitud.aprobada')).closest('tr')!
  expect(within(row).getByText('Recibido')).toBeInTheDocument()
  expect(api.requests.some(request => new URL(request.url).pathname.endsWith('/event-1'))).toBe(false)
  fireEvent.keyDown(row, { key: 'Enter' })
  const dialog = await screen.findByRole('dialog')
  expect(await within(dialog).findByText('salud.events')).toBeInTheDocument()
  expect(within(dialog).getByText('Reintento pendiente')).toBeInTheDocument()
  expect(within(dialog).getByText('2/5')).toBeInTheDocument()
  expect(within(dialog).getByText(/"applicationId": "app-42"/)).toHaveTextContent('"operation": "approve"')
})

it('mantiene el evento seleccionado cuando falla el detalle y permite reintentarlo', async () => {
  detailFails = true
  const user = userEvent.setup()
  api.render(<CoreEventsTable />)
  await user.click(await screen.findByText('solicitud.aprobada'))
  const dialog = await screen.findByRole('dialog')
  expect(await within(dialog).findByText(/No se pudo cargar el detalle/)).toHaveTextContent('Detalle temporalmente inaccesible')
  expect(within(dialog).getByText(event.correlationId)).toBeInTheDocument()
  detailFails = false
  await user.click(within(dialog).getByRole('button', { name: 'Reintentar' }))
  expect(await within(dialog).findByText('salud.events')).toBeInTheDocument()
})
