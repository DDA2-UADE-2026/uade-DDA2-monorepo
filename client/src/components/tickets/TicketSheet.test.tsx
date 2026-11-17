import type { ReactNode } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { toast } from "sonner"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { TicketSheet, TicketWorker } from "@/components/tickets/TicketSheet"
import { assign, get3 } from "@/generated/sdk.gen"
import type { SocialTicketDetailResponse, SocialTicketResponse, UserManagementResponse } from "@/generated/types.gen"

vi.mock("@/generated/sdk.gen", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/generated/sdk.gen")>(),
  get3: vi.fn(), assign: vi.fn(),
}))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children, to }: { children?: ReactNode; to: string }) => <a href={to}>{children}</a>,
}))
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }))
vi.mock("@/components/errors/showApiErrorToast", () => ({ showApiErrorToast: vi.fn() }))
vi.mock("@/components/UserAvatar", () => ({ UserAvatar: () => <span aria-hidden="true" /> }))
vi.mock("@/components/users/UserSelectionDialog", () => ({
  UserSelectionDialog: ({ title, onSelect, onOpenChange }: {
    title: string
    onSelect: (user: UserManagementResponse) => void
    onOpenChange: (open: boolean) => void
  }) => (
    <div role="dialog" aria-label={title}>
      <button type="button" onClick={() => onSelect({ id: 7, name: "Lucía Díaz" })}>Elegir a Lucía</button>
      <button type="button" onClick={() => onSelect({ id: 3, name: "Pedro Ruiz" })}>Elegir a Pedro</button>
      <button type="button" onClick={() => onOpenChange(false)}>Cancelar selección</button>
    </div>
  ),
}))

const responseMeta = { request: new Request("http://localhost"), response: new Response(), error: undefined }

const users = new Map<number, UserManagementResponse>([
  [3, { id: 3, name: "Pedro Ruiz", username: "pruiz", email: "pedro@muni.gob.ar" }],
  [7, { id: 7, name: "Lucía Díaz", email: "lucia@muni.gob.ar" }],
])

const ticket: SocialTicketResponse = {
  id: "ticket-uuid",
  publicId: "TCK-0042",
  citizenId: "citizen-9",
  responsibleAreaId: "M8",
  currentStatus: "ROUTED",
  currentPriority: "HIGH",
  state: "RECEIVED",
  applicationId: "solicitud-1",
  requiresAttention: true,
  informationPending: true,
  sourceUpdatedAt: "2026-11-10T12:00:00Z",
}

const detail: SocialTicketDetailResponse = {
  ticket,
  correlationId: "corr-1",
  responseCausationId: "cause-1",
  reopenReason: "El vecino aportó datos nuevos",
  informationResponse: { telefono: "1122334455" },
  routing: {
    summary: "Asistencia alimentaria",
    requestType: "SOLICITUD",
    ticketType: "SOCIAL",
    category: "Alimentos",
    subcategory: "Bolsón",
    description: "Familia con tres hijos",
    reassignment: true,
    resolutionDueAt: "2026-11-20T12:00:00Z",
    location: { addressLine: "", street: "Calle Falsa", streetNumber: "123", reference: "Portón verde", neighborhoodId: "barrio-7" },
    escalation: { active: true, reasonCode: "SLA_VENCIDO", escalatedAt: "2026-11-12T12:00:00Z" },
    formData: { integrantes: 5, tieneCud: true, recibePlan: false, observaciones: "", extra: { tipo: "urgente" } },
  },
  attachments: [
    { fileName: "dni.pdf", contentType: "application/pdf", sizeBytes: 2048, url: "https://files.example/dni.pdf" },
    { fileName: "foto.jpg", contentType: "", url: "https://files.example/foto.jpg" },
  ],
}

