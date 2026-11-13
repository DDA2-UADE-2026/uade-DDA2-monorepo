import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import type { CitizenVisit } from '@/lib/citizen-visits'
import { UpcomingVisits } from './UpcomingVisits'

const permissions = vi.hoisted(() => ({ values: ['applications:own:visits:view'] as string[] }))
vi.mock('@/hooks/use-auth', () => ({ useMe: () => ({ data: { user: { permissions: permissions.values } } }) }))
vi.mock('@tanstack/react-router', async (importOriginal) => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, to, ...props }: React.ComponentProps<'a'> & { to: string }) => <a href={to} {...props}>{children}</a>,
}))

let api: ReturnType<typeof apiHarness>
let visits: CitizenVisit[]
let listFails: boolean
let detailFails: boolean
const visit: CitizenVisit = {
  id: 'visit-1', applicationId: 'application-1', applicationNumber: 87, programName: 'Apoyo alimentario',
  type: 'VISIT', startsAt: '2099-03-15T10:00:00Z', endsAt: '2099-03-15T11:00:00Z', durationMinutes: 60,
  location: 'Centro norte', status: 'SCHEDULED', rescheduled: false, lastRescheduledAt: null,
  latestRescheduleRequest: null, cancelledAt: null,
}

beforeEach(() => {
  permissions.values = ['applications:own:visits:view']
  visits = []
  listFails = false
  detailFails = false
  api = apiHarness((request) => {
    const path = new URL(request.url).pathname
    if (path === '/api/applications/visits') {
      return listFails
        ? Response.json({ status: 503, message: 'Agenda temporalmente inaccesible' }, { status: 503 })
        : Response.json({ content: visits, page: 0, size: 3, totalElements: visits.length, totalPages: 1 })
    }
    if (path === '/api/applications/application-1/visits/visit-1') {
      return detailFails
        ? Response.json({ status: 503, message: 'Detalle temporalmente inaccesible' }, { status: 503 })
        : Response.json(visit)
    }
    if (path === '/api/applications/application-1/visits/visit-1/reschedule-requests') {
      return Response.json({ content: [], page: 0, size: 5, totalElements: 0, totalPages: 1 })
    }
    throw new Error(`Unexpected request ${request.method} ${path}`)
  })
})

afterEach(() => api.restore())

describe('UpcomingVisits', () => {
  it('no consulta ni presenta visitas sin el permiso específico', async () => {
    permissions.values = []
    api.render(<UpcomingVisits />)

    expect(api.requests).toHaveLength(0)
    expect(screen.queryByRole('region', { name: 'Próximas visitas y entrevistas' })).not.toBeInTheDocument()
    expect(api.requests.some((request) => new URL(request.url).pathname === '/api/applications/visits')).toBe(false)
  })

  it('oculta la sección cuando el listado autorizado no tiene próximas visitas', async () => {
    api.render(<UpcomingVisits />)

    await waitFor(() => expect(api.requests.some((request) => new URL(request.url).pathname === '/api/applications/visits')).toBe(true))
    expect(screen.queryByRole('region', { name: 'Próximas visitas y entrevistas' })).not.toBeInTheDocument()
  })

  it('lista visitas y abre el detalle vinculado a la solicitud', async () => {
    visits = [visit]
    api.render(<UpcomingVisits />)

    expect(await screen.findByText('Centro norte')).toBeInTheDocument()
    expect(screen.getByText('Apoyo alimentario · Solicitud N.º 87')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver todas' })).toHaveAttribute('href', '/portal/visitas')
    await userEvent.click(screen.getByRole('button', { name: 'Ver detalle' }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Historial de pedidos')).toBeInTheDocument()
    expect(api.requests.some((request) => new URL(request.url).pathname.endsWith('/visit-1/reschedule-requests'))).toBe(true)
  })

  it('permite reintentar la agenda y el detalle después de errores', async () => {
    listFails = true
    api.render(<UpcomingVisits />)
    expect(await screen.findByText('Agenda temporalmente inaccesible')).toBeInTheDocument()
    listFails = false
    visits = [visit]
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Ver detalle' }))

    detailFails = true
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Ver detalle' }))
    expect(await screen.findByText('Detalle temporalmente inaccesible')).toBeInTheDocument()
    detailFails = false
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('Historial de pedidos')).toBeInTheDocument()
  })
})
