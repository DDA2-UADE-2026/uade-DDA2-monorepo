import type { ComponentProps, ReactNode } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { ActivityAttendance } from "@/components/activities/ActivityAttendance"
import { CitizenActivities } from "@/components/activities/CitizenActivities"
import { CitizenActivityDetail } from "@/components/activities/CitizenActivityDetail"
import { ProfessionalActivities } from "@/components/activities/ProfessionalActivities"
import { SidebarProvider } from "@/components/ui/sidebar"
import { client } from "@/generated/client.gen"
import type { CitizenActivityResponse, ProfessionalActivityEnrollmentResponse } from "@/generated/types.gen"

vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children, to, params, search, ...props }: ComponentProps<"a"> & { to: string; params?: Record<string, string>; search?: Record<string, unknown> }) => {
    const path = to.replace(/\$(\w+)/g, (_, key: string) => params?.[key] ?? "")
    const query = search ? `?${new URLSearchParams(Object.entries(search).map(([key, value]) => [key, String(value)]))}` : ""
    return <a {...props} href={path + query}>{children}</a>
  },
}))

const originalConfig = client.getConfig()
let queryClient: QueryClient
let requests: Request[]
let respond: (request: Request) => Promise<Response>
const activity: CitizenActivityResponse = { id: "activity-1", name: "Taller de lectura", description: "Lectura en comunidad", location: "Centro Norte", startDate: "2026-10-20", endDate: "2026-10-20", status: "OPEN", capacity: 10, enrolledCount: 1, availableCapacity: 9 }
const participant: ProfessionalActivityEnrollmentResponse = { id: "enrollment-1", activityId: "activity-1", citizenId: 3, citizenName: "Ana Pérez", enrolledAt: "2026-10-13T12:00:00Z" }
const pageOf = <T,>(content: T[]) => ({ content, page: 0, size: 10, totalElements: content.length, totalPages: 1 })

function show(children: ReactNode) {
  return render(<QueryClientProvider client={queryClient}><SidebarProvider>{children}</SidebarProvider></QueryClientProvider>)
}

beforeEach(() => {
  requests = []
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  respond = async () => Response.json({}, { status: 500 })
  client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async (input) => {
    const request = input as Request
    requests.push(request.clone())
    return respond(request)
  }) })
})

afterEach(() => {
  queryClient.clear()
  client.setConfig(originalConfig)
})

describe("actividades ciudadanas", () => {
  it("recupera la inscripción del servidor y consulta también el historial cerrado", async () => {
    respond = async () => Response.json(pageOf([{ ...activity, status: "CLOSED", myEnrollment: { id: "enrollment-1", enrolledAt: participant.enrolledAt, attendance: "PRESENT" } }]))
    show(<CitizenActivities page={1} view="inscripciones" canEnroll onSearchChange={vi.fn()} />)
    expect(await screen.findByRole("button", { name: "Ya estás inscripto" })).toBeDisabled()
    expect(screen.getByText("Presente")).toBeInTheDocument()
    expect(screen.getByText("Cerrada")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Ver detalle" })).toHaveAttribute("href", "/portal/actividades/activity-1")
    expect(new URL(requests[0].url).searchParams.get("enrolledOnly")).toBe("true")
  })

  it("confirma la inscripción y la conserva al volver a cargar la pantalla", async () => {
    let enrolled = false
    respond = async (request) => {
      if (request.method === "POST") { enrolled = true; return Response.json({ id: "enrollment-1", activityId: activity.id, confirmation: "CONFIRMED" }, { status: 201 }) }
      return Response.json(pageOf([{ ...activity, myEnrollment: enrolled ? { id: "enrollment-1", enrolledAt: participant.enrolledAt } : undefined }]))
    }
    const user = userEvent.setup()
    const view = show(<CitizenActivities page={1} view="disponibles" canEnroll onSearchChange={vi.fn()} />)
    await user.click(await screen.findByRole("button", { name: "Inscribirme" }))
    await user.click(screen.getByRole("button", { name: "Confirmar inscripción" }))
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(screen.getByRole("button", { name: "Ya estás inscripto" })).toBeDisabled()
    expect(requests.filter((request) => request.method === "POST")).toHaveLength(1)
    expect(new URL(requests.find((request) => request.method === "POST")!.url).pathname).toBe("/api/activities/activity-1/enrollments")
    view.unmount()
    queryClient.clear()
    show(<CitizenActivities page={1} view="disponibles" canEnroll onSearchChange={vi.fn()} />)
    expect(await screen.findByRole("button", { name: "Ya estás inscripto" })).toBeDisabled()
  })

  it("informa un cupo agotado durante la confirmación y refresca la oferta", async () => {
    let full = false
    respond = async (request) => {
      if (request.method === "POST") { full = true; return Response.json({ status: 409, code: "ACTIVITY_CAPACITY_FULL", message: "La actividad no tiene cupos disponibles." }, { status: 409 }) }
      return Response.json(pageOf([{ ...activity, availableCapacity: full ? 0 : 1 }]))
    }
    const user = userEvent.setup()
    show(<CitizenActivities page={1} view="disponibles" canEnroll onSearchChange={vi.fn()} />)
    await user.click(await screen.findByRole("button", { name: "Inscribirme" }))
    await user.click(screen.getByRole("button", { name: "Confirmar inscripción" }))
    expect(await screen.findByRole("alert")).toHaveTextContent("La actividad no tiene cupos disponibles.")
    await user.click(screen.getByRole("button", { name: "Cancelar" }))
    expect(await screen.findByRole("button", { name: "Sin cupo" })).toBeDisabled()
  })

  it("muestra la asistencia en el detalle cerrado sin ofrecer otra inscripción", async () => {
    respond = async () => Response.json({ ...activity, status: "CLOSED", myEnrollment: { id: "enrollment-1", enrolledAt: participant.enrolledAt, attendance: "ABSENT" } })
    show(<CitizenActivityDetail activityId="activity-1" canEnroll />)
    expect(await screen.findByText("Tu inscripción está confirmada")).toBeInTheDocument()
    expect(screen.getByText("Ausente")).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: /Inscribirme/ })).not.toBeInTheDocument()
    expect(new URL(requests[0].url).pathname).toBe("/api/activities/activity-1")
  })

  it("respeta el permiso de inscripción y muestra un historial vacío", async () => {
    respond = async () => Response.json(pageOf([activity]))
    const view = show(<CitizenActivities page={1} view="disponibles" canEnroll={false} onSearchChange={vi.fn()} />)
    await screen.findByText("Taller de lectura")
    expect(screen.queryByRole("button", { name: "Inscribirme" })).not.toBeInTheDocument()
    view.unmount()
    respond = async () => Response.json(pageOf([]))
    show(<CitizenActivities page={1} view="inscripciones" canEnroll={false} onSearchChange={vi.fn()} />)
    expect(await screen.findByText("Todavía no te inscribiste en ninguna actividad.")).toBeInTheDocument()
  })
})

