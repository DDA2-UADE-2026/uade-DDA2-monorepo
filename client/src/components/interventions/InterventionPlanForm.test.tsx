import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { InterventionPlanForm } from "@/components/interventions/InterventionPlanForm"
import { create2 } from "@/generated/sdk.gen"
import { get5QueryKey } from "@/generated/@tanstack/react-query.gen"

vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  create2: vi.fn(),
}))
vi.mock("@/components/programs/ProgramDatePicker", () => ({
  ProgramDatePicker: ({ id, value, onChange }: { id: string; value: string; onChange: (value: string) => void }) => (
    <input id={id} value={value} onChange={(event) => onChange(event.target.value)} />
  ),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

function renderForm() {
  const onCreated = vi.fn()
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><InterventionPlanForm applicationId="sol-1" onCreated={onCreated} /></QueryClientProvider>)
  return { onCreated, client }
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe("InterventionPlanForm", () => {
  it("valida que cada acción tenga descripción y fechas en orden", () => {
    renderForm()
    fireEvent.click(screen.getByRole("button", { name: "Crear plan" }))
    expect(screen.getByText("Completá las descripciones y fechas. La fecha de fin no puede ser anterior al inicio.")).toBeInTheDocument()
    expect(create2).not.toHaveBeenCalled()
  })

  it("crea el plan y lo deja en la caché del detalle", async () => {
    const plan = { id: "plan-1", applicationId: "sol-1" }
    vi.mocked(create2).mockResolvedValue({ ...responseMeta, data: plan })
    const { onCreated, client } = renderForm()

    fireEvent.change(screen.getByLabelText("Descripción del objetivo"), { target: { value: " Autonomía " } })
    fireEvent.change(screen.getByLabelText("Descripción"), { target: { value: "Acompañar trámites" } })
    fireEvent.change(screen.getByLabelText("Inicio"), { target: { value: "2026-11-20" } })
    fireEvent.change(screen.getByLabelText("Fin previsto"), { target: { value: "2026-12-20" } })
    fireEvent.click(screen.getByRole("button", { name: "Crear plan" }))

    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(plan))
    expect(create2).toHaveBeenCalledWith(expect.objectContaining({
      path: { id: "sol-1" },
      body: { objectives: [{ description: "Autonomía", actions: [{ description: "Acompañar trámites", startDate: "2026-11-20", dueDate: "2026-12-20" }] }] },
    }))
    expect(client.getQueryData(get5QueryKey({ path: { id: "sol-1" } }))).toEqual(plan)
  })
})
