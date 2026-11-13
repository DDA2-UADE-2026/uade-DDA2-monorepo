import { useState } from 'react'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import type { ApplicationDocumentResponse } from '@/generated/types.gen'
import { ApplicationDocumentPreviewDialog } from './ApplicationDocumentPreviewDialog'

let api: ReturnType<typeof apiHarness>
let readFails: boolean
const blob = new Blob(['fake document bytes'], { type: 'application/pdf' })
const documentInfo: ApplicationDocumentResponse & { id: string } = {
  id: 'document-1',
  originalName: 'constancia.pdf',
  requirementName: 'Constancia de domicilio',
  contentType: 'application/pdf',
}

function PreviewHarness({ document = documentInfo }: { document?: ApplicationDocumentResponse & { id: string } }) {
  const [open, setOpen] = useState(true)
  return open ? <ApplicationDocumentPreviewDialog applicationId="application-1" document={document} onOpenChange={setOpen} /> : null
}

beforeEach(() => {
  readFails = false
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview-document')
  vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
  api = apiHarness((request) => {
    const path = new URL(request.url).pathname
    if (path !== '/api/applications/application-1/documents/document-1/content') throw new Error(`Unexpected request ${request.method} ${path}`)
    return readFails
      ? Response.json({ status: 503, message: 'Archivo temporalmente inaccesible' }, { status: 503 })
      : new Response(blob, { headers: { 'Content-Type': documentInfo.contentType! } })
  })
})

afterEach(() => {
  api.restore()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('ApplicationDocumentPreviewDialog', () => {
  it('usa la vista de imagen y revoca el URL al cerrar el diálogo', async () => {
    const imageDocument = { ...documentInfo, originalName: undefined, requirementName: undefined, contentType: 'image/png' }
    api.render(<PreviewHarness document={imageDocument} />)

    expect(await screen.findByAltText('Documento adjunto')).toHaveAttribute('src', 'blob:preview-document')
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    await waitFor(() => expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-document'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('permite reintentar una lectura fallida y mantiene la descarga bloqueada hasta obtener el archivo', async () => {
    readFails = true
    api.render(<PreviewHarness document={{ ...documentInfo, originalName: undefined, requirementName: undefined }} />)

    expect(await screen.findByText('Archivo temporalmente inaccesible')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Descargar' })).toBeDisabled()
    readFails = false
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByTitle('Vista previa del documento')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Descargar' })).toBeEnabled()
    expect(api.requests).toHaveLength(2)
  })
})
