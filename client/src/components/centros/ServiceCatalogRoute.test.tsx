import type { ComponentType } from "react"
import { fireEvent, screen, waitFor } from "@testing-library/react"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"

import {
  activateMunicipalService,
  createMunicipalService,
  deactivateMunicipalService,
  listMunicipalServices,
  updateMunicipalService,
} from "@/generated/sdk.gen"
import type { MunicipalServiceResponse } from "@/generated/types.gen"
import { Route } from "@/routes/_app/gestion/centros/servicios"
import { preloadRouteComponent, renderRouteComponent, routerState } from "@/test/route-harness"

vi.mock("@tanstack/react-router", async (importOriginal) =>
  (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
vi.mock("@/components/layout/OutletNav", async () => (await import("@/test/route-harness")).outletNavMock)
vi.mock("@/components/layout/OutletNavBreadcrumbs", () => ({ OutletNavBreadcrumbs: () => <nav>Catálogo de servicios</nav> }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  listMunicipalServices: vi.fn(),
  createMunicipalService: vi.fn(),
  updateMunicipalService: vi.fn(),
  activateMunicipalService: vi.fn(),
  deactivateMunicipalService: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

const services: MunicipalServiceResponse[] = [
  { id: "srv-1", name: "Psicología", description: "Atención psicológica", durationMinutes: 45, active: true, updatedAt: "2026-11-10T12:00:00Z" },
  { id: "srv-2", name: "Nutrición", durationMinutes: 30, active: false },
]

let RouteComponent: ComponentType

beforeAll(async () => {
  RouteComponent = await preloadRouteComponent(Route)
})

function renderCatalog() {
  routerState.search = { page: 1, search: "", estado: "todos" }
  return renderRouteComponent(RouteComponent)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(listMunicipalServices).mockResolvedValue({ ...responseMeta, data: { content: services, totalElements: 2, totalPages: 1 } })
  vi.mocked(createMunicipalService).mockResolvedValue({ ...responseMeta, data: { id: "srv-3" } })
  vi.mocked(updateMunicipalService).mockResolvedValue({ ...responseMeta, data: services[0] })
  vi.mocked(activateMunicipalService).mockResolvedValue({ ...responseMeta, data: { ...services[1], active: true } })
  vi.mocked(deactivateMunicipalService).mockResolvedValue({ ...responseMeta, data: { ...services[0], active: false } })
})

describe("catálogo de servicios", () => {
  it("lista los servicios y busca con Enter", async () => {
    renderCatalog()
    expect(await screen.findByText("Psicología")).toBeInTheDocument()
    expect(screen.getByText("45 min")).toBeInTheDocument()
    expect(screen.getByText("Inactivo")).toBeInTheDocument()

    const input = screen.getByLabelText("Buscar servicios")
    fireEvent.change(input, { target: { value: "  psico " } })
    fireEvent.keyDown(input, { key: "a" })
    expect(routerState.navigate).not.toHaveBeenCalled()
    fireEvent.keyDown(input, { key: "Enter" })
    expect(routerState.navigate).toHaveBeenCalledWith({ search: { page: 1, search: "psico", estado: "todos" } })
  })

  it("crea un servicio nuevo y refresca el listado", async () => {
    renderCatalog()
    await screen.findByText("Psicología")

    fireEvent.click(screen.getByRole("button", { name: "Nuevo servicio" }))
    fireEvent.change(screen.getByLabelText("Nombre"), { target: { value: " Trabajo social " } })
    fireEvent.change(screen.getByLabelText("Descripción"), { target: { value: "Orientación" } })
    fireEvent.submit(document.getElementById("service-form")!)

    await waitFor(() => expect(createMunicipalService).toHaveBeenCalledWith(expect.objectContaining({
      body: { name: "Trabajo social", description: "Orientación", durationMinutes: 30 },
    })))
    await waitFor(() => expect(screen.queryByText("Definí el servicio una sola vez en el catálogo municipal.")).not.toBeInTheDocument())
    expect(listMunicipalServices).toHaveBeenCalledTimes(2)
  })

  it("edita un servicio existente", async () => {
    renderCatalog()
    await screen.findByText("Psicología")

    fireEvent.click(screen.getByRole("button", { name: "Editar servicio Psicología" }))
    fireEvent.change(screen.getByLabelText("Duración estimada (minutos)"), { target: { value: "60" } })
    fireEvent.submit(document.getElementById("service-form")!)

    await waitFor(() => expect(updateMunicipalService).toHaveBeenCalledWith(expect.objectContaining({
      path: { id: "srv-1" },
      body: { name: "Psicología", description: "Atención psicológica", durationMinutes: 60 },
    })))
    await waitFor(() => expect(screen.queryByText("Editar servicio")).not.toBeInTheDocument())
  })

  it("desactiva un servicio activo y activa uno inactivo", async () => {
    renderCatalog()
    await screen.findByText("Psicología")

    fireEvent.click(screen.getByRole("button", { name: "Desactivar servicio Psicología" }))
    fireEvent.click(await screen.findByRole("button", { name: "Desactivar" }))
    await waitFor(() => expect(deactivateMunicipalService).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "srv-1" } })))
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument())

    fireEvent.click(screen.getByRole("button", { name: "Activar servicio Nutrición" }))
    fireEvent.click(await screen.findByRole("button", { name: "Activar" }))
    await waitFor(() => expect(activateMunicipalService).toHaveBeenCalledWith(expect.objectContaining({ path: { id: "srv-2" } })))
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument())
  })
})
