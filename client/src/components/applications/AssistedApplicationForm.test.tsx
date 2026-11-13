import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { enrollmentToday } from '@/lib/application-flow'
import { AssistedApplicationForm } from '@/routes/_app/gestion/solicitudes/asistida'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#details">{children}</a>,
}))

let api: ReturnType<typeof apiHarness>
let submissionFails: boolean
let programFails: boolean
let available: boolean
const response = { id: 'app-1', applicationNumber: 72, status: 'SUBMITTED', programName: 'Apoyo alimentario', programEditionName: 'Edición vigente', submittedAt: '2026-11-13T12:00:00Z' }

beforeEach(() => {
  submissionFails = false; programFails = false; available = true
  sessionStorage.clear()
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  api = apiHarness(request => {
    const url = new URL(request.url)
    if (url.pathname === '/users') return Response.json([
      { id: 1, name: 'Operador actual', username: 'operador', active: true },
      { id: 2, name: 'Ana Prueba', username: 'ana', email: 'ana@example.test', active: true },
    ])
    if (url.pathname === '/api/programs') return Response.json({ content: [{ id: 'program-1', name: 'Apoyo alimentario', availableEditions: 1 }], totalElements: 1, totalPages: 1 })
    if (url.pathname === '/api/programs/program-1') return programFails
      ? Response.json({ message: 'Catálogo no disponible' }, { status: 503 })
      : Response.json({ id: 'program-1', name: 'Apoyo alimentario', editions: [{
        id: 'edition-1', name: 'Edición vigente', status: 'ACTIVE',
        documentRequirements: [{ id: 'dni', name: 'DNI', required: true }],
        enrollmentPeriods: [{ id: 'period-1', status: available ? 'OPEN' : 'CLOSED', openDate: enrollmentToday(), closeDate: enrollmentToday() }],
      }] })
    if (url.pathname === '/api/admin/applications') return submissionFails
      ? Response.json({ status: 409, message: 'La convocatoria cambió' }, { status: 409 }) : Response.json(response, { status: 201 })
    throw new Error(`Unexpected request ${request.method} ${url.pathname}`)
  })
})
afterEach(() => { api.restore(); sessionStorage.clear(); vi.unstubAllGlobals(); vi.restoreAllMocks() })

async function choosePerson() {
  const user = userEvent.setup()
  await user.click(screen.getByRole('button', { name: 'Persona solicitante' }))
  const row = (await screen.findByText('Ana Prueba')).closest('tr')!
  expect(screen.queryByText('Operador actual')).not.toBeInTheDocument()
  await user.click(within(row).getByRole('button', { name: 'Seleccionar' }))
}
async function chooseProgram() {
  const user = userEvent.setup()
  await user.click(screen.getByRole('button', { name: 'Programa' }))
  const row = (await screen.findByText('Apoyo alimentario')).closest('tr')!
  await user.click(within(row).getByRole('button', { name: 'Seleccionar' }))
}
async function choosePeriod() {
  const user = userEvent.setup()
  await waitFor(() => expect(screen.getByRole('combobox')).toBeEnabled())
  await user.click(screen.getByRole('combobox'))
  await user.click(await screen.findByRole('option', { name: /Edición vigente/ }))
}

it('no permite registrar sin persona y convocatoria y excluye al operador del selector', async () => {
  api.render(<AssistedApplicationForm currentUserId={1} />)
  expect(screen.getByRole('button', { name: 'Registrar solicitud' })).toBeDisabled()
  expect(screen.getByRole('combobox')).toBeDisabled()
  expect(api.requests).toHaveLength(0)
  await choosePerson()
  expect(screen.getByRole('button', { name: 'Persona solicitante' })).toHaveTextContent('Ana Prueba')
  expect(screen.getByRole('button', { name: 'Registrar solicitud' })).toBeDisabled()
})

it('registra para el titular elegido, muestra confirmación y permite iniciar otra presentación', async () => {
  api.render(<AssistedApplicationForm currentUserId={1} />)
  await choosePerson(); await chooseProgram(); await choosePeriod()
  expect(screen.getByRole('heading', { name: 'Resumen de la presentación' })).toBeInTheDocument()
  expect(screen.getByText('1 obligatorio')).toBeInTheDocument()
  await userEvent.setup().click(screen.getByRole('button', { name: 'Registrar solicitud' }))
  expect(await screen.findByText('Solicitud registrada')).toBeInTheDocument()
  expect(screen.getByText('N.º 72')).toBeInTheDocument()
  expect(screen.getByText('Presentada')).toBeInTheDocument()
  const write = api.requests.find(r => r.method === 'POST')!
  expect(await write.json()).toEqual({ userId: 2, enrollmentPeriodId: 'period-1' })
  expect(write.headers.get('Idempotency-Key')).toBeTruthy()
  await userEvent.setup().click(screen.getByRole('button', { name: 'Registrar otra solicitud' }))
  expect(screen.getByRole('button', { name: 'Persona solicitante' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Registrar solicitud' })).toBeDisabled()
})

it('mantiene las elecciones y la misma clave de idempotencia al reintentar un fallo', async () => {
  submissionFails = true
  api.render(<AssistedApplicationForm currentUserId={1} />)
  await choosePerson(); await chooseProgram(); await choosePeriod()
  const user = userEvent.setup()
  await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))
  expect(await screen.findByText('La convocatoria cambió')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Persona solicitante' })).toHaveTextContent('Ana Prueba')
  submissionFails = false
  await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))
  expect(await screen.findByText('Solicitud registrada')).toBeInTheDocument()
  const writes = api.requests.filter(r => r.method === 'POST')
  expect(writes).toHaveLength(2)
  expect(writes[0].headers.get('Idempotency-Key')).toBe(writes[1].headers.get('Idempotency-Key'))
})

it('informa que el programa no tiene períodos vigentes y mantiene bloqueada la presentación', async () => {
  available = false
  api.render(<AssistedApplicationForm currentUserId={1} />)
  await choosePerson(); await chooseProgram()
  expect(await screen.findByText('Sin convocatorias habilitadas')).toBeInTheDocument()
  expect(screen.getByRole('combobox')).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Registrar solicitud' })).toBeDisabled()
})

it('permite recuperar un error al consultar convocatorias', async () => {
  programFails = true
  api.render(<AssistedApplicationForm currentUserId={1} />)
  await chooseProgram()
  expect(await screen.findByText('No se pudieron cargar las convocatorias.')).toBeInTheDocument()
  programFails = false
  await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }))
  await waitFor(() => expect(screen.getByRole('combobox')).toBeEnabled())
  expect(screen.getByRole('button', { name: 'Registrar solicitud' })).toBeDisabled()
})
