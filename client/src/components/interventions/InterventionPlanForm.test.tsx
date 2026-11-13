import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { get4QueryKey } from '@/generated/@tanstack/react-query.gen'
import { InterventionPlanForm } from './InterventionPlanForm'

let api: ReturnType<typeof apiHarness>
let fails: boolean
const saved = { id: 'plan-1', applicationId: 'app-1', objectives: [] }
beforeEach(() => {
  fails = false
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  api = apiHarness(() => fails ? Response.json({ message: 'La solicitud no está aprobada' }, { status: 409 }) : Response.json(saved))
})
afterEach(() => { api.restore(); vi.unstubAllGlobals() })
async function chooseToday(label: string) {
  await userEvent.click(screen.getByRole('button', { name: label }))
  await userEvent.click(await screen.findByRole('button', { name: new RegExp(format(new Date(), "d 'de' MMMM 'de' yyyy", { locale: es })) }))
}
async function fill() {
  await userEvent.type(screen.getByLabelText('Descripción del objetivo'), '  Mejorar la autonomía  ')
  await userEvent.type(screen.getByLabelText('Descripción'), '  Entrevista inicial  ')
  await chooseToday('Inicio')
  await chooseToday('Fin previsto')
}

it('rechaza un plan incompleto sin consultar al servidor', async () => {
  api.render(<InterventionPlanForm applicationId="app-1" />)
  await userEvent.click(screen.getByRole('button', { name: 'Crear plan' }))
  expect(screen.getByText('Completá las descripciones y fechas. La fecha de fin no puede ser anterior al inicio.')).toBeInTheDocument()
  expect(api.requests).toHaveLength(0)
})

it('agrega y elimina objetivos y acciones conservando los datos de los demás', async () => {
  api.render(<InterventionPlanForm applicationId="app-1" />)
  await userEvent.type(screen.getByLabelText('Descripción del objetivo'), 'Objetivo principal')
  expect(screen.queryByRole('button', { name: 'Eliminar objetivo 1' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Eliminar acción 1' })).not.toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Agregar acción' }))
  await userEvent.type(screen.getAllByLabelText('Descripción')[1], 'Acción temporal')
  await userEvent.click(screen.getByRole('button', { name: 'Eliminar acción 2' }))
  expect(screen.getAllByLabelText('Descripción')).toHaveLength(1)
  await userEvent.click(screen.getByRole('button', { name: 'Agregar objetivo' }))
  await userEvent.type(screen.getAllByLabelText('Descripción del objetivo')[1], 'Objetivo temporal')
  await userEvent.click(screen.getByRole('button', { name: 'Eliminar objetivo 2' }))
  expect(screen.getByLabelText('Descripción del objetivo')).toHaveValue('Objetivo principal')
  expect(api.requests).toHaveLength(0)
})

it('crea un plan con fechas elegidas, textos normalizados y sin identificadores internos del borrador', async () => {
  const created = vi.fn()
  api.render(<InterventionPlanForm applicationId="app-1" onCreated={created} />)
  await fill()
  await userEvent.click(screen.getByRole('button', { name: 'Crear plan' }))
  await waitFor(() => expect(created).toHaveBeenCalledWith(saved))
  expect(api.requests).toHaveLength(1)
  expect(new URL(api.requests[0].url).pathname).toBe('/api/applications/app-1/intervention-plan')
  expect(api.requests[0].method).toBe('POST')
  const date = format(new Date(), 'yyyy-MM-dd')
  expect(await api.requests[0].json()).toEqual({ objectives: [{ description: 'Mejorar la autonomía', actions: [{ description: 'Entrevista inicial', startDate: date, dueDate: date }] }] })
  expect(api.queryClient.getQueryData(get4QueryKey({ path: { id: 'app-1' } }))).toEqual(saved)
})

it('conserva el borrador tras un error del servidor y permite reintentar', async () => {
  fails = true
  const created = vi.fn()
  api.render(<InterventionPlanForm applicationId="app-1" onCreated={created} />)
  await fill()
  await userEvent.click(screen.getByRole('button', { name: 'Crear plan' }))
  expect(await screen.findByText('La solicitud no está aprobada')).toBeInTheDocument()
  expect(created).not.toHaveBeenCalled()
  expect(screen.getByLabelText('Descripción del objetivo')).toHaveValue('  Mejorar la autonomía  ')
  fails = false
  await userEvent.click(screen.getByRole('button', { name: 'Crear plan' }))
  await waitFor(() => expect(created).toHaveBeenCalledWith(saved))
  expect(api.requests).toHaveLength(2)
})
