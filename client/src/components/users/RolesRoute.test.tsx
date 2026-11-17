import type { ComponentType } from "react"
import { fireEvent, screen, waitFor } from "@testing-library/react"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"

import { create1, findAll1, findAll4, update1 } from "@/generated/sdk.gen"
import { Route } from "@/routes/_app/gestion/roles"
import { preloadRouteComponent, renderRouteComponent } from "@/test/route-harness"

vi.mock("@tanstack/react-router", async (importOriginal) =>
  (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
vi.mock("@/components/layout/OutletNav", async () => (await import("@/test/route-harness")).outletNavMock)
vi.mock("@/components/layout/OutletNavBreadcrumbs", () => ({ OutletNavBreadcrumbs: () => null }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  findAll1: vi.fn(), findAll4: vi.fn(), create1: vi.fn(), update1: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

let RouteComponent: ComponentType

beforeAll(async () => {
  RouteComponent = await preloadRouteComponent(Route)
})

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(findAll1).mockResolvedValue({ ...responseMeta, data: [{ id: 2, name: "AUDITOR", permissions: ["roles:view"] }] })
  vi.mocked(findAll4).mockResolvedValue({ ...responseMeta, data: [{ id: 1, name: "roles:view" }, { id: 2, name: "tickets:management:view" }] })
  vi.mocked(create1).mockResolvedValue({ ...responseMeta, data: { id: 3, name: "MESA" } })
  vi.mocked(update1).mockResolvedValue({ ...responseMeta, data: { id: 2, name: "AUDITOR" } })
})

const submit = () => fireEvent.submit(document.getElementById("role-form")!)

describe("roles y permisos", () => {
  it("crea un rol con los permisos elegidos", async () => {
    renderRouteComponent(RouteComponent)
    await screen.findByText("AUDITOR")

    fireEvent.click(screen.getByRole("button", { name: "Nuevo rol" }))
    fireEvent.change(await screen.findByLabelText("Nombre del rol"), { target: { value: "MESA" } })
    fireEvent.click(await screen.findByRole("switch", { name: "tickets:management:view" }))
    submit()

    await waitFor(() => expect(create1).toHaveBeenCalledWith(expect.objectContaining({ body: { name: "MESA", permissions: ["tickets:management:view"] } })))
    await waitFor(() => expect(screen.queryByText("Definí el nombre y los permisos del nuevo rol.")).not.toBeInTheDocument())
    expect(findAll1).toHaveBeenCalledTimes(2)
  })

  it("edita los permisos de un rol existente", async () => {
    renderRouteComponent(RouteComponent)
    await screen.findByText("AUDITOR")

    fireEvent.click(screen.getByRole("button", { name: "Editar AUDITOR" }))
    fireEvent.click(await screen.findByRole("switch", { name: "roles:view" }))
    submit()

    await waitFor(() => expect(update1).toHaveBeenCalledWith(expect.objectContaining({ path: { id: 2 }, body: { name: "AUDITOR", permissions: [] } })))
    await waitFor(() => expect(screen.queryByText("Editar rol")).not.toBeInTheDocument())
  })
})
