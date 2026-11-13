import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { apiHarness } from '@/test/api-harness'
import type { ApplicationDocumentResponse, ApplicationResponse } from '@/generated/types.gen'
import { ApplicationDocuments } from './ApplicationDocuments'

let api: ReturnType<typeof apiHarness>
let documents: ApplicationDocumentResponse[]
let readFails: boolean
let finalized: boolean
const base = '/api/applications/app-1/documents'
const application: ApplicationResponse & { id: string } = { id: 'app-1', status: 'SUBMITTED', documentRequirements: [{ id: 'dni', name: 'DNI', required: true, description: 'Ambas caras' }] }
beforeEach(() => {
  documents = []; readFails = false; finalized = false
  vi.spyOn(toast, 'error').mockImplementation(() => 'error')
  api = apiHarness(request => {
    const path = new URL(request.url).pathname
    if (request.method === 'DELETE') {
      if (finalized) return Response.json({ status: 409, code: 'APPLICATION_DOCUMENTS_FINALIZED', message: 'La solicitud fue resuelta' }, { status: 409 })
      documents = []
      return new Response(null, { status: 204 })
    }
    if (path === base) return readFails ? Response.json({ message: 'Documentación inaccesible' }, { status: 503 }) : Response.json(documents)
    throw new Error(`Unexpected request ${request.method} ${path}`)
  })
})
afterEach(() => { api.restore(); vi.restoreAllMocks() })

it('distingue documentación obligatoria y opcional faltante y permite adjuntarla', async () => {
  api.render(<ApplicationDocuments application={{ ...application, documentRequirements: [...application.documentRequirements!, { id: 'extra', name: 'Constancia', required: false }] }} />)
  expect(await screen.findByText('Falta adjuntar')).toBeInTheDocument()
  expect(screen.getByText('Sin adjuntar')).toBeInTheDocument()
  expect(screen.getByText('Obligatorio')).toBeInTheDocument()
  expect(screen.getByText('Opcional')).toBeInTheDocument()
  expect(screen.getByText('Ambas caras')).toBeInTheDocument()
  await userEvent.click(screen.getAllByRole('button', { name: 'Adjuntar documento' })[0])
  expect(await screen.findByRole('dialog')).toHaveTextContent('DNI')
  expect(screen.getByLabelText('Archivo del documento')).toBeEnabled()
})

it.each(['APPROVED', 'REJECTED'] as const)('deja la documentación en consulta cuando la solicitud está %s', async status => {
  documents = [{ id: 'doc-1', requirementId: 'dni', originalName: 'dni.pdf', status: 'VALID' }]
  api.render(<ApplicationDocuments application={{ ...application, status }} />)
  expect(await screen.findByText('dni.pdf')).toBeInTheDocument()
  expect(screen.getByText('Documentación disponible para consulta')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Ver archivo' })).toBeEnabled()
  expect(screen.queryByRole('button', { name: /Reemplazar|Adjuntar documento|Eliminar/ })).not.toBeInTheDocument()
})

it('muestra la observación del profesional y permite reemplazar la entrega', async () => {
  documents = [{ id: 'doc-1', requirementId: 'dni', originalName: 'dni.pdf', status: 'OBSERVED', observation: 'Falta el reverso', sizeBytes: 1024, contentType: 'application/pdf' }]
  api.render(<ApplicationDocuments application={application} />)
  expect(await screen.findByText('Falta el reverso')).toBeInTheDocument()
  expect(screen.getByText('Observado')).toBeInTheDocument()
  expect(screen.getByText(/PDF/)).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Reemplazar' }))
  expect(await screen.findByRole('dialog')).toHaveTextContent('dni.pdf')
})

it('confirma una eliminación y refresca el requisito que vuelve a faltar', async () => {
  documents = [{ id: 'doc-1', requirementId: 'dni', originalName: 'dni.pdf', status: 'PENDING' }]
  api.render(<ApplicationDocuments application={application} />)
  await userEvent.click(await screen.findByRole('button', { name: 'Eliminar' }))
  const dialog = screen.getByRole('alertdialog')
  expect(dialog).toHaveTextContent('DNI')
  expect(api.requests.filter(request => request.method === 'DELETE')).toHaveLength(0)
  await userEvent.click(within(dialog).getByRole('button', { name: 'Eliminar' }))
  expect(await screen.findByText('Falta adjuntar')).toBeInTheDocument()
  const request = api.requests.find(request => request.method === 'DELETE')!
  expect(new URL(request.url).pathname).toBe(`${base}/doc-1`)
  expect(screen.queryByText('dni.pdf')).not.toBeInTheDocument()
})

it('bloquea nuevas modificaciones si el backend informa que la solicitud se resolvió', async () => {
  documents = [{ id: 'doc-1', requirementId: 'dni', originalName: 'dni.pdf' }]
  finalized = true
  api.render(<ApplicationDocuments application={application} />)
  await userEvent.click(await screen.findByRole('button', { name: 'Eliminar' }))
  await userEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Eliminar' }))
  expect(await screen.findByText('Documentación disponible para consulta')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Reemplazar' })).not.toBeInTheDocument()
  expect(toast.error).toHaveBeenCalledWith('No se pudo completar la operación', { description: 'La solicitud fue resuelta' })
})

it('recupera una lectura fallida y explica cuando la edición no pide documentos', async () => {
  readFails = true
  api.render(<ApplicationDocuments application={{ id: 'app-1', status: 'SUBMITTED' }} />)
  expect(await screen.findByText('Documentación inaccesible')).toBeInTheDocument()
  readFails = false
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(await screen.findByText('Esta edición no solicita documentación.')).toBeInTheDocument()
  await waitFor(() => expect(api.requests).toHaveLength(2))
})