function renderSheet(props: Partial<Parameters<typeof TicketSheet>[0]> = {}) {
  const onClose = vi.fn()
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <TicketSheet ticket={ticket} users={users} canManage canPickUsers onClose={onClose} {...props} />
    </QueryClientProvider>,
  )
  return { onClose }
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(get3).mockResolvedValue({ ...responseMeta, data: detail })
})

describe("TicketSheet", () => {
  it("muestra la derivación completa que trae el detalle", async () => {
    renderSheet()

    expect(await screen.findByText("Asistencia alimentaria")).toBeInTheDocument()
    expect(get3).toHaveBeenCalledWith(expect.objectContaining({ path: { ticketId: "ticket-uuid" } }))
    expect(screen.getByText("TCK-0042 · ticket-uuid")).toBeInTheDocument()
    expect(screen.getByText("Requiere atención")).toBeInTheDocument()
    expect(screen.getByText("Información pendiente")).toBeInTheDocument()
    expect(screen.getAllByText("Recibido").length).toBeGreaterThan(0)
    expect(screen.getAllByText("Alta").length).toBeGreaterThan(0)
    expect(screen.getByText("Derivado")).toBeInTheDocument()
    expect(screen.getByText("citizen-9")).toBeInTheDocument()
    expect(screen.getByText("solicitud-1").closest("a")).toHaveAttribute("href", "/gestion/solicitudes/$solicitudId")
    expect(screen.getByText("corr-1")).toBeInTheDocument()
    expect(screen.getByText("El vecino aportó datos nuevos")).toBeInTheDocument()

    // Derivación, ubicación y escalamiento
    expect(screen.getByText("Familia con tres hijos")).toBeInTheDocument()
    expect(screen.getByText("Derivado nuevamente desde otra área")).toBeInTheDocument()
    expect(screen.getByText("Calle Falsa 123")).toBeInTheDocument()
    expect(screen.getByText("Portón verde")).toBeInTheDocument()
    expect(screen.getByText("barrio-7")).toBeInTheDocument()
    expect(screen.getByText("SLA_VENCIDO")).toBeInTheDocument()

    // Formulario: números, booleanos, vacíos y objetos
    expect(screen.getByText("integrantes").nextSibling).toHaveTextContent("5")
    expect(screen.getByText("tieneCud").nextSibling).toHaveTextContent("Sí")
    expect(screen.getByText("recibePlan").nextSibling).toHaveTextContent("No")
    expect(screen.getByText("observaciones").nextSibling).toHaveTextContent("—")
    expect(screen.getByText("extra").nextSibling).toHaveTextContent('"tipo": "urgente"')
    expect(screen.getByText(/"telefono": "1122334455"/)).toBeInTheDocument()

    // Adjuntos
    expect(screen.getByText("dni.pdf")).toBeInTheDocument()
    expect(screen.getByText("application/pdf · 2.0 KB")).toBeInTheDocument()
    expect(screen.getAllByText("Abrir")[0].closest("a")).toHaveAttribute("href", "https://files.example/dni.pdf")
  })

  it("asigna el ticket al administrativo elegido y avisa con su nombre", async () => {
    vi.mocked(assign).mockResolvedValue({ ...responseMeta, data: { ...detail, ticket: { ...ticket, assignedWorkerId: 7 } } })
    renderSheet()
    await screen.findByText("Asistencia alimentaria")
    expect(screen.getByText("Sin asignar")).toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "Asignar" }))
    fireEvent.click(screen.getByText("Elegir a Lucía"))

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Ticket asignado a Lucía Díaz"))
    expect(assign).toHaveBeenCalledWith(expect.objectContaining({ path: { ticketId: "ticket-uuid" }, body: { workerId: 7 } }))
    expect(await screen.findByRole("button", { name: "Reasignar" })).toBeInTheDocument()
    expect(screen.queryByLabelText("Asignar ticket")).not.toBeInTheDocument()
  })

  it("no vuelve a asignar al mismo administrativo y permite cancelar la selección", async () => {
    const assigned = { ...ticket, assignedWorkerId: 3 }
    vi.mocked(get3).mockResolvedValue({ ...responseMeta, data: { ...detail, ticket: assigned } })
    renderSheet({ ticket: assigned })
    await screen.findByText("Asistencia alimentaria")
    expect(screen.getByText("Pedro Ruiz")).toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "Reasignar" }))
    fireEvent.click(screen.getByText("Elegir a Pedro"))
    expect(screen.queryByLabelText("Reasignar ticket")).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole("button", { name: "Reasignar" }))
    fireEvent.click(screen.getByText("Cancelar selección"))
    expect(screen.queryByLabelText("Reasignar ticket")).not.toBeInTheDocument()
    expect(assign).not.toHaveBeenCalled()
  })

  it("informa el error de la API si la asignación falla", async () => {
    vi.mocked(assign).mockRejectedValue(new Error("El usuario no tiene el permiso"))
    renderSheet()
    await screen.findByText("Asistencia alimentaria")

    fireEvent.click(screen.getByRole("button", { name: "Asignar" }))
    fireEvent.click(screen.getByText("Elegir a Lucía"))

    await waitFor(() => expect(showApiErrorToast).toHaveBeenCalled())
    expect(toast.success).not.toHaveBeenCalled()
  })

  it("explica por qué no se puede asignar un ticket cerrado o sin permiso de usuarios", async () => {
    const closed: SocialTicketResponse = { ...ticket, state: "CLOSED", closedReason: "Resuelto por M2", anonymous: true, applicationId: undefined }
    vi.mocked(get3).mockResolvedValue({ ...responseMeta, data: { ticket: closed, attachments: [] } })
    renderSheet({ ticket: closed })

    expect(await screen.findByText("Sin adjuntos.")).toBeInTheDocument()
    expect(screen.getByText("El ticket no tiene una gestión activa en M8: no se puede asignar.")).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Asignar" })).not.toBeInTheDocument()
    expect(screen.getByText("Anónimo")).toBeInTheDocument()
    expect(screen.getByText("Sin vincular")).toBeInTheDocument()
    expect(screen.getByText("Resuelto por M2")).toBeInTheDocument()
    expect(screen.queryByText("Ubicación")).not.toBeInTheDocument()
  })

  it("pide el permiso de usuarios cuando el ticket es gestionable", async () => {
    renderSheet({ canPickUsers: false })
    await screen.findByText("Asistencia alimentaria")
    expect(screen.getByText("Para asignar necesitás además el permiso users:view.")).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Asignar" })).not.toBeInTheDocument()
  })

  it("permite reintentar si no se pudo cargar el detalle", async () => {
    vi.mocked(get3).mockRejectedValueOnce(new Error("Timeout"))
    renderSheet()

    expect(await screen.findByText("No se pudo cargar el detalle del ticket: Timeout")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Reintentar" }))
    expect(await screen.findByText("Asistencia alimentaria")).toBeInTheDocument()
    expect(get3).toHaveBeenCalledTimes(2)
  })

  it("no consulta el detalle ni muestra el panel sin ticket seleccionado", () => {
    renderSheet({ ticket: null })
    expect(get3).not.toHaveBeenCalled()
    expect(screen.queryByText("Administrativo asignado")).not.toBeInTheDocument()
  })

  it("avisa al cerrar el panel", async () => {
    const { onClose } = renderSheet()
    await screen.findByText("Asistencia alimentaria")
    fireEvent.keyDown(document.activeElement ?? document.body, { key: "Escape" })
    await waitFor(() => expect(onClose).toHaveBeenCalled())
  })
})

describe("TicketWorker", () => {
  it("usa un nombre genérico si el usuario no está en el directorio", () => {
    render(<TicketWorker workerId={99} users={users} />)
    expect(screen.getByText("Usuario #99")).toBeInTheDocument()
  })
})
