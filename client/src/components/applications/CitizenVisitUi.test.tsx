import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import type { CitizenVisit } from '@/lib/citizen-visits'
import { CitizenVisitCard, CitizenVisitDialog } from './CitizenVisitUi'

const auth = vi.hoisted(() => ({ allowed: true }))
vi.mock('@/hooks/use-auth', () => ({ useMe: () => ({ data: { user: { permissions: auth.allowed ? ['applications:own:visits:reschedule-requests:create'] : [] } } }) }))
vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#application">{children}</a>,
}))
let api: ReturnType<typeof apiHarness>
let visit: CitizenVisit
let visitFails: boolean
let historyFails: boolean
let writeFails: boolean
let history: object[]
const base = '/api/applications/app-1/visits/visit-1'
beforeEach(() => {
  auth.allowed = true; visitFails = false; historyFails = false; writeFails = false; history = []
  visit = { id: 'visit-1', applicationId: 'app-1', applicationNumber: 42, programName: 'Alimentos', type: 'VISIT', startsAt: '2099-01-01T10:00:00Z', endsAt: '2099-01-01T11:00:00Z', durationMinutes: 60, location: 'Centro norte', status: 'SCHEDULED', rescheduled: false, lastRescheduledAt: null, latestRescheduleRequest: null, cancelledAt: null }
  api = apiHarness(request => {
    const url = new URL(request.url)
    if (request.method === 'POST') return writeFails ? Response.json({ message: 'Ya existe un pedido pendiente' }, { status: 409 }) : Response.json({ id: 'request-new' }, { status: 201 })
    if (url.pathname === base) return visitFails ? Response.json({ message: 'Visita inaccesible' }, { status: 503 }) : Response.json(visit)
    if (url.pathname === `${base}/reschedule-requests`) return historyFails ? Response.json({ message: 'Historial inaccesible' }, { status: 503 }) : Response.json({ content: history, page: Number(url.searchParams.get('page')), totalElements: history.length ? 7 : 0, totalPages: history.length ? 2 : 1, size: 5 })
    if (url.pathname === `${base}/reschedule-requests/request-1`) return Response.json({ reason: 'Consulta médica', status: 'APPROVED', scheduleAtRequest: { startsAt: visit.startsAt }, reviewedAt: '2026-11-13T12:00:00Z', reviewComment: 'Pedido aprobado', approvedSchedule: { startsAt: '2099-01-02T10:00:00Z', location: 'Centro sur' } })
    throw new Error(`Unexpected request ${request.method} ${request.url}`)
  })
})
afterEach(() => { api.restore() })
const open = () => api.render(<CitizenVisitDialog applicationId="app-1" visitId="visit-1" onClose={vi.fn()} />)
const writes = () => api.requests.filter(request => request.method === 'POST')

it('relaciona la tarjeta con su solicitud y permite abrir el detalle', async () => {
  const onOpen = vi.fn()
  visit.latestRescheduleRequest = { id: 'request-1', status: 'PENDING', requestedAt: '2026-11-13T12:00:00Z', reviewedAt: null }
  api.render(<CitizenVisitCard visit={visit} onOpen={onOpen} showApplication />)
  expect(screen.getByText('Alimentos · Solicitud N.º 42')).toBeInTheDocument()
  expect(screen.getByText('Reprogramación pendiente')).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Ver detalle' }))
  expect(onOpen).toHaveBeenCalledOnce()
})

it.each(['permission', 'past', 'cancelled', 'pending'] as const)('no permite pedir reprogramación si se incumple la condición %s', async condition => {
  if (condition === 'permission') auth.allowed = false
  if (condition === 'past') visit.startsAt = '2020-01-01T10:00:00Z'
  if (condition === 'cancelled') { visit.status = 'CANCELLED'; visit.cancellationReason = 'Suspendida por el equipo' }
  if (condition === 'pending') visit.latestRescheduleRequest = { id: 'pending', status: 'PENDING', requestedAt: '2026-11-13T12:00:00Z', reviewedAt: null }
  open()
  await screen.findByText('Centro norte')
  expect(screen.queryByLabelText('Motivo')).not.toBeInTheDocument()
  expect(writes()).toHaveLength(0)
  if (condition === 'pending') expect(screen.getByText(/La fecha y el lugar actuales siguen vigentes/)).toBeInTheDocument()
  if (condition === 'cancelled') expect(screen.getByText('Suspendida por el equipo')).toBeInTheDocument()
})

it('valida el motivo, envía únicamente el texto normalizado y mantiene el horario vigente', async () => {
  open()
  const reason = await screen.findByLabelText('Motivo')
  expect(screen.getByRole('button', { name: 'Enviar pedido' })).toBeDisabled()
  await userEvent.type(reason, '   ')
  expect(screen.getByRole('button', { name: 'Enviar pedido' })).toBeDisabled()
  await userEvent.type(reason, 'Consulta médica   ')
  await userEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }))
  await waitFor(() => expect(reason).toHaveValue(''))
  expect(writes()).toHaveLength(1)
  expect(new URL(writes()[0].url).pathname).toBe(`${base}/reschedule-requests`)
  expect(await writes()[0].json()).toEqual({ reason: 'Consulta médica' })
  expect(screen.getByText('Centro norte')).toBeInTheDocument()
})

it('conserva el motivo ante un conflicto y permite reintentar', async () => {
  writeFails = true
  open()
  await userEvent.type(await screen.findByLabelText('Motivo'), 'Consulta médica')
  await userEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }))
  expect(await screen.findByText('Ya existe un pedido pendiente')).toBeInTheDocument()
  expect(screen.getByLabelText('Motivo')).toHaveValue('Consulta médica')
  writeFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }))
  await waitFor(() => expect(screen.getByLabelText('Motivo')).toHaveValue(''))
  expect(writes()).toHaveLength(2)
})

it('recupera el detalle y el historial tras errores independientes', async () => {
  visitFails = true; historyFails = true
  open()
  expect(await screen.findByText('Visita inaccesible')).toBeInTheDocument()
  expect(api.requests.some(request => new URL(request.url).pathname.endsWith('reschedule-requests'))).toBe(false)
  visitFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText('Historial inaccesible')).toBeInTheDocument()
  historyFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText('Todavía no hay pedidos de reprogramación.')).toBeInTheDocument()
})

it('muestra respuesta y nuevo lugar del pedido aprobado, y cierra el detalle al cambiar de página', async () => {
  history = [{ id: 'request-1', status: 'APPROVED', requestedAt: '2026-11-13T12:00:00Z' }]
  open()
  await userEvent.click(await screen.findByRole('button', { name: 'Ver detalle' }))
  expect(await screen.findByText('Consulta médica')).toBeInTheDocument()
  expect(screen.getByText('Pedido aprobado')).toBeInTheDocument()
  expect(screen.getByText(/Centro sur/)).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: /next page/i }))
  await waitFor(() => expect(api.requests.some(request => new URL(request.url).searchParams.get('page') === '1')).toBe(true))
  expect(screen.queryByText('Detalle del pedido')).not.toBeInTheDocument()
})
