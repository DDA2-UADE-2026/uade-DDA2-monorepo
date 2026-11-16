import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { ActivityStatusDialog } from "@/components/activities/ActivityStatusDialog"
import { close1, publish1 } from "@/generated/sdk.gen"
import type { ActivityResponse } from "@/generated/types.gen"

vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  publish1: vi.fn(), close1: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

function renderDialog(action: "publish" | "close", activity: Pick<ActivityResponse, "id" | "name"> = { id: "act-1", name: "Taller de lectura" }) {
  const onOpenChange = vi.fn()
  const onDone = vi.fn()
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const invalidate = vi.spyOn(client, "invalidateQueries")
  render(
    <QueryClientProvider client={client}>
      <ActivityStatusDialog activity={activity} action={action} onOpenChange={onOpenChange} onDone={onDone} />
    </QueryClientProvider>,
  )
  return { onOpenChange, onDone, invalidate }
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe("ActivityStatusDialog", () => {
  it("publica la actividad, refresca los listados y el detalle, y cierra", async () => {
    vi.mocked(publish1).mockResolvedValue({ ...responseMeta, data: { id: "act-1", name: "Taller de lectura", status: "OPEN" } })
    const { onOpenChange, onDone, invalidate } = renderDialog("publish")

    expect(screen.getByText(/«Taller de lectura» — Queda visible para la ciudadanía/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Publicar" }))

    await waitFor(() => expect(onDone).toHaveBeenCalledWith(expect.objectContaining({ id: "act-1", status: "OPEN" })))
    expect(publish1).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "act-1" } }))
    expect(onOpenChange).toHaveBeenCalledWith(false)
    // 3 listados + detalle, vista profesional y detalle de gestión
    expect(invalidate).toHaveBeenCalledTimes(6)
  })

  it("cierra la actividad aunque la respuesta no traiga id", async () => {
    vi.mocked(close1).mockResolvedValue({ ...responseMeta, data: { name: "Taller de lectura", status: "CLOSED" } })
    const { onDone, invalidate } = renderDialog("close")

    fireEvent.click(screen.getByRole("button", { name: "Cerrar" }))

    await waitFor(() => expect(onDone).toHaveBeenCalled())
    expect(close1).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "act-1" } }))
    expect(invalidate).toHaveBeenCalledTimes(3)
  })

  it("muestra el error y mantiene el diálogo abierto", async () => {
    vi.mocked(publish1).mockRejectedValue({ message: "La actividad no tiene cupos" })
    const { onOpenChange } = renderDialog("publish")

    fireEvent.click(screen.getByRole("button", { name: "Publicar" }))

    expect(await screen.findByText("La actividad no tiene cupos")).toBeInTheDocument()
    expect(onOpenChange).not.toHaveBeenCalled()
  })

  it("no permite confirmar una actividad sin id", () => {
    renderDialog("close", { name: undefined })
    expect(screen.getByText(/«Sin nombre»/)).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Cerrar" })).toBeDisabled()
  })
})
