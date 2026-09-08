import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import type { ReactNode } from "react"
import { toast } from "sonner"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { client } from "@/generated/client.gen"
import { get1QueryKey } from "@/generated/@tanstack/react-query.gen"
import { enrollmentToday, submissionStorageKey } from "@/lib/application-flow"
import { ApplicationConfirmation } from "@/routes/_app/portal/solicitudes/nueva"

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  useNavigate: () => navigate,
  Link: ({ children, className }: { children?: ReactNode; className?: string }) => <a href="#" className={className}>{children}</a>,
}))

const originalConfig = client.getConfig()
let queryClient: QueryClient
let requests: Request[]
let respond: () => Promise<Response>

function renderConfirmation() {
  const today = enrollmentToday()
  return render(<QueryClientProvider client={queryClient}>
    <ApplicationConfirmation userId={42} program={{ id: "program-1", name: "Apoyo alimentario" }}
      edition={{ status: "ACTIVE", name: "Edición 2026", documentRequirements: [{ id: "dni", name: "DNI", required: true }] }}
      period={{ id: "period-1", status: "OPEN", openDate: today, closeDate: today }} />
  </QueryClientProvider>)
}

describe("presentación de una solicitud", () => {
  beforeEach(() => {
    vi.spyOn(toast, "error").mockReturnValue("submission-error")
    navigate.mockReset()
    sessionStorage.clear()
    requests = []
    respond = async () => Response.json({ id: "application-1", status: "SUBMITTED" }, { status: 201 })
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async (input) => {
      requests.push(input as Request)
      return respond()
    }) })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    queryClient.clear()
    sessionStorage.clear()
    client.setConfig(originalConfig)
  })

  it("espera la confirmación y evita duplicar el envío mientras está pendiente", async () => {
    let finish!: (response: Response) => void
    respond = () => new Promise((resolve) => { finish = resolve })
    const user = userEvent.setup()
    renderConfirmation()
    expect(screen.getByText("Apoyo alimentario")).toBeInTheDocument()
    expect(screen.getByText("DNI")).toBeInTheDocument()
    expect(requests).toHaveLength(0)
    await user.dblClick(screen.getByRole("button", { name: "Presentar solicitud" }))
    await waitFor(() => expect(requests).toHaveLength(1))
    expect(screen.getByRole("button", { name: "Presentando…" })).toBeDisabled()
    expect(requests[0].method).toBe("POST")
    expect(new URL(requests[0].url).pathname).toBe("/api/applications")
    expect(await requests[0].json()).toEqual({ enrollmentPeriodId: "period-1" })
    expect(requests[0].headers.get("Idempotency-Key")).toMatch(/^[0-9a-f-]{36}$/)
    finish(Response.json({ id: "application-1", status: "SUBMITTED" }, { status: 201 }))
    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: "/portal/solicitudes/$solicitudId", params: { solicitudId: "application-1" }, replace: true }))
    expect(queryClient.getQueryData(get1QueryKey({ path: { id: "application-1" } }))).toMatchObject({ id: "application-1" })
    expect(sessionStorage.getItem(submissionStorageKey(42, "period-1"))).toBeNull()
  })

  it("reutiliza la clave luego de un error para no crear una segunda solicitud", async () => {
    respond = async () => Response.json({ status: 500, message: "No pudimos confirmar la presentación." }, { status: 500 })
    const user = userEvent.setup()
    renderConfirmation()
    await user.click(screen.getByRole("button", { name: "Presentar solicitud" }))
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("No se pudo completar la operación", {
      description: "No pudimos confirmar la presentación.",
    }))
    expect(screen.queryByText("No pudimos confirmar la presentación.")).not.toBeInTheDocument()
    const key = requests[0].headers.get("Idempotency-Key")
    expect(sessionStorage.getItem(submissionStorageKey(42, "period-1"))).toBe(key)
    respond = async () => Response.json({ id: "application-1" }, { status: 200 })
    await user.click(screen.getByRole("button", { name: "Presentar solicitud" }))
    await waitFor(() => expect(navigate).toHaveBeenCalledTimes(1))
    expect(requests).toHaveLength(2)
    expect(requests[1].headers.get("Idempotency-Key")).toBe(key)
  })
})
