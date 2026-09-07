import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { client } from "@/generated/client.gen"
import type { CreateProgramDocumentRequirementRequest, ErrorResponse, ProgramDocumentRequirementResponse } from "@/generated/types.gen"
import { ProgramDocumentRequirements } from "./ProgramDocumentRequirements"

const editionId = "17d4f8e5-c9d3-4b93-8645-354ac2125dca"
const requirementId = "d9b2d465-1f44-46c3-af7c-39d01c6e0f0b"
const endpoint = `/api/admin/program-editions/${editionId}/document-requirements`
const originalConfig = client.getConfig()
let queryClient: QueryClient
let documents: ProgramDocumentRequirementResponse[]
let writes: { method: string; path: string; body?: CreateProgramDocumentRequirementRequest }[]
let writeError: ErrorResponse | undefined

function renderRequirements(disabled = false) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ProgramDocumentRequirements editionId={editionId} disabled={disabled} />
    </QueryClientProvider>,
  )
}

describe("documentos requeridos de una edición", () => {
  beforeEach(() => {
    documents = []
    writes = []
    writeError = undefined
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    client.setConfig({
      baseUrl: "http://localhost",
      fetch: vi.fn<typeof fetch>(async (input) => {
        const request = input as Request
        const path = new URL(request.url).pathname
        if (request.method === "GET" && path === endpoint) return Response.json(documents)
        if (!path.startsWith(endpoint)) throw new Error(`Unexpected request: ${path}`)

        const body = request.method === "DELETE" ? undefined : await request.json() as CreateProgramDocumentRequirementRequest
        writes.push({ method: request.method, path, body })
        if (writeError) return Response.json(writeError, { status: writeError.status })
        if (request.method === "DELETE") {
          documents = []
          return new Response(null, { status: 204 })
        }
        documents = [{ ...body, id: requirementId, programEditionId: editionId }]
        return Response.json(documents[0], { status: request.method === "POST" ? 201 : 200 })
      }),
    })
  })

  afterEach(() => {
    queryClient.clear()
    client.setConfig(originalConfig)
  })

  it("crea un documento opcional con el contrato de Hey API y actualiza la lista", async () => {
    const user = userEvent.setup()
    renderRequirements()
    await screen.findByText("Sin documentos requeridos")
    await user.click(screen.getByRole("button", { name: "Nuevo documento requerido" }))
    const dialog = within(screen.getByRole("dialog"))
    await user.type(dialog.getByLabelText("Código"), "dni_front")
    await user.type(dialog.getByLabelText("Nombre"), "  Frente del DNI  ")
    await user.type(dialog.getByLabelText("Descripción"), "  Imagen legible  ")
    await user.click(dialog.getByRole("checkbox", { name: "Presentación obligatoria" }))
    await user.click(dialog.getByRole("button", { name: "Crear documento requerido" }))

    await screen.findByText("Frente del DNI")
    expect(writes).toEqual([{
      method: "POST",
      path: endpoint,
      body: { code: "DNI_FRONT", name: "Frente del DNI", description: "Imagen legible", required: false },
    }])
    expect(screen.getByText("Opcional")).toBeInTheDocument()
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
  })

  it("impide enviar códigos inválidos y nombres en blanco", async () => {
    const user = userEvent.setup()
    renderRequirements()
    await user.click(screen.getByRole("button", { name: "Nuevo documento requerido" }))
    const dialog = within(screen.getByRole("dialog"))
    await user.type(dialog.getByLabelText("Código"), "1-DNI")
    await user.type(dialog.getByLabelText("Nombre"), "   ")
    await user.tab()

    expect(dialog.getByRole("button", { name: "Crear documento requerido" })).toBeDisabled()
    expect(dialog.getByLabelText("Código")).toHaveAttribute("aria-invalid", "true")
    expect(dialog.getByText("Ingresá el nombre del documento.")).toBeInTheDocument()
    expect(writes).toHaveLength(0)
  })

  it("edita un documento opcional sin perder su obligatoriedad y permite eliminarlo", async () => {
    documents = [{ id: requirementId, code: "DNI_FRONT", name: "Frente del DNI", required: false }]
    const user = userEvent.setup()
    renderRequirements()
    await user.click(await screen.findByRole("button", { name: "Editar documento Frente del DNI" }))
    const dialog = within(screen.getByRole("dialog"))
    expect(dialog.getByRole("checkbox", { name: "Presentación obligatoria" })).not.toBeChecked()
    await user.clear(dialog.getByLabelText("Nombre"))
    await user.type(dialog.getByLabelText("Nombre"), "DNI actualizado")
    await user.click(dialog.getByRole("button", { name: "Guardar cambios" }))

    await screen.findByText("DNI actualizado")
    expect(writes[0]).toEqual({
      method: "PUT",
      path: `${endpoint}/${requirementId}`,
      body: { code: "DNI_FRONT", name: "DNI actualizado", required: false },
    })
    await user.click(screen.getByRole("button", { name: "Eliminar" }))
    await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "Eliminar" }))
    await screen.findByText("Sin documentos requeridos")
    expect(writes[1]).toEqual({ method: "DELETE", path: `${endpoint}/${requirementId}`, body: undefined })
  })

  it("muestra el bloqueo del backend y deshabilita cambios cuando ya hay solicitudes", async () => {
    writeError = { code: "PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED", status: 409, message: "La edición ya tiene solicitudes." }
    const user = userEvent.setup()
    renderRequirements()
    await user.click(screen.getByRole("button", { name: "Nuevo documento requerido" }))
    const dialog = within(screen.getByRole("dialog"))
    await user.type(dialog.getByLabelText("Código"), "DNI_FRONT")
    await user.type(dialog.getByLabelText("Nombre"), "Frente del DNI")
    await user.click(dialog.getByRole("button", { name: "Crear documento requerido" }))

    expect(await dialog.findByText("La edición ya tiene solicitudes.")).toBeInTheDocument()
    expect(dialog.getByRole("button", { name: "Crear documento requerido" })).toBeDisabled()
    await user.click(dialog.getByRole("button", { name: "Cancelar" }))
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(screen.getByText("Documentación bloqueada")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Nuevo documento requerido" })).toBeDisabled()
    expect(writes).toHaveLength(1)
  })

  it("mantiene la consulta y deshabilita las acciones en una edición cerrada", async () => {
    documents = [{ id: requirementId, code: "DNI_FRONT", name: "Frente del DNI", required: true }]
    renderRequirements(true)
    await screen.findByText("Frente del DNI")
    expect(screen.getByText("Obligatoria")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Nuevo documento requerido" })).toBeDisabled()
    expect(screen.getByRole("button", { name: "Editar documento Frente del DNI" })).toBeDisabled()
    expect(screen.getByRole("button", { name: "Eliminar" })).toBeDisabled()
  })
})
