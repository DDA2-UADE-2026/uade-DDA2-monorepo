import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { apiHarness } from '@/test/api-harness'
import { get3QueryKey } from '@/generated/@tanstack/react-query.gen'
import type { AdminApplicationResponse } from '@/generated/types.gen'
import { AssignedWorkerCard } from './AssignedWorkerCard'

let api: ReturnType<typeof apiHarness>
let application: AdminApplicationResponse
let fails: boolean
const grants = ['applications:management:assign', 'users:view']
beforeEach(() => {
  fails = false
  application = { id: 'app-1', status: 'IN_EVALUATION', userId: 2, assignedWorkerUserId: 1, assignedWorkerName: 'Profesional actual' }
  api = apiHarness(request => {
    if (request.method === 'GET') return Response.json([{ id: 2, name: 'Titular', active: true }, { id: 1, name: 'Profesional actual', active: true }, { id: 3, name: 'Nueva profesional', active: true }])
    if (fails) return Response.json({ status: 409, message: 'Tiene visitas programadas' }, { status: 409 })
    return Response.json({ ...application, assignedWorkerUserId: request.method === 'DELETE' ? null : 3, assignedWorkerName: request.method === 'DELETE' ? null : 'Nueva profesional' })
  })
})
afterEach(() => { api.restore(); vi.restoreAllMocks() })
const writes = () => api.requests.filter(request => request.method !== 'GET')
const renderCard = (permissions = grants, evaluated = false) => api.render(<AssignedWorkerCard application={application} permissions={permissions} evaluated={evaluated} />)

it.each(['APPROVED', 'REJECTED', 'DRAFT'] as const)('impide reasignar solicitudes en estado %s', async status => {
  application.status = status
  renderCard()
  expect(screen.getByText('Profesional actual')).toBeInTheDocument()
  expect(screen.queryByRole('button')).not.toBeInTheDocument()
  expect(writes()).toHaveLength(0)
})

it('exige permiso de usuarios para elegir otro trabajador y no permite desasignar después de evaluar', () => {
  renderCard(['applications:management:assign'], true)
  expect(screen.queryByRole('button', { name: 'Reasignar' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Asignar automáticamente' })).not.toBeInTheDocument()
})

it('excluye al titular y asigna una persona distinta actualizando el detalle en caché', async () => {
  renderCard()
  await userEvent.click(screen.getByRole('button', { name: 'Reasignar' }))
  await screen.findByText('Nueva profesional')
  expect(screen.queryByText('Titular')).not.toBeInTheDocument()
  const row = screen.getByText('Nueva profesional').closest('tr')!
  await userEvent.click(within(row).getByRole('button', { name: 'Seleccionar' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  expect(writes()).toHaveLength(1)
  expect(writes()[0].method).toBe('PUT')
  expect(new URL(writes()[0].url).pathname).toBe('/api/admin/applications/app-1/assigned-worker')
  expect(await writes()[0].json()).toEqual({ userId: 3 })
  expect(api.queryClient.getQueryData(get3QueryKey({ path: { id: 'app-1' } }))).toMatchObject({ assignedWorkerUserId: 3, assignedWorkerName: 'Nueva profesional' })
})

it('vuelve a la asignación automática antes de la evaluación', async () => {
  renderCard()
  await userEvent.click(screen.getByRole('button', { name: 'Asignar automáticamente' }))
  await waitFor(() => expect(api.queryClient.getQueryData(get3QueryKey({ path: { id: 'app-1' } }))).toMatchObject({ assignedWorkerUserId: null }))
  expect(writes()[0].method).toBe('DELETE')
})

it('no reemplaza el trabajador en caché cuando el servidor rechaza la reasignación', async () => {
  fails = true
  const error = vi.spyOn(toast, 'error')
  api.queryClient.setQueryData(get3QueryKey({ path: { id: 'app-1' } }), application)
  renderCard()
  await userEvent.click(screen.getByRole('button', { name: 'Reasignar' }))
  const row = (await screen.findByText('Nueva profesional')).closest('tr')!
  await userEvent.click(within(row).getByRole('button', { name: 'Seleccionar' }))
  await waitFor(() => expect(error).toHaveBeenCalledWith('No se pudo completar la operación', { description: 'Tiene visitas programadas' }))
  expect(api.queryClient.getQueryData(get3QueryKey({ path: { id: 'app-1' } }))).toMatchObject({ assignedWorkerUserId: 1 })
})
