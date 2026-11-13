import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { expect, it, vi } from "vitest"

import { ToggleGroup, ToggleGroupItem } from "./toggle-group"

it("cambia el filtro de visitas y no permite seleccionar una opción deshabilitada", async () => {
  const onValueChange = vi.fn()
  const user = userEvent.setup()
  render(
    <ToggleGroup aria-label="Filtrar visitas" orientation="vertical" spacing={0} variant="outline" size="sm" value={["proximas"]} onValueChange={onValueChange}>
      <ToggleGroupItem value="proximas">Próximas</ToggleGroupItem>
      <ToggleGroupItem value="todas">Todas</ToggleGroupItem>
      <ToggleGroupItem value="canceladas" disabled>Canceladas</ToggleGroupItem>
    </ToggleGroup>,
  )

  const group = screen.getByRole("group", { name: "Filtrar visitas" })
  expect(group).toHaveAttribute("data-orientation", "vertical")
  expect(group).toHaveAttribute("data-spacing", "0")
  expect(screen.getByRole("button", { name: "Próximas" })).toHaveAttribute("aria-pressed", "true")
  await user.click(screen.getByRole("button", { name: "Todas" }))
  expect(onValueChange).toHaveBeenCalledWith(["todas"], expect.anything())
  expect(screen.getByRole("button", { name: "Canceladas" })).toBeDisabled()
})
