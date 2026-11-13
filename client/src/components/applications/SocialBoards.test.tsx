import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { apiHarness } from '@/test/api-harness'
import { SocialVisitsBoard } from './SocialVisitsBoard'
import { RescheduleRequestDetail, SocialRescheduleInbox } from './SocialRescheduleInbox'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, to, params, ...props }: { children?: ReactNode; to: string; params?: Record<string, string> }) => <a {...props} href={Object.entries(params ?? {}).reduce((url, [key, value]) => url.replace(`$${key}`, value), to)}>{children}</a>,
}))
let api: ReturnType<typeof apiHarness>
let readFails: boolean
let writeFails: boolean
const visitBase = '/api/admin/application-visits'
const requestBase = '/api/admin/application-visit-reschedule-requests'
const request = { id: 'request-1', applicationId: 'app-1', applicationNumber: 42, visitId: 'visit-1', applicantName: 'Ana Prueba', responsibleId: 1, responsibleName: 'Profesional prueba', status: 'PENDING', reason: 'Consulta médica', requestedAt: '2026-11-13T12:00:00Z', originalStartsAt: '2099-01-01T10:00:00Z', scheduleAtRequest: { startsAt: '2099-01-01T10:00:00Z', location: 'Centro norte' } }
beforeEach(() => {
  readFails = false; writeFails = false
  vi.spyOn(toast, 'success').mockImplementation(() => 'success')
  vi.spyOn(toast, 'error').mockImplementation(() => 'error')
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  api = apiHarness(req => {
    if (req.method !== 'GET') return writeFails ? Response.json({ message: 'Conflicto de actuación' }, { status: 409 }) : Response.json({ id: 'saved' })
    if (readFails) return Response.json({ message: 'Servicio inaccesible' }, { status: 503 })
    const url = new URL(req.url)
    if (url.pathname === `${requestBase}/request-1`) return Response.json(request)
    if (url.pathname === requestBase) return Response.json({ content: [request], totalElements: 30, totalPages: 2 })
    if (url.pathname === visitBase) return Response.json({ content: [{ id: 'visit-1', applicationId: 'app-1', applicationNumber: 42, type: 'VISIT', status: 'SCHEDULED', applicantName: 'Ana Prueba', responsibleName: 'Profesional prueba', startsAt: '2099-01-01T10:00:00Z', latestRescheduleRequest: { status: 'PENDING' } }], totalElements: 30, totalPages: 2 })
    throw new Error(`Unexpected request ${req.url}`)
  })
})
afterEach(() => { api.restore(); vi.restoreAllMocks(); vi.unstubAllGlobals() })
const writes = () => api.requests.filter(req => req.method !== 'GET')
async function chooseDate(label: string) {
  await userEvent.click(screen.getByRole('button', { name: label }))
  await userEvent.click(await screen.findByRole('button', { name: new RegExp(format(new Date(), "d 'de' MMMM 'de' yyyy", { locale: es })) }))
}

it('no consulta visitas ni reprogramaciones si el rol no puede verlas', () => {
  api.render(<><SocialVisitsBoard permissions={[]} /><SocialRescheduleInbox permissions={[]} page={1} status="" onPageChange={vi.fn()} onStatusChange={vi.fn()} /></>)
  expect(screen.getByText('Tu rol no tiene permiso para consultar visitas.')).toBeInTheDocument()
  expect(screen.getByText('Tu rol no tiene permiso para consultar solicitudes de reprogramación.')).toBeInTheDocument()
  expect(api.requests).toHaveLength(0)
})

it('filtra visitas por número y estado y consulta la siguiente página usando índices desde cero', async () => {
  api.render(<SocialVisitsBoard permissions={['applications:visits:view']} />)
  await screen.findByText('Ana Prueba')
  expect(screen.getByText('Pedido pendiente')).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('N.º de solicitud'), { target: { value: '42' } })
  await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
  await waitFor(() => expect(new URL(api.requests.at(-1)!.url).searchParams.get('applicationNumber')).toBe('42'))
  await userEvent.click(screen.getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: 'Cancelada' }))
  await waitFor(() => expect(new URL(api.requests.at(-1)!.url).searchParams.get('status')).toBe('CANCELLED'))
  await userEvent.click(screen.getByRole('button', { name: /next page/i }))
  await waitFor(() => expect(new URL(api.requests.at(-1)!.url).searchParams.get('page')).toBe('1'))
})

it('no ofrece programación si la solicitud no admite más actuaciones', () => {
  api.render(<SocialVisitsBoard applicationId="app-1" permissions={['applications:visits:create']} canSchedule={false} />)
  expect(screen.queryByRole('button', { name: 'Programar actuación' })).not.toBeInTheDocument()
})

