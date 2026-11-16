import type { ComponentType } from "react"
import { fireEvent, screen, waitFor } from "@testing-library/react"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"

import { create3, list1 } from "@/generated/sdk.gen"
import { Route } from "@/routes/_app/gestion/programas/index"
import { preloadRouteComponent, renderRouteComponent, routerState } from "@/test/route-harness"

vi.mock("@tanstack/react-router", async (importOriginal) =>
  (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
vi.mock("@/components/layout/OutletNav", async () => (await import("@/test/route-harness")).outletNavMock)
vi.mock("@/components/layout/OutletNavBreadcrumbs", () => ({ OutletNavBreadcrumbs: () => null }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  list1: vi.fn(), create3: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

let RouteComponent: ComponentType

beforeAll(async () => {
  RouteComponent = await preloadRouteComponent(Route)
})

beforeEach(() => {
  vi.resetAllMocks()
  routerState.search = { page: 1 }
  vi.mocked(list1).mockResolvedValue({ ...responseMeta, data: { content: [{ id: "prog-1", name: "Apoyo escolar", active: true }], totalElements: 1, totalPages: 1 } })
})

describe("programas", () => {
  it("crea un programa y abre su detalle", async () => {
    vi.mocked(create3).mockResolvedValue({ ...responseMeta, data: { id: "prog-2", name: "Becas laborales" } })
    renderRouteComponent(RouteComponent)
    await screen.findByText("Apoyo escolar")

    fireEvent.click(screen.getByRole("button", { name: "Nuevo programa" }))
    fireEvent.change(await screen.findByLabelText("Nombre"), { target: { value: "Becas laborales" } })
    fireEvent.change(screen.getByLabelText("Objetivo"), { target: { value: "Inserción laboral" } })
    fireEvent.submit(document.getElementById("create-program-form")!)

    await waitFor(() => expect(routerState.navigate).toHaveBeenCalledWith({ to: "/gestion/programas/$programaId", params: { programaId: "prog-2" } }))
    expect(create3).toHaveBeenCalledWith(expect.objectContaining({ body: { name: "Becas laborales", objective: "Inserción laboral" } }))
    expect(list1).toHaveBeenCalledTimes(2)
  })

  it("muestra el error si la API rechaza el alta", async () => {
    vi.mocked(create3).mockRejectedValue({ message: "Ya existe un programa con ese nombre" })
    renderRouteComponent(RouteComponent)
    await screen.findByText("Apoyo escolar")

    fireEvent.click(screen.getByRole("button", { name: "Nuevo programa" }))
    fireEvent.change(await screen.findByLabelText("Nombre"), { target: { value: "Apoyo escolar" } })
    fireEvent.submit(document.getElementById("create-program-form")!)

    expect(await screen.findByText("Ya existe un programa con ese nombre")).toBeInTheDocument()
    expect(routerState.navigate).not.toHaveBeenCalled()
  })
})
