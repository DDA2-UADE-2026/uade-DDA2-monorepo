import type { ComponentType } from "react"
import { fireEvent, screen } from "@testing-library/react"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"

import { listCorePublications, listCoreSubscriptions } from "@/generated/sdk.gen"
import { Route as EventDocsRoute } from "@/routes/_app/gestion/debug/documentacion-eventos"
import { Route as EventsRoute } from "@/routes/_app/gestion/eventos"
import { preloadRouteComponent, renderRouteComponent, routeOptions, routerState } from "@/test/route-harness"

vi.mock("@tanstack/react-router", async (importOriginal) =>
  (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
vi.mock("@/components/ui/sidebar", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/components/ui/sidebar")>(),
  SidebarTrigger: () => null,
}))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  listCorePublications: vi.fn(), listCoreSubscriptions: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

let EventDocs: ComponentType

beforeAll(async () => {
  EventDocs = await preloadRouteComponent(EventDocsRoute)
})

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(listCorePublications).mockResolvedValue({ ...responseMeta, data: [{ id: "p-1", eventType: "social.ticket.updated", active: true }] })
  vi.mocked(listCoreSubscriptions).mockResolvedValue({ ...responseMeta, data: [] })
})

function renderEventDocs(permissions: string[]) {
  routerState.context = { user: { name: "Admin", permissions } }
  return renderRouteComponent(EventDocs)
}

describe("documentación de eventos", () => {
  it("embebe la UI de AsyncAPI publicada por Springwolf", async () => {
    renderEventDocs([])
    const frame = await screen.findByTitle("Documentación de eventos async")
    expect(frame.getAttribute("src")).toMatch(/\/springwolf\/asyncapi-ui\.html$/)
  })

  it("muestra el contrato con Core solo con integrations:core:view", async () => {
    renderEventDocs(["integrations:core:view"])
    fireEvent.click(await screen.findByRole("button", { name: "Contrato con Core" }))
    expect(await screen.findByText("social.ticket.updated")).toBeInTheDocument()
  })

  it("explica la falta de permiso en lugar de consultar Core", async () => {
    renderEventDocs([])
    fireEvent.click(await screen.findByRole("button", { name: "Contrato con Core" }))
    expect(await screen.findByText("Sin acceso")).toBeInTheDocument()
    expect(listCorePublications).not.toHaveBeenCalled()
  })
})

describe("monitor de eventos", () => {
  it("acepta solo las ventanas de tiempo conocidas", () => {
    const validateSearch = routeOptions(EventsRoute).validateSearch!
    expect(validateSearch.parse({ ventana: "24" })).toEqual({ ventana: "24" })
    expect(validateSearch.parse({ ventana: "5" })).toEqual({ ventana: undefined })
  })
})
