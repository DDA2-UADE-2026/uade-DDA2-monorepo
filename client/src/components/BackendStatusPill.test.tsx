import { render, screen } from "@testing-library/react"
import { afterEach, expect, it, vi } from "vitest"

import { BackendStatusPill } from "./BackendStatusPill"

const health = vi.hoisted(() => ({ isPending: true, isError: false }))
vi.mock("@/hooks/use-backend-health", () => ({ useBackendHealth: () => health }))
afterEach(() => { health.isPending = true; health.isError = false })

it("expone estados de verificación, disponibilidad y variante compacta", () => {
  const { rerender } = render(<BackendStatusPill />)
  expect(screen.getByTitle("Verificando estado…")).toBeInTheDocument()

  health.isPending = false
  rerender(<BackendStatusPill xs />)
  expect(screen.getByTitle("Servicio operativo")).toHaveTextContent("Servicio operativo")
  expect(screen.getByTitle("Servicio operativo")).toHaveClass("px-1!")

  health.isError = true
  rerender(<BackendStatusPill />)
  expect(screen.getByTitle("Servicio no disponible")).toHaveTextContent("Servicio no disponible")
})
