import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { client } from "@/generated/client.gen"
import { socialManagement } from "./social-management"

const originalConfig = client.getConfig()
let requests: Request[]

beforeEach(() => {
  requests = []
  client.setConfig({
    baseUrl: "http://localhost:8080",
    fetch: vi.fn<typeof fetch>(async input => {
      requests.push((input as Request).clone())
      return Response.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    }),
  })
})

afterEach(() => client.setConfig(originalConfig))

describe("contrato de gestión social del PR #90", () => {
  it("consulta visitas usando los filtros y la página del backend", async () => {
    await socialManagement.visits({ applicationId: "application-1", page: 0, size: 20, status: "SCHEDULED" })
    const url = new URL(requests[0].url)
    expect(requests[0].method).toBe("GET")
    expect(url.pathname).toBe("/api/admin/application-visits")
    expect(url.searchParams.get("applicationId")).toBe("application-1")
    expect(url.searchParams.get("page")).toBe("0")
    expect(url.searchParams.get("status")).toBe("SCHEDULED")
  })

  it("envía la decisión de reprogramación a la ruta de la solicitud y visita", async () => {
    await socialManagement.reviewRequest("application-1", "visit-2", "request-3", { status: "REJECTED", comment: "El horario original ya comenzó." })
    expect(requests[0].method).toBe("PATCH")
    expect(new URL(requests[0].url).pathname).toBe("/api/applications/application-1/visits/visit-2/reschedule-requests/request-3/review")
    expect(requests[0].headers.get("Content-Type")).toContain("application/json")
    expect(await requests[0].json()).toEqual({ status: "REJECTED", comment: "El horario original ya comenzó." })
  })
})
