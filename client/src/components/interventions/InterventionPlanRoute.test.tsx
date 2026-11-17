import type { ComponentType } from "react"
import { screen } from "@testing-library/react"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"

import { get4, get5 } from "@/generated/sdk.gen"
import { Route } from "@/routes/_app/gestion/intervenciones/$planId"
import { preloadRouteComponent, renderRouteComponent, routerState } from "@/test/route-harness"

vi.mock("@tanstack/react-router", async (importOriginal) =>
  (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
vi.mock("@/components/layout/OutletNav", async () => (await import("@/test/route-harness")).outletNavMock)
vi.mock("@/components/layout/OutletNavBreadcrumbs", () => ({ OutletNavBreadcrumbs: () => null }))
vi.mock("@/components/UserAvatar", () => ({ UserAvatar: () => <span aria-hidden="true" /> }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  get4: vi.fn(), get5: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

let RouteComponent: ComponentType

beforeAll(async () => {
  RouteComponent = await preloadRouteComponent(Route)
})

function renderPlan(permissions: string[]) {
  routerState.params = { planId: "sol-1" }
  routerState.context = { user: { id: 9, name: "Trabajadora", permissions } }
  return renderRouteComponent(RouteComponent)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(get4).mockResolvedValue({ ...responseMeta, data: { id: "sol-1", applicationNumber: 57, programName: "Apoyo escolar", programEditionName: "2026", userName: "Marta Gómez", status: "APPROVED" } })
})

describe("detalle de intervención", () => {
  it("muestra el caso y su plan con las acciones", async () => {
    vi.mocked(get5).mockResolvedValue({ ...responseMeta, data: {
      id: "plan-1",
      responsibleUserName: "Lucía Díaz",
      objectives: [{ id: "obj-1", description: "Sostener la escolaridad", actions: [{ id: "acc-1", description: "Gestionar útiles", status: "IN_PROGRESS", startDate: "2026-11-01", dueDate: "2026-12-01" }] }],
    } })
    renderPlan([])

    expect(await screen.findByText("Intervención · Solicitud N.º 57")).toBeInTheDocument()
    expect(screen.getByText("Apoyo escolar · 2026")).toBeInTheDocument()
    expect(await screen.findByText("Sostener la escolaridad")).toBeInTheDocument()
    expect(screen.getByText("Gestionar útiles")).toBeInTheDocument()
    expect(get4).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "sol-1" } }))
    expect(get5).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "sol-1" } }))
  })

  it("sin plan ofrece crearlo solo con el permiso correspondiente", async () => {
    vi.mocked(get5).mockRejectedValue({ status: 404, message: "Sin plan" })
    const { unmount } = renderPlan(["applications:interventions:create"])
    expect(await screen.findByText("Crear plan de intervención")).toBeInTheDocument()
    unmount()

    renderPlan([])
    expect(await screen.findByText("El caso todavía no tiene un plan")).toBeInTheDocument()
  })
})
