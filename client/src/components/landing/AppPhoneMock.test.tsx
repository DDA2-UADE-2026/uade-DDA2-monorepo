import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, expect, it, vi } from "vitest"

import { apiHarness } from "@/test/api-harness"
import type { AvailableProgramListItemResponse } from "@/generated/types.gen"
import { AppPhoneMock } from "./AppPhoneMock"

const auth = vi.hoisted(() => ({ name: "Delfina Brazenas" }))
vi.mock("@/hooks/use-auth", () => ({ useMe: () => ({ data: { user: { name: auth.name } } }) }))

let api: ReturnType<typeof apiHarness>
let programs: AvailableProgramListItemResponse[]
let totalPrograms: number
let failOnce: boolean

beforeEach(() => {
  auth.name = "Delfina Brazenas"
  programs = []
  totalPrograms = 0
  failOnce = false
  api = apiHarness(() => {
    if (failOnce) {
      failOnce = false
      return Response.json({ message: "Servicio no disponible" }, { status: 503 })
    }
    return Response.json({ content: programs, totalElements: totalPrograms, totalPages: Math.ceil(totalPrograms / 3), page: 0, size: 3 })
  })
})

afterEach(() => api.restore())

it("muestra el nombre, el total y hasta tres programas disponibles", async () => {
  totalPrograms = 8
  programs = [
    { id: "p-1", name: "Apoyo alimentario", availableEditions: 1, nextEditionStartDate: "2099-03-15" },
    { id: "p-2", name: "Acompañamiento familiar", availableEditions: 2 },
    { id: "p-3", name: "Personas mayores", availableEditions: 0 },
    { id: "p-4", name: "No debe aparecer" },
  ]
  api.render(<AppPhoneMock />)

  expect(await screen.findByText("Hola, Delfina")).toBeInTheDocument()
  expect(await screen.findByText("Apoyo alimentario")).toBeInTheDocument()
  expect(screen.getByText("8 programas")).toBeInTheDocument()
  expect(screen.getByText("Ver todos")).toBeInTheDocument()
  expect(screen.getByText("Acompañamiento familiar")).toBeInTheDocument()
  expect(screen.getByText("Personas mayores")).toBeInTheDocument()
  expect(screen.queryByText("No debe aparecer")).not.toBeInTheDocument()
  expect(screen.getByText("1 edición")).toBeInTheDocument()
  expect(screen.getByText("2 ediciones")).toBeInTheDocument()
})

it("muestra el estado vacío y un nombre alternativo cuando falta el usuario", async () => {
  auth.name = ""
  api.render(<AppPhoneMock />)

  expect(await screen.findByText("Hola, Sofía")).toBeInTheDocument()
  expect(await screen.findByText("No hay programas disponibles por el momento.")).toBeInTheDocument()
  expect(screen.getByText("0 programas")).toBeInTheDocument()
})

it("informa el error y recupera la lista al reintentar", async () => {
  failOnce = true
  programs = [{ id: "p-1", name: "Programa recuperado", availableEditions: 1 }]
  totalPrograms = 1
  api.render(<AppPhoneMock />)

  expect(await screen.findByText("No pudimos cargar los programas.")).toBeInTheDocument()
  await screen.findByText("Reintentar")
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }))
  expect(await screen.findByText("Programa recuperado")).toBeInTheDocument()
  expect(api.requests).toHaveLength(2)
})
