import type { ReactNode } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { toast } from "sonner"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { CitizenActivitiesSection } from "@/components/activities/CitizenActivitiesSection"
import { enroll, list13 } from "@/generated/sdk.gen"

vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children, to }: { children?: ReactNode; to: string }) => <a href={to}>{children}</a>,
}))
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  list13: vi.fn(), enroll: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

function renderSection(canEnroll = true) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><CitizenActivitiesSection canEnroll={canEnroll} /></QueryClientProvider>)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(list13).mockResolvedValue({ ...responseMeta, data: { content: [
    { id: "act-1", name: "Taller de huerta", location: "Plaza", capacity: 10, availableCapacity: 4, status: "OPEN" },
    { id: "act-2", name: "Ajedrez", capacity: 8, availableCapacity: 0, status: "OPEN" },
    { id: "act-3", name: "Yoga", capacity: 8, availableCapacity: 2, status: "OPEN", myEnrollment: { id: "ins-1" } },
  ] } })
  vi.mocked(enroll).mockResolvedValue({ ...responseMeta, data: { id: "ins-2" } })
})

describe("CitizenActivitiesSection", () => {
  it("muestra la vista previa y permite inscribirse donde hay cupo", async () => {
    renderSection()

    expect(await screen.findByText("Taller de huerta")).toBeInTheDocument()
    expect(list13).toHaveBeenCalledWith(expect.objectContaining({ query: { page: 0, size: 3 } }))
    expect(screen.getByRole("button", { name: "Sin cupo" })).toBeDisabled()
    expect(screen.getByText("Ver detalle")).toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "Inscribirme" }))
    fireEvent.click(await screen.findByRole("button", { name: "Confirmar inscripción" }))

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Te inscribiste en la actividad"))
    expect(enroll).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "act-1" } }))
    await waitFor(() => expect(screen.getAllByText("Ver detalle")).toHaveLength(2))
  })

  it("sin permiso de inscripción solo lista las actividades", async () => {
    renderSection(false)
    expect(await screen.findByText("Taller de huerta")).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Inscribirme" })).not.toBeInTheDocument()
  })

  it("informa cuando no hay actividades abiertas", async () => {
    vi.mocked(list13).mockResolvedValue({ ...responseMeta, data: { content: [] } })
    renderSection()
    expect(await screen.findByText("No hay actividades abiertas")).toBeInTheDocument()
  })
})
