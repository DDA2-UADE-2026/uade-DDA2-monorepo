import { screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import type { ApplicationResponse } from '@/generated/types.gen'
import { RecentApplications } from './RecentApplications'

const navigate = vi.fn()
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useNavigate: () => navigate,
    Link: ({ children, to, params, ...props }: React.ComponentProps<'a'> & { to: string; params?: Record<string, string> }) => {
      const href = Object.entries(params ?? {}).reduce((path, [key, value]) => path.replace(`$${key}`, value), to)
      return <a href={href} {...props}>{children}</a>
    },
  }
})

let api: ReturnType<typeof apiHarness>
let rows: ApplicationResponse[]
let failApplications: boolean
let failProgramCover: boolean
let applicationStatus = 503

const application = (overrides: Partial<ApplicationResponse>): ApplicationResponse => ({
  id: 'application-1',
  applicationNumber: 1,
  status: 'SUBMITTED',
  programName: 'Apoyo alimentario',
  submittedAt: '2026-11-01T12:00:00Z',
  ...overrides,
}) as ApplicationResponse

beforeEach(() => {
  rows = []
  failApplications = false
  failProgramCover = false
  applicationStatus = 503
  api = apiHarness((request) => {
    const url = new URL(request.url)
    if (url.pathname === '/api/applications') {
      return failApplications
        ? Response.json({ status: applicationStatus, message: 'Servicio temporalmente no disponible' }, { status: applicationStatus })
        : Response.json({ content: rows, totalElements: rows.length, totalPages: 1, number: 0, size: 10 })
    }
    const programMatch = url.pathname.match(/^\/api\/programs\/([^/]+)$/)
    if (programMatch) {
      return failProgramCover
        ? Response.json({ status: 503, message: 'Portada no disponible' }, { status: 503 })
        : Response.json({ id: programMatch[1], name: 'Programa', imageUrl: 'https://example.test/portada.jpg' })
    }
    throw new Error(`Unexpected request ${request.method} ${url.pathname}`)
  })
})

afterEach(() => {
  api.restore()
  vi.restoreAllMocks()
})

describe('RecentApplications', () => {
  it('ordena por fecha, muestra como máximo tres y consulta una sola portada por programa', async () => {
    rows = [
      application({ id: 'old', applicationNumber: 10, programId: 'p-1', submittedAt: '2026-09-01T12:00:00Z', programName: 'Antigua' }),
      application({ id: 'newest', applicationNumber: 13, programId: 'p-1', submittedAt: '2026-11-10T12:00:00Z', programName: 'Más reciente' }),
      application({ id: 'middle', applicationNumber: 12, programId: 'p-2', submittedAt: '2026-10-10T12:00:00Z', programName: 'Intermedia' }),
      application({ id: 'fourth', applicationNumber: 11, programId: 'p-1', submittedAt: '2026-09-15T12:00:00Z', programName: 'Fuera del límite' }),
    ]
    api.render(<RecentApplications />)

    const names = await screen.findAllByText(/Más reciente|Intermedia|Antigua|Fuera del límite/)
    expect(names.map((node) => node.textContent)).toEqual(['Más reciente', 'Intermedia', 'Fuera del límite'])
    expect(screen.queryByText('Antigua')).not.toBeInTheDocument()
    await waitFor(() => expect(api.requests.filter((request) => new URL(request.url).pathname.startsWith('/api/programs/'))).toHaveLength(2))
    expect(api.requests.filter((request) => new URL(request.url).pathname === '/api/applications')).toHaveLength(1)
    expect(screen.getByRole('button', { name: 'Ver solicitud N.º 13' })).toBeInTheDocument()
  })

  it('muestra el estado vacío y ofrece ir a los programas', async () => {
    api.render(<RecentApplications />)

    expect(await screen.findByText('Todavía no presentaste solicitudes')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver programas' })).toBeInTheDocument()
  })

  it('prioriza el mensaje específico para una respuesta 404', async () => {
    failApplications = true
    applicationStatus = 404
    api.render(<RecentApplications />)

    expect(await screen.findByText('No encontramos la solicitud o el programa')).toBeInTheDocument()
    expect(screen.getByText('Servicio temporalmente no disponible')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('usa textos de respaldo y no muestra acción de detalle si la respuesta no tiene identificador', async () => {
    rows = [application({ id: undefined, programId: undefined, applicationNumber: 44, programName: undefined, status: null as unknown as ApplicationResponse['status'], submittedAt: undefined })]
    api.render(<RecentApplications />)

    expect(await screen.findByText('Solicitud de programa')).toBeInTheDocument()
    expect(screen.getByText('N.º 44')).toBeInTheDocument()
    expect(screen.getByText('Sin estado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Ver solicitud/ })).not.toBeInTheDocument()
    expect(api.requests.some((request) => new URL(request.url).pathname.startsWith('/api/programs/'))).toBe(false)
  })

  it('permite reintentar una carga fallida y conserva una imagen genérica si falla la portada', async () => {
    failApplications = true
    api.render(<RecentApplications />)
    expect(await screen.findByText('Servicio temporalmente no disponible')).toBeInTheDocument()

    failApplications = false
    rows = [application({ id: 'recover', applicationNumber: 44, programId: 'program-failed' })]
    failProgramCover = true
    screen.getByRole('button', { name: 'Reintentar' }).click()

    expect(await screen.findByText('Apoyo alimentario')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver solicitud N.º 44' })).toBeInTheDocument()
    expect(api.requests.filter((request) => new URL(request.url).pathname === '/api/applications')).toHaveLength(2)
    await waitFor(() => expect(api.requests.some((request) => new URL(request.url).pathname === '/api/programs/program-failed')).toBe(true))
  })
})
