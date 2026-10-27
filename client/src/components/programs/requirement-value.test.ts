import { describe, expect, it } from "vitest"

import { formatRequirementValue, requirementValueError } from "@/components/programs/ProgramRouteUi"

describe("requirementValueError", () => {
  it("acepta los formatos que valida el backend", () => {
    expect(requirementValueError("MIN_AGE", "18")).toBeUndefined()
    expect(requirementValueError("RESIDENCY_YEARS", "0")).toBeUndefined()
    expect(requirementValueError("MAX_INCOME", "150000")).toBeUndefined()
    expect(requirementValueError("MAX_INCOME", "150000.5")).toBeUndefined()
    expect(requirementValueError("HAS_CHILDREN", "true")).toBeUndefined()
    expect(requirementValueError("HAS_CHILDREN", "false")).toBeUndefined()
  })

  it("rechaza vacíos, decimales en años, montos mal formados y booleanos inválidos", () => {
    expect(requirementValueError("MIN_AGE", "")).toBeDefined()
    expect(requirementValueError("MIN_AGE", "18.5")).toBeDefined()
    expect(requirementValueError("MAX_INCOME", "1.000.000")).toBeDefined()
    expect(requirementValueError("MAX_INCOME", "10.123")).toBeDefined()
    expect(requirementValueError("HAS_CHILDREN", "si")).toBeDefined()
  })
})

describe("formatRequirementValue", () => {
  it("muestra cada tipo en lenguaje natural", () => {
    expect(formatRequirementValue("MIN_AGE", "18")).toBe("18 años")
    expect(formatRequirementValue("RESIDENCY_YEARS", "1")).toBe("1 año")
    expect(formatRequirementValue("HAS_CHILDREN", "TRUE")).toBe("Sí")
    expect(formatRequirementValue("HAS_CHILDREN", "false")).toBe("No")
    expect(formatRequirementValue("MAX_INCOME", "150000")).toMatch(/150\.000/)
  })

  it("deja intacto un valor que no se puede interpretar", () => {
    expect(formatRequirementValue("MIN_AGE", "abc")).toBe("abc")
    expect(formatRequirementValue(undefined, "x")).toBe("x")
  })
})
