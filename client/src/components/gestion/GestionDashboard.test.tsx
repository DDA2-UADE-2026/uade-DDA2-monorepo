import type { ReactNode } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { GestionDashboard } from "@/components/gestion/GestionDashboard"
import { findAll, list1, list4, list7, listMunicipalCenters } from "@/generated/sdk.gen"
import type { UserResponse } from "@/generated/types.gen"
import { useMe } from "@/hooks/use-auth"

vi.mock("@/hooks/use-auth", () => ({ useMe: vi.fn() }))
vi.mock("@/components/UserAvatar", () => ({ UserAvatar: () => <span aria-hidden="true" /> }))
vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  findAll: vi.fn(), list1: vi.fn(), list4: vi.fn(), list7: vi.fn(), listMunicipalCenters: vi.fn(),
}))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children, to }: { children?: ReactNode; to: string }) => <a href={to}>{children}</a>,
}))

const admin: UserResponse = {
  name: "Administradora",
  activeRole: "ADMIN",
  permissions: ["users:view", "programs:management:view", "applications:management:view", "applications:management:create", "activities:management:view", "centers:management:view"],
}

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

function renderDashboard(user = admin) {
  vi.mocked(useMe).mockReturnValue({ data: { user } } as ReturnType<typeof useMe>)
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
  return render(<QueryClientProvider client={client}><GestionDashboard /></QueryClientProvider>)
}

beforeEach(() => {
  vi.resetAllMocks()
  // Keep the generated query functions in use; only replace their HTTP calls.
  vi.mocked(findAll).mockResolvedValue({ ...responseMeta, data: [
    { id: 1, name: "Ana Pérez", active: true, roles: ["ADMIN"], createdAt: "2026-10-01T12:00:00Z" },
    { id: 2, name: "Juan López", active: false, roles: ["CIUDADANO"], createdAt: "2026-10-12T12:00:00Z" },
  ] })
  vi.mocked(list1).mockResolvedValue({ ...responseMeta, data: { content: [{ id: "programa-1", name: "Apoyo escolar", active: true }], totalElements: 42 } })
  vi.mocked(list4).mockResolvedValue({ ...responseMeta, data: { content: [{ id: "solicitud-1", applicationNumber: 121, userName: "Marta Gómez", programName: "Apoyo escolar", status: "SUBMITTED" }], totalElements: 121 } })
  vi.mocked(list7).mockResolvedValue({ ...responseMeta, data: { content: [{ id: "actividad-1", name: "Taller de lectura", status: "OPEN", location: "Biblioteca", startDate: "2026-10-20" }], totalElements: 18 } })
  vi.mocked(listMunicipalCenters).mockResolvedValue({ ...responseMeta, data: { content: [{ id: "centro-1", name: "Centro Norte", address: "Av. San Martín 120", active: true }], totalElements: 8 } })
})

describe("resumen de gestión", () => {
  it("combina las cinco fuentes y usa los totales, no el tamaño de las vistas previas", async () => {
    renderDashboard()
    await screen.findByText("Centro Norte")
    expect(screen.getByText("Taller de lectura")).toBeInTheDocument()
    expect(screen.getByText("#121")).toBeInTheDocument()
    expect(screen.getByText("Marta Gómez")).toBeInTheDocument()
    expect(screen.getByText("Ana Pérez")).toBeInTheDocument()
    const summary = within(screen.getByRole("region", { name: "Resumen de la gestión" }))
    for (const total of ["2", "42", "121", "18", "8"]) expect(summary.getByText(total)).toBeInTheDocument()
    const usersPanel = screen.getByRole("heading", { name: "Usuarios recientes" }).closest('[data-slot="card"]')!
    expect(within(usersPanel as HTMLElement).getAllByRole("listitem")[0]).toHaveTextContent("Juan López")
    expect(screen.getByText("Activas: 1")).toBeInTheDocument()
    expect(list1).toHaveBeenCalledWith(expect.objectContaining({ query: { page: 0, size: 5 } }))
  })

  it("mantiene las otras secciones disponibles si una falla y permite reintentarla", async () => {
    vi.mocked(list1).mockRejectedValueOnce(new Error("Sin conexión"))
    renderDashboard()
    await screen.findByRole("alert")
    expect(await screen.findByText("Centro Norte")).toBeInTheDocument()
    expect(screen.getByText("No disponible por el momento")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Reintentar" }))
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument())
    expect(list1).toHaveBeenCalledTimes(2)
    expect(listMunicipalCenters).toHaveBeenCalledTimes(1)
  })

  it("consulta únicamente las áreas permitidas y actualiza solo esas fuentes", async () => {
    renderDashboard({ name: "Operador", activeRole: "OPERADOR", permissions: ["programs:management:view"] })
    await screen.findByText("Apoyo escolar")
    expect(screen.queryByRole("heading", { name: "Usuarios recientes" })).not.toBeInTheDocument()
    expect(screen.queryByText("Solicitud asistida")).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Refrescar resumen" }))
    await waitFor(() => expect(list1).toHaveBeenCalledTimes(2))
    for (const request of [findAll, list4, list7, listMunicipalCenters]) expect(request).not.toHaveBeenCalled()
  })

  it("muestra un estado vacío sin inventar datos cuando las fuentes no tienen registros", async () => {
    vi.mocked(list1).mockResolvedValue({ ...responseMeta, data: { content: [], totalElements: 0 } })
    renderDashboard({ name: "Operador", activeRole: "OPERADOR", permissions: ["programs:management:view"] })
    expect(await screen.findByText("Todavía no hay registros para mostrar.")).toBeInTheDocument()
    expect(screen.getByText("0")).toBeInTheDocument()
  })
})
