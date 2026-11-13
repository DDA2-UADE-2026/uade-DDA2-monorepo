import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { todayLocal } from './InterventionUi'
import { InterventionFollowUps } from './InterventionFollowUps'

let api: ReturnType<typeof apiHarness>
let readFails: boolean
let writeFails: boolean
let entries: object[]
const path = '/api/applications/app-1/intervention-plan/actions/action-1/follow-ups'
const historyPath = '/api/admin/applications/app-1/intervention-plan/actions/action-1/follow-ups'
const entry = { id: 'follow-1', sequenceNumber: 1, status: 'IN_PROGRESS', occurredOn: todayLocal(), notes: 'Notas profesionales reservadas', result: 'Resultado privado', authorUserName: 'Profesional prueba' }
beforeEach(() => {
  readFails = false; writeFails = false; entries = [entry]
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  api = apiHarness(request => {
    if (request.method === 'GET' && new URL(request.url).pathname === historyPath) return readFails ? Response.json({ message: 'Historial inaccesible' }, { status: 503 }) : Response.json(entries)
    if (request.method !== 'GET') return writeFails ? Response.json({ message: 'Conflicto de seguimiento' }, { status: 409 }) : Response.json({ id: 'saved' })
    throw new Error(`Unexpected request ${request.method} ${request.url}`)
  })
})
afterEach(() => { api.restore(); vi.unstubAllGlobals() })
const renderHistory = (canCreate = true, canPublish = true, status: 'IN_PROGRESS' | 'COMPLETED' = 'IN_PROGRESS') => api.render(<InterventionFollowUps applicationId="app-1" action={{ id: 'action-1', description: 'Acceso a alimentos', status }} canCreate={canCreate} canPublish={canPublish} onClose={vi.fn()} />)
const writes = () => api.requests.filter(request => request.method !== 'GET')

it('presenta el historial profesional sin habilitar escritura cuando faltan permisos', async () => {
  renderHistory(false, false)
  expect(await screen.findByText(entry.notes)).toBeInTheDocument()
  expect(screen.getByText(entry.result)).toBeInTheDocument()
  expect(screen.getByText('Privada')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Registrar seguimiento' })).not.toBeInTheDocument()
  expect(screen.queryByTitle('Publicar resumen')).not.toBeInTheDocument()
})

it('impide seguir una acción completada y muestra el historial vacío', async () => {
  entries = []
  renderHistory(true, true, 'COMPLETED')
  expect(await screen.findByText('Sin seguimientos')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Registrar seguimiento' })).not.toBeInTheDocument()
})

it('reintenta la lectura del historial después de un error', async () => {
  readFails = true
  renderHistory()
  expect(await screen.findByText('Historial inaccesible')).toBeInTheDocument()
  readFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText(entry.notes)).toBeInTheDocument()
})

it('exige notas y resultado al completar y envía el seguimiento normalizado', async () => {
  renderHistory()
  await userEvent.click(screen.getByRole('button', { name: 'Registrar seguimiento' }))
  const dialog = screen.getByRole('dialog', { name: 'Registrar seguimiento' })
  expect(within(dialog).getByRole('button', { name: 'Registrar' })).toBeDisabled()
  await userEvent.type(within(dialog).getByLabelText('Notas profesionales'), '  Se realizó la visita  ')
  await userEvent.click(within(dialog).getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: 'Completada' }))
  expect(within(dialog).getByRole('button', { name: 'Registrar' })).toBeDisabled()
  await userEvent.type(within(dialog).getByLabelText('Resultado (obligatorio)'), '  Objetivo alcanzado  ')
  await userEvent.click(within(dialog).getByRole('button', { name: 'Registrar' }))
  await waitFor(() => expect(writes()).toHaveLength(1))
  expect(new URL(writes()[0].url).pathname).toBe(path)
  expect(writes()[0].method).toBe('POST')
  expect(await writes()[0].json()).toEqual({ status: 'COMPLETED', occurredOn: todayLocal(), notes: 'Se realizó la visita', result: 'Objetivo alcanzado' })
  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Registrar seguimiento' })).not.toBeInTheDocument())
})

it('conserva las notas ante un rechazo y permite corregir el envío', async () => {
  writeFails = true
  renderHistory()
  await userEvent.click(screen.getByRole('button', { name: 'Registrar seguimiento' }))
  await userEvent.type(screen.getByLabelText('Notas profesionales'), 'Seguimiento en curso')
  await userEvent.click(screen.getByRole('button', { name: 'Registrar' }))
  expect(await screen.findByText('Conflicto de seguimiento')).toBeInTheDocument()
  expect(screen.getByLabelText('Notas profesionales')).toHaveValue('Seguimiento en curso')
  writeFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Registrar' }))
  await waitFor(() => expect(writes()).toHaveLength(2))
  expect(await writes()[1].json()).toEqual({ status: 'IN_PROGRESS', occurredOn: todayLocal(), notes: 'Seguimiento en curso' })
})

it('no copia notas privadas al resumen ciudadano y solo publica el texto escrito expresamente', async () => {
  renderHistory()
  await screen.findByText(entry.notes)
  await userEvent.click(screen.getByTitle('Publicar resumen'))
  const summary = screen.getByLabelText('Resumen visible')
  expect(summary).toHaveValue('')
  expect(screen.getByRole('button', { name: 'Publicar' })).toBeDisabled()
  await userEvent.type(summary, '  Se acordó continuar el acompañamiento  ')
  await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
  await waitFor(() => expect(writes()).toHaveLength(1))
  expect(new URL(writes()[0].url).pathname).toBe(`${path}/follow-1/citizen-publication`)
  expect(await writes()[0].json()).toEqual({ summary: 'Se acordó continuar el acompañamiento' })
})

it('edita una publicación existente y conserva el texto si el backend rechaza el cambio', async () => {
  entries = [{ ...entry, citizenPublication: { visible: true, summary: 'Texto original' } }]
  writeFails = true
  renderHistory()
  await screen.findByText('Visible')
  await userEvent.click(screen.getByTitle('Editar publicación'))
  expect(screen.getByLabelText('Resumen visible')).toHaveValue('Texto original')
  await userEvent.type(screen.getByLabelText('Resumen visible'), ' actualizado')
  await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
  expect(await screen.findByText('No se pudo publicar')).toBeInTheDocument()
  expect(screen.getByLabelText('Resumen visible')).toHaveValue('Texto original actualizado')
})

it('requiere confirmar el retiro de un resumen y conserva el seguimiento profesional', async () => {
  entries = [{ ...entry, citizenPublication: { visible: true, summary: 'Resumen ciudadano' } }]
  renderHistory()
  await screen.findByText('Visible')
  await userEvent.click(screen.getByTitle('Retirar del portal'))
  const dialog = screen.getByRole('alertdialog')
  expect(dialog).toHaveTextContent('El seguimiento y el resumen profesional se conservarán.')
  expect(writes()).toHaveLength(0)
  await userEvent.click(within(dialog).getByRole('button', { name: 'Retirar' }))
  await waitFor(() => expect(writes()).toHaveLength(1))
  expect(writes()[0].method).toBe('DELETE')
  expect(new URL(writes()[0].url).pathname).toBe(`${path}/follow-1/citizen-publication`)
})
