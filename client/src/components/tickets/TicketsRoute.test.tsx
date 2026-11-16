import type { ComponentType } from "react"
import { fireEvent, screen, waitFor } from "@testing-library/react"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"

import { findAll, get3, list9 } from "@/generated/sdk.gen"
import type { SocialTicketResponse, UserResponse } from "@/generated/types.gen"
import { TICKET_MANAGE_PERMISSION, TICKET_VIEW_PERMISSION } from "@/lib/tickets"
import { Route } from "@/routes/_app/gestion/tickets/index"
import { preloadRouteComponent, renderRouteComponent, routeOptions, routerState } from "@/test/route-harness"

type TicketSearch = { page: number; estado?: string; atencion?: boolean }

vi.mock("@tanstack/react-router", async (importOriginal) =>
  (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
vi.mock("@/components/layout/OutletNav", async () => (await import("@/test/route-harness")).outletNavMock)
vi.mock("@/components/layout/OutletNavBreadcrumbs", () => ({ OutletNavBreadcrumbs: () => <nav>Tickets</nav> }))
vi.mock("@/components/UserAvatar", () => ({ UserAvatar: () => <span aria-hidden="true" /> }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  list9: vi.fn(), findAll: vi.fn(), get3: vi.fn(),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

const tickets: SocialTicketResponse[] = [
  { id: "t-1", publicId: "TCK-1", state: "RECEIVED", currentStatus: "ROUTED", currentPriority: "LOW", citizenId: "citizen-1", assignedWorkerId: 5, requiresAttention: true, informationPending: true, responsibleAreaId: "M8" },
  { id: "t-2", publicId: "TCK-2", state: "CLOSED", currentStatus: "CLOSED", anonymous: true, responsibleAreaId: "M8" },
]

let RouteComponent: ComponentType

beforeAll(async () => {
  RouteComponent = await preloadRouteComponent(Route)
})

function renderRoute(permissions: string[], search: TicketSearch = { page: 1 }) {
  routerState.context = { user: { name: "Admin", permissions } satisfies UserResponse }
  routerState.search = search
  return renderRouteComponent(RouteComponent)
}

const lastSearchUpdate = () => {
  const { search } = routerState.navigate.mock.lastCall![0] as { search: (prev: TicketSearch) => TicketSearch }
  return search({ page: 3, estado: "RECEIVED", atencion: true })
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(list9).mockResolvedValue({ ...responseMeta, data: { content: tickets, totalElements: 45, totalPages: 3 } })
  vi.mocked(findAll).mockResolvedValue({ ...responseMeta, data: [{ id: 5, name: "Lucía Díaz", email: "lucia@muni.gob.ar" }] })
  vi.mocked(get3).mockResolvedValue({ ...responseMeta, data: { ticket: tickets[0], attachments: [] } })
})

describe("bandeja de tickets", () => {
  it("sin el permiso de tickets no consulta la API", async () => {
    renderRoute([])
    expect(await screen.findByText("Sin acceso")).toBeInTheDocument()
    expect(list9).not.toHaveBeenCalled()
    expect(findAll).not.toHaveBeenCalled()
  })

  it("lista los tickets con el administrativo resuelto desde el directorio", async () => {
    renderRoute([TICKET_VIEW_PERMISSION, "users:view"], { page: 2, estado: "RECEIVED", atencion: true })

    expect(await screen.findByText("TCK-1")).toBeInTheDocument()
    expect(list9).toHaveBeenCalledWith(expect.objectContaining({ query: { page: 1, size: 20, state: "RECEIVED", attention: true } }))
    expect(await screen.findByText("Lucía Díaz")).toBeInTheDocument()
    expect(screen.getByLabelText("Requiere atención")).toBeInTheDocument()
    expect(screen.getByLabelText("Información pendiente")).toBeInTheDocument()
    expect(screen.getByText("Origen: Derivado")).toBeInTheDocument()
    expect(screen.getByText("citizen-1")).toBeInTheDocument()
    expect(screen.getByText("Anónimo")).toBeInTheDocument()
    expect(screen.getByText("Sin asignar")).toBeInTheDocument()
  })

  it("no pide el directorio de usuarios sin users:view", async () => {
    renderRoute([TICKET_VIEW_PERMISSION])
    expect(await screen.findByText("TCK-1")).toBeInTheDocument()
    expect(findAll).not.toHaveBeenCalled()
    expect(screen.getByText("Usuario #5")).toBeInTheDocument()
  })

  it("abre el detalle del ticket con clic o con teclado", async () => {
    renderRoute([TICKET_VIEW_PERMISSION, TICKET_MANAGE_PERMISSION, "users:view"])
    const row = (await screen.findByText("TCK-1")).closest("tr")!

    fireEvent.keyDown(row, { key: "Tab" })
    expect(get3).not.toHaveBeenCalled()

    fireEvent.keyDown(row, { key: "Enter" })
    await waitFor(() => expect(get3).toHaveBeenCalledWith(expect.objectContaining({ path: { ticketId: "t-1" } })))
    expect(await screen.findByText("Administrativo asignado")).toBeInTheDocument()

    fireEvent.keyDown(document.activeElement ?? document.body, { key: "Escape" })
    await waitFor(() => expect(screen.queryByText("Administrativo asignado")).not.toBeInTheDocument())

    fireEvent.click(screen.getByText("TCK-2").closest("tr")!)
    await waitFor(() => expect(get3).toHaveBeenCalledWith(expect.objectContaining({ path: { ticketId: "t-2" } })))
  })

  it("filtra por atención y vuelve a la primera página", async () => {
    renderRoute([TICKET_VIEW_PERMISSION])
    await screen.findByText("TCK-1")

    fireEvent.click(screen.getByRole("switch"))
    expect(lastSearchUpdate()).toEqual({ page: 1, estado: "RECEIVED", atencion: true })
  })

  it("pagina con el total informado por la API", async () => {
    renderRoute([TICKET_VIEW_PERMISSION])
    await screen.findByText("TCK-1")

    fireEvent.click(screen.getByText("2"))
    expect(lastSearchUpdate()).toEqual({ page: 2, estado: "RECEIVED", atencion: true })
  })

  it("refresca el listado a pedido", async () => {
    renderRoute([TICKET_VIEW_PERMISSION])
    await screen.findByText("TCK-1")
    expect(screen.getByText(/^Actualizado /)).toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "Refrescar" }))
    await waitFor(() => expect(list9).toHaveBeenCalledTimes(2))
  })

  it("distingue la bandeja vacía de un filtro sin resultados", async () => {
    vi.mocked(list9).mockResolvedValue({ ...responseMeta, data: { content: [], totalElements: 0, totalPages: 0 } })
    const { unmount } = renderRoute([TICKET_VIEW_PERMISSION])
    expect(await screen.findByText("Todavía no hay tickets")).toBeInTheDocument()
    unmount()

    renderRoute([TICKET_VIEW_PERMISSION], { page: 1, estado: "CLOSED" })
    expect(await screen.findByText("Sin resultados")).toBeInTheDocument()
    expect(screen.getByText("No hay tickets para estos filtros.")).toBeInTheDocument()
  })

  it("permite reintentar si el listado falla", async () => {
    vi.mocked(list9).mockRejectedValueOnce(new Error("Sin conexión"))
    renderRoute([TICKET_VIEW_PERMISSION])

    expect(await screen.findByText("No se pudieron cargar los tickets: Sin conexión")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Reintentar" }))
    expect(await screen.findByText("TCK-1")).toBeInTheDocument()
  })

  it("valida los parámetros de búsqueda de la URL", () => {
    const validateSearch = routeOptions(Route).validateSearch!
    expect(validateSearch.parse({ page: "2", estado: "CLOSED", atencion: true })).toEqual({ page: 2, estado: "CLOSED", atencion: true })
    expect(validateSearch.parse({ page: "-1", estado: "INVALIDO", atencion: "x" })).toEqual({ page: 1, estado: undefined, atencion: undefined })
  })
})

describe("filtro por estado", () => {
  it("traduce las opciones del selector", async () => {
    renderRoute([TICKET_VIEW_PERMISSION], { page: 1, estado: "WAITING_INFORMATION" })
    await screen.findByText("TCK-1")
    expect(document.getElementById("ticket-state")).toHaveTextContent("Esperando información")
  })
})
