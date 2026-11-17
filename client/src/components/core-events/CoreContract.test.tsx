import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { CoreContract } from "@/components/core-events/CoreContract"
import { listCorePublications, listCoreSubscriptions } from "@/generated/sdk.gen"

vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  listCorePublications: vi.fn(), listCoreSubscriptions: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

function renderContract() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
  return render(<QueryClientProvider client={client}><CoreContract /></QueryClientProvider>)
}

const card = (title: string) => screen.getByText(title).closest('[data-slot="card"]') as HTMLElement

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(listCorePublications).mockResolvedValue({ ...responseMeta, data: [
    { id: "p-2", eventType: "social.ticket.updated", active: false },
    { id: "p-1", eventType: "social.application.created", active: true },
    { eventType: "social.benefit.granted", active: true },
  ] })
  vi.mocked(listCoreSubscriptions).mockResolvedValue({ ...responseMeta, data: [
    { id: "s-1", eventType: "core.ticket.routed", queueName: "m8.tickets", maxAttempts: 5, active: true },
    { id: "s-2" },
  ] })
})

describe("CoreContract", () => {
  it("lista publicaciones y suscripciones ordenadas y cuenta las activas", async () => {
    renderContract()

    expect(await screen.findByText("social.application.created")).toBeInTheDocument()
    const publications = within(card("Publicaciones"))
    expect(publications.getByText(/2 activas de 3\./)).toBeInTheDocument()
    expect(publications.getAllByRole("listitem").map((item) => item.querySelector("p")?.textContent)).toEqual([
      "social.application.created",
      "social.benefit.granted",
      "social.ticket.updated",
    ])
    expect(publications.getByText("Inactiva")).toBeInTheDocument()

    const subscriptions = within(card("Suscripciones"))
    expect(await subscriptions.findByText("m8.tickets · hasta 5 intentos")).toBeInTheDocument()
    expect(subscriptions.getByText("— · hasta ? intentos")).toBeInTheDocument()
    expect(subscriptions.getByText(/1 activas de 2\./)).toBeInTheDocument()
  })

  it("muestra el mensaje vacío de cada sección", async () => {
    vi.mocked(listCorePublications).mockResolvedValue({ ...responseMeta, data: [] })
    vi.mocked(listCoreSubscriptions).mockResolvedValue({ ...responseMeta, data: [] })
    renderContract()

    expect(await screen.findByText("No hay publicaciones declaradas.")).toBeInTheDocument()
    expect(await screen.findByText("No hay suscripciones registradas.")).toBeInTheDocument()
  })

  it("permite reintentar una sección que falló sin afectar a la otra", async () => {
    vi.mocked(listCorePublications).mockRejectedValueOnce(new Error("Core no disponible"))
    vi.mocked(listCoreSubscriptions).mockRejectedValueOnce(new Error("Timeout"))
    renderContract()

    expect(await screen.findByText("Core no disponible")).toBeInTheDocument()
    expect(await screen.findByText("Timeout")).toBeInTheDocument()

    fireEvent.click(within(card("Publicaciones")).getByRole("button", { name: "Reintentar" }))
    expect(await screen.findByText("social.application.created")).toBeInTheDocument()

    fireEvent.click(within(card("Suscripciones")).getByRole("button", { name: "Reintentar" }))
    await waitFor(() => expect(screen.getByText("core.ticket.routed")).toBeInTheDocument())
    expect(listCorePublications).toHaveBeenCalledTimes(2)
    expect(listCoreSubscriptions).toHaveBeenCalledTimes(2)
  })
})
