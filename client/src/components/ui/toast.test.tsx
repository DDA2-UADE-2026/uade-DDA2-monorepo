import { act, render, screen, waitFor } from "@testing-library/react"
import { expect, it } from "vitest"

import { Toaster, createToastManager } from "./toast"

it("presenta notificaciones con sus tipos y permite cerrarlas desde el control accesible", async () => {
  const manager = createToastManager()
  render(<Toaster toastManager={manager} />)

  let successId = ""
  act(() => {
    successId = manager.add({ title: "Guardado", description: "Los cambios se guardaron.", type: "success", timeout: 0 })
    manager.add({ title: "Información", type: "info", timeout: 0 })
    manager.add({ title: "Atención", type: "warning", timeout: 0 })
    manager.add({ title: "Error", type: "error", timeout: 0 })
    manager.add({ title: "Procesando", type: "loading", timeout: 0 })
    manager.add({ title: "Sin ícono", type: "custom", timeout: 0 })
  })

  expect(await screen.findByText("Guardado")).toBeInTheDocument()
  for (const title of ["Información", "Atención", "Error", "Procesando", "Sin ícono"]) {
    expect(screen.getByText(title)).toBeInTheDocument()
  }
  expect(screen.getByText("Los cambios se guardaron.")).toBeInTheDocument()
  expect(document.querySelectorAll('[data-slot="toast-icon"]')).toHaveLength(5)
  expect(document.querySelectorAll('[data-slot="toast-close"][aria-label="Close toast"]')).toHaveLength(6)

  act(() => manager.close(successId))
  await waitFor(() => expect(screen.queryByText("Guardado")).not.toBeInTheDocument())
})
