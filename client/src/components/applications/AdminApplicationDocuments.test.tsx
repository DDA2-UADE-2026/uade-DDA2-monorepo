import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { AdminDocumentPreviewDialog, AdminDocumentReviewDialog, AdminDocumentsTable, AdminDocumentStatusBadge } from './AdminApplicationDocuments'
import type { ApplicationDocumentResponse } from '@/generated/types.gen'

const document: ApplicationDocumentResponse & { id: string } = {
  id: 'doc-1', requirementId: 'req-1', requirementName: 'DNI', requirementCode: 'DNI', required: true,
  originalName: 'dni.pdf', status: 'PENDING', sizeBytes: 1024,
}
let api: ReturnType<typeof apiHarness>
const originalCreateUrl = Object.getOwnPropertyDescriptor(URL, 'createObjectURL')
const originalRevokeUrl = Object.getOwnPropertyDescriptor(URL, 'revokeObjectURL')
beforeEach(() => { api = apiHarness(() => Response.json({})) })
afterEach(() => {
  api.restore(); vi.restoreAllMocks()
  if (originalCreateUrl) Object.defineProperty(URL, 'createObjectURL', originalCreateUrl)
  else Reflect.deleteProperty(URL, 'createObjectURL')
  if (originalRevokeUrl) Object.defineProperty(URL, 'revokeObjectURL', originalRevokeUrl)
  else Reflect.deleteProperty(URL, 'revokeObjectURL')
})

describe('revisión administrativa documental', () => {
  it.each([['PENDING', 'Pendiente'], ['VALID', 'Válido'], ['OBSERVED', 'Observado'], [undefined, 'Sin estado']] as const)('identifica el estado %s', (status, label) => {
    api.render(<AdminDocumentStatusBadge status={status} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })

  it('ofrece revisión solo para entregas pendientes y reemplazo si está permitido', async () => {
    const replace = vi.fn()
    api.render(<AdminDocumentsTable applicationId="app-1" documents={[document, { ...document, id: 'doc-2', status: 'VALID', originalName: 'otro.pdf', observation: 'Revisado', reviewedAt: '2026-11-01T12:00:00Z' }]} onReplace={replace} />)
    expect(screen.getAllByRole('button', { name: 'Revisar' })).toHaveLength(1)
    await userEvent.setup().click(screen.getAllByRole('button', { name: 'Reemplazar DNI' })[0])
    expect(replace).toHaveBeenCalledWith(document)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Revisar' }))
    expect(screen.getByRole('dialog')).toHaveTextContent('Revisar documento')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Cancelar' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('muestra campos faltantes y auditoría sin ofrecer reemplazo de solo lectura', () => {
    api.render(<AdminDocumentsTable applicationId="app-1" documents={[{ documentId: 'legacy', reviewedAt: '2026-11-01T12:00:00Z', reviewedByUserId: 9 }]} />)
    expect(screen.getByText('Archivo sin nombre')).toBeInTheDocument()
    expect(screen.getByText('Usuario #9')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Reemplazar|Revisar/ })).not.toBeInTheDocument()
  })

  it('guarda una validación y cierra después de recibir confirmación', async () => {
    const close = vi.fn()
    api.render(<AdminDocumentReviewDialog applicationId="app-1" document={document} onOpenChange={close} />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Confirmar validación' }))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
    expect(api.requests[0].method).toBe('PATCH')
    expect(new URL(api.requests[0].url).pathname).toContain('/app-1/documents/doc-1/review')
    expect(await api.requests[0].json()).toEqual({ status: 'VALID' })
  })

  it('exige motivo al observar y envía el texto sin espacios extremos', async () => {
    const user = userEvent.setup()
    const close = vi.fn()
    api.render(<AdminDocumentReviewDialog applicationId="app-1" document={document} onOpenChange={close} />)
    await user.click(screen.getByRole('button', { name: 'Observar' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar observación' }))
    expect(screen.getByText('La observación es obligatoria.')).toBeInTheDocument()
    expect(api.requests).toHaveLength(0)
    await user.type(screen.getByLabelText('Observación'), '  Adjuntar ambas caras  ')
    await user.click(screen.getByRole('button', { name: 'Confirmar observación' }))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
    expect(await api.requests[0].json()).toEqual({ status: 'OBSERVED', observation: 'Adjuntar ambas caras' })
  })

  it('conserva el motivo tras un error del servidor y permite reintentar', async () => {
    api.restore()
    let fail = true
    api = apiHarness(() => fail ? Response.json({ message: 'La entrega cambió', status: 409 }, { status: 409 }) : Response.json({}))
    const close = vi.fn()
    const user = userEvent.setup()
    api.render(<AdminDocumentReviewDialog applicationId="app-1" document={document} onOpenChange={close} />)
    await user.click(screen.getByRole('button', { name: 'Observar' }))
    await user.type(screen.getByLabelText('Observación'), 'Falta reverso')
    await user.click(screen.getByRole('button', { name: 'Confirmar observación' }))
    expect(await screen.findByText('La entrega cambió')).toBeInTheDocument()
    expect(screen.getByLabelText('Observación')).toHaveValue('Falta reverso')
    expect(close).not.toHaveBeenCalled()
    fail = false
    await user.click(screen.getByRole('button', { name: 'Confirmar observación' }))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
    expect(api.requests).toHaveLength(2)
  })

  it('mantiene deshabilitadas las acciones mientras guarda', async () => {
    api.restore()
    let resolve!: (response: Response) => void
    api = apiHarness(() => new Promise<Response>(done => { resolve = done }))
    const close = vi.fn()
    api.render(<AdminDocumentReviewDialog applicationId="app-1" document={document} onOpenChange={close} />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Confirmar validación' }))
    expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Observar' })).toBeDisabled()
    resolve(Response.json({}))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
  })

  it('informa un error de vista previa y permite recuperar el archivo', async () => {
    api.restore()
    let fail = true
    api = apiHarness(() => fail ? Response.json({ message: 'Archivo no disponible' }, { status: 503 }) : new Response(new Blob(['%PDF test'], { type: 'application/pdf' })))
    const createUrl = vi.fn(() => 'blob:preview')
    const revokeUrl = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createUrl })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeUrl })
    const view = api.render(<AdminDocumentPreviewDialog applicationId="app-1" document={document} onOpenChange={vi.fn()} />)
    expect(screen.getByText('Cargando vista previa…')).toBeInTheDocument()
    expect(await screen.findByText('Archivo no disponible')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Descargar' })).toBeDisabled()
    fail = false
    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(screen.getByTitle('dni.pdf')).toHaveAttribute('src', 'blob:preview'))
    expect(screen.getByRole('button', { name: 'Descargar' })).toBeEnabled()
    const anchor = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    await userEvent.setup().click(screen.getByRole('button', { name: 'Descargar' }))
    expect(anchor).toHaveBeenCalledOnce()
    view.unmount()
    expect(revokeUrl).toHaveBeenCalledWith('blob:preview')
  })
})