it('programa una entrevista con fecha, duración y lugar y limpia el borrador al confirmar', async () => {
  api.render(<SocialVisitsBoard applicationId="app-1" permissions={['applications:visits:create', 'applications:visits:view']} />)
  expect(screen.getByRole('button', { name: 'Programar actuación' })).toBeDisabled()
  await userEvent.click(screen.getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: 'Entrevista' }))
  await chooseDate('Inicio')
  fireEvent.change(screen.getByLabelText('Hora de inicio'), { target: { value: '23:59' } })
  await userEvent.type(screen.getByLabelText('Lugar'), '  Centro sur  ')
  fireEvent.change(screen.getByLabelText('Duración en minutos'), { target: { value: '45' } })
  await userEvent.click(screen.getByRole('button', { name: 'Programar actuación' }))
  await waitFor(() => expect(toast.success).toHaveBeenCalledWith('Actuación programada'))
  expect(writes()[0].method).toBe('POST')
  expect(new URL(writes()[0].url).pathname).toBe('/api/applications/app-1/visits')
  expect(await writes()[0].json()).toEqual({ type: 'INTERVIEW', startsAt: new Date(`${format(new Date(), 'yyyy-MM-dd')}T23:59`).toISOString(), durationMinutes: 45, location: 'Centro sur' })
  expect(screen.getByLabelText('Lugar')).toHaveValue('')
})

it('recupera una lectura fallida del tablero de visitas', async () => {
  readFails = true
  api.render(<SocialVisitsBoard permissions={['applications:visits:view']} />)
  expect(await screen.findByText('No se pudieron cargar las actuaciones.')).toBeInTheDocument()
  readFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText('Ana Prueba')).toBeInTheDocument()
})

it('envía los cambios de filtro y página de la bandeja al estado de su pantalla', async () => {
  const page = vi.fn(); const status = vi.fn()
  api.render(<SocialRescheduleInbox permissions={['applications:visits:reschedule-requests:view']} page={1} status="PENDING" onPageChange={page} onStatusChange={status} />)
  await screen.findByText('Ana Prueba')
  expect(new URL(api.requests[0].url).searchParams.get('status')).toBe('PENDING')
  await userEvent.click(screen.getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: 'Aprobada' }))
  expect(status).toHaveBeenCalledWith('APPROVED')
  await userEvent.click(screen.getByRole('button', { name: /next page/i }))
  expect(page).toHaveBeenCalledWith(2)
})

it.each([{ canReview: false, user: 1 }, { canReview: true, user: 2 }])('no deja resolver un pedido sin permiso o fuera del responsable: %j', async ({ canReview, user }) => {
  api.render(<RescheduleRequestDetail id="request-1" canReview={canReview} currentUserId={user} />)
  await screen.findByText('Consulta médica')
  expect(screen.queryByRole('button', { name: 'Confirmar decisión' })).not.toBeInTheDocument()
  expect(writes()).toHaveLength(0)
})

it('exige un comentario para rechazar y permite reintentar un conflicto sin perderlo', async () => {
  writeFails = true
  api.render(<RescheduleRequestDetail id="request-1" canReview currentUserId={1} />)
  await screen.findByText('Consulta médica')
  await userEvent.click(screen.getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: 'Rechazar' }))
  expect(screen.getByRole('button', { name: 'Confirmar decisión' })).toBeDisabled()
  await userEvent.type(screen.getByLabelText('Comentario (obligatorio)'), '  No hay disponibilidad  ')
  await userEvent.click(screen.getByRole('button', { name: 'Confirmar decisión' }))
  await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Conflicto de actuación'))
  expect(screen.getByLabelText('Comentario (obligatorio)')).toHaveValue('  No hay disponibilidad  ')
  writeFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Confirmar decisión' }))
  await waitFor(() => expect(toast.success).toHaveBeenCalledWith('Pedido resuelto'))
  expect(writes()).toHaveLength(2)
  expect(await writes()[1].json()).toEqual({ status: 'REJECTED', comment: 'No hay disponibilidad' })
  expect(new URL(writes()[1].url).pathname).toBe('/api/applications/app-1/visits/visit-1/reschedule-requests/request-1/review')
})

it('aprueba un pedido con el nuevo horario y lugar, omitiendo comentarios vacíos', async () => {
  api.render(<RescheduleRequestDetail id="request-1" canReview currentUserId={1} showVisitLink />)
  await screen.findByText('Consulta médica')
  expect(screen.getByRole('button', { name: 'Ver actuación' })).toBeInTheDocument()
  await chooseDate('Nuevo inicio')
  fireEvent.change(screen.getByLabelText('Hora del nuevo inicio'), { target: { value: '23:59' } })
  await userEvent.type(screen.getByLabelText('Nuevo lugar'), '  Centro sur  ')
  await userEvent.click(screen.getByRole('button', { name: 'Confirmar decisión' }))
  await waitFor(() => expect(toast.success).toHaveBeenCalledWith('Pedido resuelto'))
  expect(await writes()[0].json()).toEqual({ status: 'APPROVED', startsAt: new Date(`${format(new Date(), 'yyyy-MM-dd')}T23:59`).toISOString(), durationMinutes: 60, location: 'Centro sur' })
})
