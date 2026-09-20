import { describe, expect, it } from "vitest"

import { centerSearchSchema, toCenterListQuery } from "./centerFilters"

describe("centerSearchSchema", () => {
  it("aplica los valores por defecto", () => {
    expect(centerSearchSchema.parse({})).toEqual({ page: 1, search: "", estado: "todos" })
  })

  it("tolera una URL malformada", () => {
    expect(centerSearchSchema.parse({ page: "roto", estado: "desconocido" })).toEqual({
      page: 1,
      search: "",
      estado: "todos",
    })
  })
})

describe("toCenterListQuery", () => {
  it("convierte la página a base cero y recorta la búsqueda", () => {
    expect(toCenterListQuery({ page: 3, search: "  norte ", estado: "todos" })).toEqual({
      page: 2,
      size: 10,
      search: "norte",
    })
  })

  it("mapea el filtro de estado a active", () => {
    expect(toCenterListQuery({ page: 1, search: "", estado: "activos" }).active).toBe(true)
    expect(toCenterListQuery({ page: 1, search: "", estado: "inactivos" }).active).toBe(false)
    expect(toCenterListQuery({ page: 1, search: "", estado: "todos" })).not.toHaveProperty("active")
  })
})
