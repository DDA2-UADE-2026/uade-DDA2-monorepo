import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { ProgramImageUploadDialog } from '@/components/programs/ProgramImageUploadDialog'
import { AdminApplicationDocumentUploadDialog } from './AdminApplicationDocumentUploadDialog'

let api: ReturnType<typeof apiHarness>
let fails: boolean
beforeEach(() => {
  fails = false
  api = apiHarness(() => fails ? Response.json({ status: 409, message: 'El recurso cambió' }, { status: 409 }) : Response.json({ id: 'saved' }))
})
afterEach(() => api.restore())
const variants = [
  { kind: 'documento administrativo', label: 'Archivo del documento', send: 'Cargar documento', replace: 'Reemplazar documento', method: 'PUT', path: '/api/admin/applications/app-1/documents/dni', file: () => new File(['%PDF-1.4 test'], 'dni.pdf', { type: 'application/pdf' }), render: (close: (open: boolean) => void, existing = false, disabled = false) => <AdminApplicationDocumentUploadDialog applicationId="app-1" requirement={{ id: 'dni', name: 'DNI', required: true }} existing={existing ? { id: 'old', originalName: 'viejo.pdf' } : undefined} disabled={disabled} onOpenChange={close} /> },
  { kind: 'portada de programa', label: 'Imagen de portada', send: 'Subir portada', replace: 'Reemplazar portada', method: 'POST', path: '/api/admin/programs/program-1/image', file: () => new File(['image test'], 'portada.png', { type: 'image/png' }), render: (close: (open: boolean) => void, existing = false) => <ProgramImageUploadDialog programId="program-1" hasImage={existing} onOpenChange={close} /> },
]

describe.each(variants)('$kind', variant => {
  it('mantiene deshabilitado el envío sin archivo y cierra solo después de guardar', async () => {
    const close = vi.fn()
    api.render(variant.render(close))
    expect(screen.getByRole('button', { name: variant.send })).toBeDisabled()
    await userEvent.upload(screen.getByLabelText(variant.label), variant.file())
    expect(close).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: variant.send }))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
    expect(api.requests).toHaveLength(1)
    expect(api.requests[0].method).toBe(variant.method)
    expect(new URL(api.requests[0].url).pathname).toBe(variant.path)
  })

  it('usa la operación de reemplazo cuando ya existe una entrega', async () => {
    const close = vi.fn()
    api.render(variant.render(close, true))
    await userEvent.upload(screen.getByLabelText(variant.label), variant.file())
    await userEvent.click(screen.getByRole('button', { name: variant.replace }))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
    expect(api.requests[0].method).toBe('PUT')
  })

  it.each(['empty', 'oversize', 'mismatch'] as const)('rechaza el archivo %s sin llamar al servidor', async condition => {
    api.render(variant.render(vi.fn()))
    const file = variant.file()
    if (condition === 'empty') Object.defineProperty(file, 'size', { value: 0 })
    if (condition === 'oversize') Object.defineProperty(file, 'size', { value: 10 * 1024 * 1024 + 1 })
    if (condition === 'mismatch') Object.defineProperty(file, 'type', { value: 'text/plain' })
    // Se permite seleccionar un formato incorrecto para probar la validación propia del componente.
    await userEvent.setup({ applyAccept: false }).upload(screen.getByLabelText(variant.label), file)
    expect(screen.getByRole('alert')).toHaveTextContent(condition === 'empty' ? /vac[ií]/ : condition === 'oversize' ? /10 MB/ : /formato correcto/)
    expect(screen.getByRole('button', { name: variant.send })).toBeDisabled()
    expect(api.requests).toHaveLength(0)
  })

  it('permite quitar una selección sin enviar ni cerrar el diálogo', async () => {
    const close = vi.fn()
    api.render(variant.render(close))
    await userEvent.upload(screen.getByLabelText(variant.label), variant.file())
    await userEvent.click(screen.getByRole('button', { name: /Quitar.*seleccionad/ }))
    expect(screen.getByRole('button', { name: variant.send })).toBeDisabled()
    expect(api.requests).toHaveLength(0)
    expect(close).not.toHaveBeenCalled()
  })

  it('rechaza arrastrar varios archivos a una entrega individual', () => {
    const view = api.render(variant.render(vi.fn()))
    const dropzone = view.container.ownerDocument.querySelector('[data-slot="attachment"]')!.parentElement!
    fireEvent.dragOver(dropzone)
    fireEvent.drop(dropzone, { dataTransfer: { files: [variant.file(), variant.file()] } })
    expect(screen.getByRole('alert')).toHaveTextContent(/un solo archivo|una sola imagen/)
    expect(api.requests).toHaveLength(0)
  })

  it('conserva el archivo tras un error y permite reintentar el guardado', async () => {
    fails = true
    const close = vi.fn()
    const file = variant.file()
    api.render(variant.render(close))
    await userEvent.upload(screen.getByLabelText(variant.label), file)
    await userEvent.click(screen.getByRole('button', { name: variant.send }))
    expect(await screen.findByText('El recurso cambió')).toBeInTheDocument()
    expect(screen.getByText(file.name)).toBeInTheDocument()
    expect(close).not.toHaveBeenCalled()
    fails = false
    await userEvent.click(screen.getByRole('button', { name: variant.send }))
    await waitFor(() => expect(close).toHaveBeenCalledWith(false))
    expect(api.requests).toHaveLength(2)
  })
})

it('no permite seleccionar ni cargar documentación de una solicitud bloqueada', () => {
  api.render(variants[0].render(vi.fn(), false, true))
  expect(screen.getByLabelText('Archivo del documento')).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Cargar documento' })).toBeDisabled()
  expect(screen.getByText('Esta solicitud ya no admite cambios en la documentación.')).toBeInTheDocument()
})
