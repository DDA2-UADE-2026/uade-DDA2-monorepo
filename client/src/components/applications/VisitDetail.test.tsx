import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { apiHarness } from '@/test/api-harness'
import type { Visit } from '@/lib/social-management'
import { VisitDetail } from './VisitDetail'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#application">{children}</a>,
}))
let api: ReturnType<typeof apiHarness>
let visit: Visit
let readFails: boolean
let writeFails: boolean
const permissions = ['applications:visits:manage', 'applications:visits:results:create']
beforeEach(() => {
  readFails = false; writeFails = false
  visit = {
    id: 'visit-1', applicationId: 'app-1', applicationNumber: 42, applicationStatus: 'IN_VISIT',
    applicantUserId: 2, applicantName: 'Ana Prueba', programName: 'Alimentos', programEditionName: 'Anual',
    type: 'VISIT', startsAt: '2099-01-01T10:00:00Z', endsAt: '2099-01-01T11:00:00Z', durationMinutes: 60,
    location: 'Centro norte', status: 'SCHEDULED', responsibleId: 1, responsibleName: 'Profesional prueba',
    scheduledAt: '2026-01-01T10:00:00Z', rescheduled: false, lastRescheduledAt: null, latestRescheduleRequest: null,
    cancelledAt: null, cancelledByName: null, resultRecordedAt: null, resultRecordedByName: null,
  }
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  vi.spyOn(toast, 'success').mockImplementation(() => 'success')
  vi.spyOn(toast, 'error').mockImplementation(() => 'error')
  api = apiHarness(request => {
    if (request.method === 'GET') return readFails ? Response.json({ message: 'Sin conexión' }, { status: 503 }) : Response.json(visit)
    return writeFails ? Response.json({ message: 'La actuación cambió' }, { status: 409 }) : Response.json(visit)
  })
})
afterEach(() => { api.restore(); vi.restoreAllMocks(); vi.unstubAllGlobals() })
const writes = () => api.requests.filter(request => request.method !== 'GET')
const renderVisit = (grants = permissions, currentUserId = 1) => api.render(<VisitDetail id="visit-1" permissions={grants} currentUserId={currentUserId} />)

it('carga datos del titular y responsable y bloquea el resultado antes del fin', async () => {
  renderVisit()
  expect(screen.getByRole('status', { name: 'Cargando actuación' })).toBeInTheDocument()
  expect(await screen.findByRole('heading', { name: 'Visita · Solicitud N.º 42' })).toBeInTheDocument()
  expect(screen.getByText('Ana Prueba')).toBeInTheDocument()
  expect(screen.getByText('Profesional prueba')).toBeInTheDocument()
  expect(screen.getByText('Centro norte')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Registrar resultado' })).toBeDisabled()
  expect(screen.getByText(/Podés registrar el resultado cuando termine/)).toBeInTheDocument()
})

it.each([{ grants: [], owner: 1 }, { grants: permissions, owner: 9 }])('oculta acciones sin permisos o sin ser responsable: %o', async ({ grants, owner }) => {
  renderVisit(grants, owner)
  await screen.findByText('Centro norte')
  for (const name of ['Reprogramar', 'Cancelar', 'Registrar resultado']) expect(screen.queryByRole('button', { name })).not.toBeInTheDocument()
  expect(writes()).toHaveLength(0)
})

it('bloquea cambios con pedido de reprogramación pendiente', async () => {
  visit.latestRescheduleRequest = { id: 'request-1', status: 'PENDING', requestedAt: '2026-01-01T12:00:00Z', reviewedAt: null }
  renderVisit()
  expect(await screen.findByText('Reprogramación pendiente')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Registrar resultado' })).not.toBeInTheDocument()
  expect(screen.getByText('Pedido de reprogramación: pendiente')).toBeInTheDocument()
})

it('cancela con motivo obligatorio y normalizado, y refresca los datos', async () => {
  const user = userEvent.setup()
  renderVisit()
  await user.click(await screen.findByRole('button', { name: 'Cancelar' }))
  const confirm = screen.getByRole('button', { name: 'Confirmar' })
  expect(confirm).toBeDisabled()
  await user.type(screen.getByLabelText('Motivo'), '   ')
  expect(confirm).toBeDisabled()
  await user.type(screen.getByLabelText('Motivo'), 'No disponible   ')
  await user.click(confirm)
  await waitFor(() => expect(toast.success).toHaveBeenCalledWith('Actuación actualizada'))
  expect(writes()).toHaveLength(1)
  expect(writes()[0].method).toBe('PATCH')
  expect(new URL(writes()[0].url).pathname).toBe('/api/applications/app-1/visits/visit-1/cancel')
  expect(await writes()[0].json()).toEqual({ reason: 'No disponible' })
  expect(screen.queryByLabelText('Motivo')).not.toBeInTheDocument()
  await waitFor(() => expect(api.requests.filter(request => request.method === 'GET').length).toBeGreaterThan(1))
})

it('conserva el motivo tras un conflicto y permite reintentar sin perder datos', async () => {
  writeFails = true
  const user = userEvent.setup()
  renderVisit()
  await user.click(await screen.findByRole('button', { name: 'Cancelar' }))
  await user.type(screen.getByLabelText('Motivo'), 'Reprogramación telefónica')
  await user.click(screen.getByRole('button', { name: 'Confirmar' }))
  await waitFor(() => expect(toast.error).toHaveBeenCalledWith('La actuación cambió'))
  expect(screen.getByLabelText('Motivo')).toHaveValue('Reprogramación telefónica')
  writeFails = false
  await user.click(screen.getByRole('button', { name: 'Confirmar' }))
  await waitFor(() => expect(toast.success).toHaveBeenCalled())
  expect(writes()).toHaveLength(2)
})

it('registra el resultado después del fin y envía observaciones y conclusiones sin espacios sobrantes', async () => {
  visit.startsAt = '2020-01-01T10:00:00Z'; visit.endsAt = '2020-01-01T11:00:00Z'
  const user = userEvent.setup()
  renderVisit()
  await user.click(await screen.findByRole('button', { name: 'Registrar resultado' }))
  await user.click(screen.getByRole('combobox'))
  await user.click(await screen.findByRole('option', { name: 'No realizada' }))
  await user.type(screen.getByLabelText('Observaciones (opcional)'), '  Ausente  ')
  await user.type(screen.getByLabelText('Conclusiones (opcional)'), '  Contactar nuevamente  ')
  await user.click(screen.getByRole('button', { name: 'Confirmar' }))
  await waitFor(() => expect(toast.success).toHaveBeenCalled())
  expect(new URL(writes()[0].url).pathname).toBe('/api/applications/app-1/visits/visit-1/result')
  expect(await writes()[0].json()).toEqual({ status: 'NOT_REALIZED', observations: 'Ausente', conclusions: 'Contactar nuevamente' })
})

it('vuelve sin enviar cambios y recupera una lectura fallida', async () => {
  readFails = true
  const user = userEvent.setup()
  renderVisit()
  expect(await screen.findByText('No se pudo cargar la actuación')).toBeInTheDocument()
  readFails = false
  await user.click(screen.getByRole('button', { name: 'Reintentar' }))
  await user.click(await screen.findByRole('button', { name: 'Cancelar' }))
  await user.type(screen.getByLabelText('Motivo'), 'Borrador')
  await user.click(screen.getByRole('button', { name: 'Volver' }))
  expect(screen.queryByLabelText('Motivo')).not.toBeInTheDocument()
  expect(writes()).toHaveLength(0)
})