describe("asistencia profesional", () => {
  it("lista actividades desde la API profesional y enlaza al registro de asistencia", async () => {
    respond = async () => Response.json(pageOf([activity]))
    show(<ProfessionalActivities page={1} onPageChange={vi.fn()} />)
    expect(await screen.findByRole("button", { name: "Ver asistencia" })).toHaveAttribute("href", "/gestion/actividades/activity-1/asistencia?page=1")
    expect(new URL(requests[0].url).pathname).toBe("/api/professional/activities")
  })

  it("registra y corrige la asistencia, mostrando el profesional que guardó el cambio", async () => {
    let enrollment = { ...participant }
    respond = async (request) => {
      if (request.method === "PATCH") {
        const body = await request.json() as { attendance: "PRESENT" | "ABSENT" }
        enrollment = { ...enrollment, attendance: body.attendance, attendanceRecordedBy: { id: 5, name: "Profesional Uno" }, attendanceRecordedAt: "2026-10-20T13:00:00Z" }
        return Response.json(enrollment)
      }
      return Response.json(new URL(request.url).pathname.endsWith("/enrollments") ? pageOf([enrollment]) : activity)
    }
    const user = userEvent.setup()
    show(<ActivityAttendance activityId="activity-1" page={1} canManage onPageChange={vi.fn()} />)
    await user.click(await screen.findByRole("button", { name: "Marcar presente a Ana Pérez" }))
    await waitFor(() => expect(screen.getByRole("button", { name: "Marcar presente a Ana Pérez" })).toBeDisabled())
    expect(await screen.findByText("Profesional Uno")).toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Marcar ausente a Ana Pérez" }))
    await waitFor(() => expect(screen.getByRole("button", { name: "Marcar ausente a Ana Pérez" })).toBeDisabled())
    const changes = requests.filter((request) => request.method === "PATCH")
    expect(changes).toHaveLength(2)
    expect(new URL(changes[0].url).pathname).toBe("/api/professional/activities/activity-1/enrollments/enrollment-1/attendance")
    expect(await changes[0].json()).toEqual({ attendance: "PRESENT" })
    expect(await changes[1].json()).toEqual({ attendance: "ABSENT" })
  })

  it("no ofrece cambios al usuario con permiso de solo lectura", async () => {
    respond = async (request) => Response.json(new URL(request.url).pathname.endsWith("/enrollments") ? pageOf([participant]) : activity)
    show(<ActivityAttendance activityId="activity-1" page={1} canManage={false} onPageChange={vi.fn()} />)
    expect(await screen.findByText("Ana Pérez")).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: /Marcar/ })).not.toBeInTheDocument()
  })

  it("conserva el estado sin registrar si falla el guardado y permite reintentar", async () => {
    let fail = true
    respond = async (request) => {
      if (request.method === "PATCH") return fail ? Response.json({ status: 500, message: "No se pudo guardar la asistencia." }, { status: 500 }) : Response.json({ ...participant, attendance: "PRESENT" })
      return Response.json(new URL(request.url).pathname.endsWith("/enrollments") ? pageOf([{ ...participant, attendance: fail ? undefined : "PRESENT" }]) : activity)
    }
    const user = userEvent.setup()
    show(<ActivityAttendance activityId="activity-1" page={1} canManage onPageChange={vi.fn()} />)
    await user.click(await screen.findByRole("button", { name: "Marcar presente a Ana Pérez" }))
    expect(await screen.findByRole("alert")).toHaveTextContent("No se pudo guardar la asistencia.")
    expect(screen.getByText("Sin registrar")).toBeInTheDocument()
    fail = false
    await user.click(screen.getByRole("button", { name: "Marcar presente a Ana Pérez" }))
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument())
    expect(requests.filter((request) => request.method === "PATCH")).toHaveLength(2)
  })

  it("no consulta inscriptos cuando el detalle de la actividad está prohibido", async () => {
    respond = async () => Response.json({ status: 403, message: "Sin permiso para esta actividad." }, { status: 403 })
    show(<ActivityAttendance activityId="activity-1" page={1} canManage={false} onPageChange={vi.fn()} />)
    expect(await screen.findByRole("alert")).toHaveTextContent("Sin permiso para esta actividad.")
    expect(requests).toHaveLength(1)
  })
})
