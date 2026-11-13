import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { ApplicationBenefits } from './ApplicationBenefits'

vi.mock('@tanstack/react-router', async importOriginal => ({ ...await importOriginal<typeof import('@tanstack/react-router')>(), Link: ({ children }: { children?: ReactNode }) => <a href="#benefit">{children}</a> }))
let api: ReturnType<typeof apiHarness>
let fails: boolean
let readFails: boolean
let emptyCatalog: boolean
beforeEach(() => {
  fails = false; readFails = false; emptyCatalog = false
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  api = apiHarness(request => {
    if (request.method === 'POST') return fails ? Response.json({ message: 'Vigencia fuera de la edición' }, { status: 409 }) : Response.json({ id: 'grant-new' })
    if (new URL(request.url).pathname.endsWith('/benefits')) return Response.json(emptyCatalog ? [] : [{ id: 'benefit-1', benefitType: 'FOOD_ASSISTANCE', amount: 1500 }])
    return readFails ? Response.json({ message: 'Beneficios inaccesibles' }, { status: 503 }) : Response.json([])
  })
})
afterEach(() => { api.restore(); vi.unstubAllGlobals() })
const application = { id: 'app-1', status: 'APPROVED' as const, programEditionId: 'edition-1' }
async function fill() {
  await userEvent.click(screen.getByRole('button', { name: 'Otorgar beneficio' }))
  await waitFor(() => expect(screen.getByRole('combobox')).toBeEnabled())
  await userEvent.click(screen.getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: /Asistencia alimentaria/ }))
  for (const label of ['Inicio de vigencia', 'Fin de vigencia']) {
    await userEvent.click(within(screen.getByText(label).closest('label')!).getByRole('button'))
    await userEvent.click(await screen.findByRole('button', { name: new RegExp(format(new Date(), "d 'de' MMMM 'de' yyyy", { locale: es })) }))
  }
}

it.each([{ allowed: false, status: 'APPROVED' as const }, { allowed: true, status: 'IN_EVALUATION' as const }])('no ofrece otorgamiento sin permiso y solicitud aprobada: %j', async ({ allowed, status }) => {
  api.render(<ApplicationBenefits application={{ ...application, status }} canAssign={allowed} />)
  await screen.findByText('Esta solicitud todavía no tiene beneficios otorgados.')
  expect(screen.queryByRole('button', { name: 'Otorgar beneficio' })).not.toBeInTheDocument()
})

it('requiere beneficio y ambas fechas y envía el contrato de otorgamiento', async () => {
  api.render(<ApplicationBenefits application={application} canAssign />)
  await fill()
  await userEvent.click(screen.getByRole('button', { name: 'Otorgar' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  const request = api.requests.find(request => request.method === 'POST')!
  expect(new URL(request.url).pathname).toBe('/api/admin/applications/app-1/benefit-grants')
  expect(await request.json()).toEqual({ programBenefitId: 'benefit-1', startDate: format(new Date(), 'yyyy-MM-dd'), endDate: format(new Date(), 'yyyy-MM-dd') })
})

it('mantiene el diálogo ante un rechazo y permite reintentar con los datos elegidos', async () => {
  fails = true
  api.render(<ApplicationBenefits application={application} canAssign />)
  await fill()
  await userEvent.click(screen.getByRole('button', { name: 'Otorgar' }))
  expect(await screen.findByText('Vigencia fuera de la edición')).toBeInTheDocument()
  expect(screen.getByRole('combobox')).toHaveTextContent('Asistencia alimentaria')
  fails = false
  await userEvent.click(screen.getByRole('button', { name: 'Otorgar' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  expect(api.requests.filter(request => request.method === 'POST')).toHaveLength(2)
})

it('explica una convocatoria sin beneficios y evita el envío incompleto', async () => {
  emptyCatalog = true
  api.render(<ApplicationBenefits application={application} canAssign />)
  await userEvent.click(screen.getByRole('button', { name: 'Otorgar beneficio' }))
  expect(await screen.findByText('La convocatoria no tiene beneficios configurados.')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Otorgar' })).toBeDisabled()
  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  expect(api.requests.filter(request => request.method === 'POST')).toHaveLength(0)
})

it('permite recuperar el listado después de un error de carga', async () => {
  readFails = true
  api.render(<ApplicationBenefits application={application} canAssign={false} />)
  expect(await screen.findByText('Beneficios inaccesibles')).toBeInTheDocument()
  readFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText('Esta solicitud todavía no tiene beneficios otorgados.')).toBeInTheDocument()
})
