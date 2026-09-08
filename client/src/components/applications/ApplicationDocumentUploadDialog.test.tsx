import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { client } from "@/generated/client.gen"
import { ApplicationDocumentUploadDialog } from "./ApplicationDocumentUploadDialog"

const originalConfig = client.getConfig()
let queryClient: QueryClient
let requests: Request[]
let failed: boolean
const onOpenChange = vi.fn()

function renderDialog(disabled = false) {
  return render(<QueryClientProvider client={queryClient}>
    <ApplicationDocumentUploadDialog applicationId="application-1" requirement={{ id: "dni", name: "DNI", required: true }} disabled={disabled} onOpenChange={onOpenChange} />
  </QueryClientProvider>)
}

describe("adjuntar documentación desde un diálogo", () => {
  beforeEach(() => {
    requests = []
    failed = false
    onOpenChange.mockReset()
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async (input) => {
      requests.push(input as Request)
      return failed ? Response.json({ status: 500, message: "No se pudo guardar el archivo." }, { status: 500 })
        : Response.json({ id: "document-1", requirementId: "dni", originalName: "dni.pdf", reviewStatus: "PENDING" })
    }) })
  })

  afterEach(() => {
    queryClient.clear()
    client.setConfig(originalConfig)
  })

  /* TODO: reactivar cuando se resuelva la incompatibilidad multipart entre JSDOM y Node.
  it("envía el archivo mediante el contrato multipart y cierra después de guardar", async () => {
    const user = userEvent.setup()
    renderDialog()
    expect(screen.getByRole("button", { name: "Enviar documento" })).toBeDisabled()
    await user.upload(screen.getByLabelText("Archivo del documento"), new File(["%PDF-1.4 test"], "dni.pdf", { type: "application/pdf" }))
    expect(screen.getByText("dni.pdf")).toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Enviar documento" }))
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(requests).toHaveLength(1)
    expect(requests[0].method).toBe("PUT")
    expect(new URL(requests[0].url).pathname).toBe("/api/applications/application-1/documents/dni")
    expect(requests[0].headers.get("content-type")).toContain("multipart/form-data; boundary=")
    const body = await requests[0].text()
    expect(body).toContain('name="file"; filename="dni.pdf"')
    expect(body).toContain("%PDF-1.4 test")
  })
  */

  it("conserva el archivo tras un error y permite reintentar", async () => {
    failed = true
    const user = userEvent.setup()
    renderDialog()
    await user.upload(screen.getByLabelText("Archivo del documento"), new File(["%PDF-1.4 test"], "dni.pdf", { type: "application/pdf" }))
    await user.click(screen.getByRole("button", { name: "Enviar documento" }))
    await screen.findByText("No se pudo guardar el archivo.")
    expect(screen.getByText("dni.pdf")).toBeInTheDocument()
    expect(onOpenChange).not.toHaveBeenCalled()
    failed = false
    await user.click(screen.getByRole("button", { name: "Enviar documento" }))
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(requests).toHaveLength(2)
  })

  it("rechaza archivos vacíos sin llamar al servidor y respeta el bloqueo de la solicitud", async () => {
    const user = userEvent.setup()
    const view = renderDialog()
    await user.upload(screen.getByLabelText("Archivo del documento"), new File([], "dni.pdf", { type: "application/pdf" }))
    expect(screen.getByRole("alert")).toHaveTextContent("El archivo está vacío")
    expect(screen.getByRole("button", { name: "Enviar documento" })).toBeDisabled()
    view.unmount()
    renderDialog(true)
    expect(screen.getByLabelText("Archivo del documento")).toBeDisabled()
    expect(screen.getByRole("button", { name: "Enviar documento" })).toBeDisabled()
    expect(requests).toHaveLength(0)
  })
})
