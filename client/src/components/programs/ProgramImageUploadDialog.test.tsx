import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { client } from "@/generated/client.gen"
import { ProgramImageUploadDialog } from "./ProgramImageUploadDialog"

const originalConfig = client.getConfig()
const onOpenChange = vi.fn()
let queryClient: QueryClient
let requests: Request[]

function renderDialog(hasImage = false) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ProgramImageUploadDialog programId="program-1" hasImage={hasImage} onOpenChange={onOpenChange} />
    </QueryClientProvider>,
  )
}

describe("subir una portada desde administración", () => {
  beforeEach(() => {
    requests = []
    onOpenChange.mockReset()
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    client.setConfig({
      baseUrl: "http://localhost",
      fetch: vi.fn<typeof fetch>(async (input) => {
        requests.push(input as Request)
        return Response.json({ id: "image-1", programId: "program-1", url: "/api/images/image-1" })
      }),
    })
  })

  afterEach(() => {
    queryClient.clear()
    client.setConfig(originalConfig)
  })

  it("crea una portada nueva con POST", async () => {
    const user = userEvent.setup()
    renderDialog()

    expect(screen.getByRole("button", { name: "Subir portada" })).toBeDisabled()
    await user.upload(screen.getByLabelText("Imagen de portada"), new File(["image"], "portada.png", { type: "image/png" }))
    await user.click(screen.getByRole("button", { name: "Subir portada" }))

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(requests).toHaveLength(1)
    expect(requests[0].method).toBe("POST")
    expect(new URL(requests[0].url).pathname).toBe("/api/admin/programs/program-1/image")
  })

  it("reemplaza una portada existente con PUT", async () => {
    const user = userEvent.setup()
    renderDialog(true)

    await user.upload(screen.getByLabelText("Imagen de portada"), new File(["image"], "nueva.jpg", { type: "image/jpeg" }))
    await user.click(screen.getByRole("button", { name: "Reemplazar portada" }))

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(requests).toHaveLength(1)
    expect(requests[0].method).toBe("PUT")
  })

  it("rechaza un archivo no permitido sin llamar al servidor", async () => {
    const user = userEvent.setup({ applyAccept: false })
    renderDialog()

    await user.upload(screen.getByLabelText("Imagen de portada"), new File(["pdf"], "portada.pdf", { type: "application/pdf" }))

    expect(screen.getByRole("alert")).toHaveTextContent("JPG o PNG")
    expect(screen.getByRole("button", { name: "Subir portada" })).toBeDisabled()
    expect(requests).toHaveLength(0)
  })
})
