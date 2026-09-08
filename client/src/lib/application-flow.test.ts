import { afterEach, describe, expect, it } from "vitest"
import { applicationDocumentRequirements, canApplyToPeriod, clearSubmissionKey, enrollmentToday, getSubmissionKey, MAX_DOCUMENT_BYTES, validateApplicationFile } from "./application-flow"

afterEach(() => sessionStorage.clear())

describe("inscripción a programas", () => {
  const edition = { status: "ACTIVE" as const }
  const period = { id: "period", status: "OPEN" as const, openDate: "2026-09-01", closeDate: "2026-09-07" }

  it("usa la fecha de Buenos Aires alrededor de medianoche UTC", () => {
    expect(enrollmentToday(new Date("2026-09-08T02:59:59Z"))).toBe("2026-09-07")
    expect(enrollmentToday(new Date("2026-09-08T03:00:00Z"))).toBe("2026-09-08")
  })

  it("incluye ambos extremos del período y rechaza fechas fuera de vigencia", () => {
    expect(canApplyToPeriod(edition, period, "2026-09-01")).toBe(true)
    expect(canApplyToPeriod(edition, period, "2026-09-07")).toBe(true)
    expect(canApplyToPeriod(edition, period, "2026-08-31")).toBe(false)
    expect(canApplyToPeriod(edition, period, "2026-09-08")).toBe(false)
  })

  it("exige una edición activa y un período abierto con fechas completas", () => {
    expect(canApplyToPeriod({ status: "CLOSED" }, period, "2026-09-05")).toBe(false)
    expect(canApplyToPeriod(edition, { ...period, status: "SUSPENDED" }, "2026-09-05")).toBe(false)
    expect(canApplyToPeriod(edition, { ...period, openDate: undefined }, "2026-09-05")).toBe(false)
  })

  it("conserva la clave al reintentar y la separa por ciudadano y período", () => {
    const key = getSubmissionKey(1, "period-1")
    expect(getSubmissionKey(1, "period-1")).toBe(key)
    expect(getSubmissionKey(2, "period-1")).not.toBe(key)
    expect(getSubmissionKey(1, "period-2")).not.toBe(key)
    clearSubmissionKey(1, "period-1")
    expect(getSubmissionKey(1, "period-1")).not.toBe(key)
  })
})

describe("documentación de solicitudes", () => {
  it("acepta los formatos permitidos y el límite de 10 MiB", () => {
    for (const [name, type] of [["DNI.PDF", "application/pdf"], ["dni.jpg", "image/jpeg"], ["dni.jpeg", "image/jpeg"], ["dni.png", "image/png"]]) {
      expect(validateApplicationFile({ name, type, size: MAX_DOCUMENT_BYTES })).toBeUndefined()
    }
  })

  it("rechaza archivos vacíos, demasiado grandes o con formato discordante", () => {
    expect(validateApplicationFile({ name: "dni.pdf", type: "application/pdf", size: 0 })).toMatch(/vacío/)
    expect(validateApplicationFile({ name: "dni.pdf", type: "application/pdf", size: MAX_DOCUMENT_BYTES + 1 })).toMatch(/10 MB/)
    expect(validateApplicationFile({ name: "dni.pdf", type: "image/png", size: 100 })).toMatch(/formato/)
    expect(validateApplicationFile({ name: "dni.exe", type: "application/pdf", size: 100 })).toMatch(/formato/)
  })

  it("conserva documentos opcionales sin entrega y evita duplicar los pendientes", () => {
    const required = { id: "dni", name: "DNI", required: true }
    const optional = { id: "extra", name: "Información adicional", required: false }
    expect(applicationDocumentRequirements({
      documentRequirements: [required, optional],
      pendingDocuments: [{ requirementId: "dni", name: "DNI" }],
    }, [{ requirementId: "dni", requirementName: "DNI" }])).toEqual([required, optional])
  })
})
