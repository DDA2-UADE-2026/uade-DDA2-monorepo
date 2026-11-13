import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { apiHarness } from '@/test/api-harness'
import type { CitizenActivityResponse } from '@/generated/types.gen'
import { CitizenActivitiesSection } from './CitizenActivitiesSection'

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ children, to, params, ...props }: React.ComponentProps<'a'> & { to: string; params?: Record<string, string> }) => {
      const href = Object.entries(params ?? {}).reduce((path, [key, value]) => path.replace(`$${key}`, value), to)
      return <a href={href} {...props}>{children}</a>
    },
  }
})

let api: ReturnType<typeof apiHarness>
let activities: CitizenActivityResponse[]
let enrollmentFails: boolean
let listingFails: boolean

const activity = (overrides: Partial<CitizenActivityResponse> = {}): CitizenActivityResponse => ({
  id: 'activity-1',
  name: 'Taller de huerta',
  description: 'Encuentro comunitario',
  location: 'Centro barrial',
  startDate: '2026-12-01T12:00:00Z',
  endDate: '2026-12-01T14:00:00Z',
  status: 'OPEN',
  capacity: 12,
  availableCapacity: 9,
  ...overrides,
}) as CitizenActivityResponse

beforeEach(() => {
  activities = []
  enrollmentFails = false
  listingFails = false
  vi.spyOn(toast, 'success').mockImplementation(() => 'success')
  api = apiHarness((request) => {
    const path = new URL(request.url).pathname
    if (path === '/api/activities' && request.method === 'GET') {
      return listingFails
        ? Response.json({ status: 503, message: 'Actividades temporalmente inaccesibles' }, { status: 503 })
        : Response.json({ content: activities, totalElements: activities.length, totalPages: 1, number: 0, size: 3 })
    }
    if (path === '/api/activities/activity-1/enrollments' && request.method === 'POST') {
      return enrollmentFails
        ? Response.json({ status: 409, message: 'La actividad ya no tiene cupos' }, { status: 409 })
        : Response.json({ activityId: 'activity-1' }, { status: 201 })
    }
    throw new Error(`Unexpected request ${request.method} ${path}`)
  })
})

afterEach(() => {
  api.restore()
  vi.restoreAllMocks()
})

describe('CitizenActivitiesSection', () => {
  it('indica cuando no hay actividades disponibles', async () => {
    api.render(<CitizenActivitiesSection canEnroll />)

    expect(await screen.findByText('No hay actividades abiertas')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver todas' })).toHaveAttribute('href', '/portal/actividades')
  })

  it('oculta la inscripción para quien no tiene permiso para inscribirse', async () => {
    activities = [activity()]
    api.render(<CitizenActivitiesSection canEnroll={false} />)

    expect(await screen.findByText('Taller de huerta')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Inscribirme' })).not.toBeInTheDocument()
  })

  it('no renderiza el panel si falla la lectura del listado', async () => {
    listingFails = true
    api.render(<CitizenActivitiesSection canEnroll />)

    await waitFor(() => expect(api.requests).toHaveLength(1))
    expect(screen.queryByRole('region', { name: 'Actividades comunitarias' })).not.toBeInTheDocument()
    expect(screen.queryByText('Actividades temporalmente inaccesibles')).not.toBeInTheDocument()
  })

  it('deshabilita inscripción cuando el registro no tiene identificador', async () => {
    activities = [activity({ id: undefined, name: 'Actividad sin identificador' })]
    api.render(<CitizenActivitiesSection canEnroll />)

    expect(await screen.findByText('Actividad sin identificador')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Inscribirme' })).toBeDisabled()
  })

  it('deshabilita la inscripción cuando se agotaron los cupos', async () => {
    activities = [activity({ name: 'Cupos completos', capacity: 3, availableCapacity: 0 })]
    api.render(<CitizenActivitiesSection canEnroll />)
    const fullCard = (await screen.findByText('Cupos completos')).closest('[data-slot="card"]') as HTMLElement
    expect(within(fullCard).getByRole('button', { name: 'Sin cupo' })).toBeDisabled()
  })

  it('confirma la inscripción, refresca la lista y muestra el acceso al detalle', async () => {
    activities = [activity()]
    api.render(<CitizenActivitiesSection canEnroll />)

    await userEvent.click(await screen.findByRole('button', { name: 'Inscribirme' }))
    expect(await screen.findByRole('dialog')).toHaveTextContent('Taller de huerta')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar inscripción' }))

    expect(await screen.findByText('Ya estás inscripto')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Ver detalle/ })).toHaveAttribute('href', '/portal/actividades/activity-1')
    expect(api.requests.some((request) => request.method === 'POST' && new URL(request.url).pathname.endsWith('/activity-1/enrollments'))).toBe(true)
    expect(toast.success).toHaveBeenCalledWith('Te inscribiste en la actividad')
  })

  it('expone un conflicto de inscripción y permite volver a confirmarla', async () => {
    activities = [activity()]
    enrollmentFails = true
    api.render(<CitizenActivitiesSection canEnroll />)

    await userEvent.click(await screen.findByRole('button', { name: 'Inscribirme' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar inscripción' }))
    expect(await screen.findByText('La actividad ya no tiene cupos')).toBeInTheDocument()

    enrollmentFails = false
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar inscripción' }))
    expect(await screen.findByText('Ya estás inscripto')).toBeInTheDocument()
    await waitFor(() => expect(api.requests.filter((request) => request.method === 'POST')).toHaveLength(2))
  })
})
