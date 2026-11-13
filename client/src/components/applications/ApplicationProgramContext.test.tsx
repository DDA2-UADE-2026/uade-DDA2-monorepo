import { screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { ApplicationProgramContext } from './ApplicationProgramContext'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#context">{children}</a>,
}))
let api: ReturnType<typeof apiHarness>
let fails: boolean
let empty: boolean
beforeEach(() => {
  fails = false; empty = false
  api = apiHarness(request => {
    const path = new URL(request.url).pathname
    if (path === '/api/admin/programs/program-1') return Response.json({ name: 'Alimentos', objective: 'Mejorar la alimentación' })
    if (path === '/api/admin/program-editions/edition-1') return Response.json({ name: 'Edición anual', status: 'ACTIVE', startDate: '2026-01-01', endDate: '2026-12-31', currentEnrollment: 12, maxCapacity: 30 })
    if (path.endsWith('/requirements')) return fails ? Response.json({ message: 'Requirements unavailable' }, { status: 503 }) : Response.json(empty ? [] : [{ id: 'req-1', type: 'MIN_AGE', value: '18', description: 'Ser mayor de edad' }])
    if (path.endsWith('/benefits')) return fails ? Response.json({ message: 'Benefits unavailable' }, { status: 503 }) : Response.json(empty ? [] : [{ id: 'benefit-1', benefitType: 'FOOD_ASSISTANCE', amount: 1500, description: 'Entrega mensual' }])
    throw new Error(`Unexpected request ${path}`)
  })
})
afterEach(() => api.restore())
const application = { id: 'app-1', userId: 2, userName: 'Ana Prueba', registeredByUserId: 3, registeredByUserName: 'Operador prueba', programId: 'program-1', programEditionId: 'edition-1' }

it('relaciona titular y registrante con objetivo, edición, requisitos, cupos y beneficios consultados', async () => {
  api.render(<ApplicationProgramContext application={application} />)
  expect(await screen.findByText('Mejorar la alimentación')).toBeInTheDocument()
  expect(await screen.findByText('Ser mayor de edad')).toBeInTheDocument()
  expect(await screen.findByText('Entrega mensual')).toBeInTheDocument()
  expect(screen.getByText('Ana Prueba')).toBeInTheDocument()
  expect(screen.getByText('Operador prueba')).toBeInTheDocument()
  expect(screen.getByText('12 / 30')).toBeInTheDocument()
  expect(screen.getByText('Alimentos')).toBeInTheDocument()
  expect(screen.getByText('Edición anual')).toBeInTheDocument()
  expect(screen.getByText('Edad mínima')).toBeInTheDocument()
  expect(screen.getByText('Asistencia alimentaria')).toBeInTheDocument()
  expect(api.requests).toHaveLength(4)
})

it('informa errores de requisitos y beneficios sin perder el contexto de la solicitud', async () => {
  fails = true
  api.render(<ApplicationProgramContext application={application} />)
  expect(await screen.findByText('No se pudieron cargar los requisitos.')).toBeInTheDocument()
  expect(await screen.findByText('No se pudieron cargar los beneficios.')).toBeInTheDocument()
  expect(screen.getByText('Ana Prueba')).toBeInTheDocument()
  expect(screen.getByText('Alimentos')).toBeInTheDocument()
})

it('distingue un catálogo vacío de un error de carga', async () => {
  empty = true
  api.render(<ApplicationProgramContext application={application} />)
  expect(await screen.findByText('Sin requisitos.')).toBeInTheDocument()
  expect(await screen.findByText('Sin beneficios.')).toBeInTheDocument()
})

it('no hace consultas sin identificadores ni atribuye registro asistido al mismo titular', () => {
  api.render(<ApplicationProgramContext application={{ userId: 2, registeredByUserId: 2, programName: 'Programa histórico' }} />)
  expect(api.requests).toHaveLength(0)
  expect(screen.getByText('Programa histórico')).toBeInTheDocument()
  expect(screen.getByText('Sin objetivo cargado.')).toBeInTheDocument()
  expect(screen.getByText('Sin asignar')).toBeInTheDocument()
  expect(screen.queryByText('Registrada por')).not.toBeInTheDocument()
  expect(screen.queryByText('Requisitos de la edición')).not.toBeInTheDocument()
})
