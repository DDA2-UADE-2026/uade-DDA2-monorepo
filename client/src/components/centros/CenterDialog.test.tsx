import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { CenterDialog } from "@/components/centros/CenterDialog"
import { createMunicipalCenter, updateMunicipalCenter } from "@/generated/sdk.gen"
import type { MunicipalCenterResponse } from "@/generated/types.gen"

vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  createMunicipalCenter: vi.fn(), updateMunicipalCenter: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

function renderDialog(center: MunicipalCenterResponse | null) {
  const onOpenChange = vi.fn()
  const onSaved = vi.fn()
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  const invalidate = vi.spyOn(client, "invalidateQueries")
  render(<QueryClientProvider client={client}><CenterDialog center={center} onOpenChange={onOpenChange} onSaved={onSaved} /></QueryClientProvider>)
  return { onOpenChange, onSaved, invalidate }
}

const submit = () => fireEvent.submit(document.getElementById("center-form")!)

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(createMunicipalCenter).mockResolvedValue({ ...responseMeta, data: { id: "centro-2" } })
  vi.mocked(updateMunicipalCenter).mockResolvedValue({ ...responseMeta, data: { id: "centro-1" } })
})

describe("CenterDialog", () => {
  it("crea un centro omitiendo los contactos vacíos", async () => {
    const { onOpenChange, onSaved, invalidate } = renderDialog(null)

    fireEvent.change(screen.getByLabelText("Nombre"), { target: { value: " Centro Sur " } })
    fireEvent.change(screen.getByLabelText("Dirección"), { target: { value: "Av. Belgrano 50" } })
    submit()

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(createMunicipalCenter).toHaveBeenCalledWith(expect.objectContaining({
      body: { name: "Centro Sur", address: "Av. Belgrano 50", phone: undefined, email: undefined },
    }))
    expect(invalidate).toHaveBeenCalled()
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it("actualiza un centro existente", async () => {
    const { onSaved } = renderDialog({ id: "centro-1", name: "Centro Norte", address: "Calle 123", phone: "4444-5555", email: "" })
    expect(screen.getByText("Editar centro")).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText("Correo (opcional)"), { target: { value: "norte@muni.gob.ar" } })
    submit()

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(updateMunicipalCenter).toHaveBeenCalledWith(expect.objectContaining({
      path: { id: "centro-1" },
      body: { name: "Centro Norte", address: "Calle 123", phone: "4444-5555", email: "norte@muni.gob.ar" },
    }))
  })

  it("muestra los errores de campo que devuelve la API", async () => {
    vi.mocked(createMunicipalCenter).mockRejectedValue({ message: "Datos inválidos", fields: [{ field: "name", message: "Ya existe un centro con ese nombre" }] })
    const { onSaved } = renderDialog(null)

    fireEvent.change(screen.getByLabelText("Nombre"), { target: { value: "Centro Norte" } })
    fireEvent.change(screen.getByLabelText("Dirección"), { target: { value: "Calle 123" } })
    submit()

    expect(await screen.findByText("Ya existe un centro con ese nombre")).toBeInTheDocument()
    expect(screen.getByText("Datos inválidos")).toBeInTheDocument()
    expect(onSaved).not.toHaveBeenCalled()
  })
})
