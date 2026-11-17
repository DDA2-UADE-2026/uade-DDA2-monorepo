import { fireEvent, render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { AuditDiffSheet } from "@/components/audit/AuditDiffSheet"
import auditDiffSheetSource from "@/components/audit/AuditDiffSheet.tsx?raw"
import type { LogResponse } from "@/generated/types.gen"

function renderAndOpenDiff(
  log: Pick<LogResponse, "entityId" | "oldValues" | "newValues">,
  details?: string,
) {
  render(
    <AuditDiffSheet
      log={log}
      details={details ? <p data-testid="audit-details">{details}</p> : undefined}
    />,
  )

  fireEvent.click(screen.getByRole("button", { name: "Ver diff" }))
}

function getDiffLineElements(): HTMLElement[] {
  const pre = document.body.querySelector("pre")
  expect(pre).not.toBeNull()
  return Array.from(pre!.querySelectorAll("span"))
}

describe("AuditDiffSheet", () => {
  it("muestra claves sin cambios, agregadas, eliminadas y modificadas en objetos anidados con claves desordenadas", async () => {
    const oldValues = JSON.stringify({
      nombre: "Programa Integral",
      estado: "BORRADOR",
      obsoleto: "quitar",
      config: {
        cupo: 50,
        vigencia: "2026",
        nota: "inicial",
      },
    })
    const newValues = JSON.stringify({
      config: {
        vigencia: "2027",
        cupo: 50,
        activo: true,
      },
      estado: "ACTIVO",
      nombre: "Programa Integral",
      nuevoCampo: "valor",
    })

    renderAndOpenDiff(
      { entityId: "prog-42", oldValues, newValues },
      "Modificación por admin",
    )

    expect(await screen.findByText("Diferencias del cambio")).toBeInTheDocument()
    expect(screen.getByTestId("audit-details")).toHaveTextContent(
      "Modificación por admin",
    )
    expect(screen.getByText("prog-42")).toBeInTheDocument()

    const lines = getDiffLineElements()
    const texts = lines.map((el) => el.textContent)

    expect(texts).toContain('nombre: "Programa Integral",')
    expect(texts).toContain('estado: "BORRADOR",')
    expect(texts).toContain('estado: "ACTIVO",')
    expect(texts).toContain('obsoleto: "quitar",')
    expect(texts).toContain('nuevoCampo: "valor",')
    expect(texts).toContain("cupo: 50,")
    expect(texts).toContain('vigencia: "2026",')
    expect(texts).toContain('vigencia: "2027",')
    expect(texts).toContain('nota: "inicial",')
    expect(texts).toContain("activo: true,")

    const findLine = (exact: string) =>
      lines.find((el) => el.textContent === exact)

    expect(findLine('nombre: "Programa Integral",')).toHaveClass(
      "text-muted-foreground",
    )
    expect(findLine('estado: "BORRADOR",')).toHaveClass("line-through")
    expect(findLine('estado: "ACTIVO",')).toHaveClass("bg-emerald-500/15")
    expect(findLine('obsoleto: "quitar",')).toHaveClass("line-through")
    expect(findLine('nuevoCampo: "valor",')).toHaveClass("bg-emerald-500/15")
    expect(findLine('nota: "inicial",')).toHaveClass("line-through")
    expect(findLine("activo: true,")).toHaveClass("bg-emerald-500/15")
  })

  it("muestra cambios en arrays (movimientos, inserciones y eliminaciones) como reemplazo estructural completo", async () => {
    const oldValues = JSON.stringify({
      permisos: ["USERS_VIEW", "ROLES_VIEW", "AUDIT_VIEW"],
      orden: [1, 2, 3],
    })
    const newValues = JSON.stringify({
      permisos: ["AUDIT_VIEW", "USERS_VIEW", "PROGRAMS_EDIT"],
      orden: [3, 1, 2],
    })

    renderAndOpenDiff({ entityId: "role-9", oldValues, newValues })
    await screen.findByText("Diferencias del cambio")

    const pre = document.body.querySelector("pre")
    expect(pre).not.toBeNull()
    const text = pre!.textContent ?? ""

    expect(text).toContain("permisos: [")
    expect(text).toContain('"ROLES_VIEW"')
    expect(text).toContain('"PROGRAMS_EDIT"')
    expect(text).toContain("orden: [")

    const lines = getDiffLineElements()
    const removedLines = lines.filter((el) =>
      el.className.includes("line-through"),
    )
    const addedLines = lines.filter((el) =>
      el.className.includes("bg-emerald-500/15"),
    )

    expect(removedLines.some((el) => el.textContent?.includes('"ROLES_VIEW"'))).toBe(
      true,
    )
    expect(addedLines.some((el) => el.textContent?.includes('"PROGRAMS_EDIT"'))).toBe(
      true,
    )
    expect(removedLines.some((el) => el.textContent?.startsWith("orden: ["))).toBe(
      true,
    )
    expect(addedLines.some((el) => el.textContent?.startsWith("orden: ["))).toBe(
      true,
    )
  })

  it("informa cuando no hay diferencias en objetos equivalentes o valores vacíos", async () => {
    const first = render(
      <AuditDiffSheet
        log={{
          entityId: "ent-eq",
          oldValues: JSON.stringify({ a: 1, b: [1, 2] }),
          newValues: JSON.stringify({ b: [1, 2], a: 1 }),
        }}
      />,
    )

    fireEvent.click(screen.getByRole("button", { name: "Ver diff" }))
    expect(
      await screen.findByText("No hay diferencias entre los valores registrados."),
    ).toBeInTheDocument()
    expect(document.body.querySelector("pre")).toBeNull()
    first.unmount()

    render(
      <AuditDiffSheet
        log={{
          entityId: undefined,
          oldValues: "",
          newValues: undefined,
        }}
      />,
    )

    fireEvent.click(screen.getByRole("button", { name: "Ver diff" }))
    expect(
      await screen.findByText("No hay diferencias entre los valores registrados."),
    ).toBeInTheDocument()
    expect(screen.getByText("—")).toBeInTheDocument()
  })

  it("tolera strings que no son JSON válido y strings largos sin lanzar errores", async () => {
    const oldText =
      "Observación extendida del expediente municipal número 1042-A correspondiente al período previo de evaluación social."
    const newText =
      "Observación extendida del expediente municipal número 1042-B correspondiente al período actualizado de evaluación social."

    const first = render(
      <AuditDiffSheet
        log={{
          entityId: "log-raw",
          oldValues: "{json-roto-anterior",
          newValues: "{json-roto-actual",
        }}
      />,
    )

    fireEvent.click(screen.getByRole("button", { name: "Ver diff" }))
    await screen.findByText("Diferencias del cambio")
    expect(screen.getByText('"{json-roto-anterior"')).toHaveClass("line-through")
    expect(screen.getByText('"{json-roto-actual"')).toHaveClass("bg-emerald-500/15")
    first.unmount()

    render(
      <AuditDiffSheet
        log={{
          entityId: "log-long",
          oldValues: JSON.stringify({ detalle: oldText }),
          newValues: JSON.stringify({ detalle: newText }),
        }}
      />,
    )

    fireEvent.click(screen.getByRole("button", { name: "Ver diff" }))
    await screen.findByText("Diferencias del cambio")
    expect(screen.getByText(`detalle: ${JSON.stringify(oldText)},`)).toHaveClass(
      "line-through",
    )
    expect(screen.getByText(`detalle: ${JSON.stringify(newText)},`)).toHaveClass(
      "bg-emerald-500/15",
    )
  })

  it("renderiza payloads HTML/XSS como texto literal sin formatter HTML ni inyección en el DOM", async () => {
    const xssOld = [
      "<script>window.__xss=1</script>",
      "seguro",
      "<img src=x onerror=\"alert(1)\">",
    ]
    const xssNew = [
      "<img src=x onerror=\"alert(1)\">",
      "<svg onload=alert(2)>",
      "<script>window.__xss=1</script>",
    ]

    renderAndOpenDiff({
      entityId: "xss-1",
      oldValues: JSON.stringify({ items: xssOld, "<b onclick=alert(3)>k</b>": "a" }),
      newValues: JSON.stringify({ items: xssNew, "<b onclick=alert(3)>k</b>": "b" }),
    })

    await screen.findByText("Diferencias del cambio")
    const pre = document.body.querySelector("pre")
    expect(pre).not.toBeNull()

    expect(pre!.textContent).toContain('<script>window.__xss=1</script>')
    expect(pre!.textContent).toContain('<img src=x onerror=\\"alert(1)\\">')
    expect(pre!.textContent).toContain("<svg onload=alert(2)>")
    expect(pre!.textContent).toContain("<b onclick=alert(3)>k</b>:")

    expect(document.body.querySelectorAll("script")).toHaveLength(0)
    expect(document.body.querySelectorAll("img")).toHaveLength(0)
    expect(pre!.querySelectorAll("svg, b, [onerror], [onload], [onclick]")).toHaveLength(0)

    expect(auditDiffSheetSource).not.toContain("formatters/html")
    expect(auditDiffSheetSource).not.toContain("formatters/annotated")
    expect(auditDiffSheetSource).not.toContain("dangerouslySetInnerHTML")
  })
})
