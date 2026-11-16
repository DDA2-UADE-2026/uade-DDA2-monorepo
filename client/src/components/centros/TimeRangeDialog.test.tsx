import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"

import { TimeRangeDialog } from "@/components/centros/TimeRangeDialog"

function renderDialog(props: Partial<Parameters<typeof TimeRangeDialog>[0]> = {}) {
  const onSubmit = vi.fn()
  render(
    <TimeRangeDialog
      title="Editar franja"
      description="Horario de atención"
      initial={{ dayOfWeek: "TUESDAY", startTime: "09:00", endTime: "12:00" }}
      pending={false}
      error={null}
      submitLabel="Guardar"
      onSubmit={onSubmit}
      onOpenChange={vi.fn()}
      {...props}
    />,
  )
  return { onSubmit }
}

const submit = () => fireEvent.submit(document.getElementById("time-range-form")!)

describe("TimeRangeDialog", () => {
  it("envía un solo día con la lista de días sincronizada", async () => {
    const { onSubmit } = renderDialog()
    submit()
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ dayOfWeek: "TUESDAY", days: ["TUESDAY"], startTime: "09:00", endTime: "12:00" }))
  })

  it("con varios días usa el primero como día principal", async () => {
    const { onSubmit } = renderDialog({ allowMultipleDays: true })
    submit()
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ dayOfWeek: "TUESDAY", days: ["TUESDAY"] })))
  })

  it("no reenvía mientras hay un guardado pendiente y muestra el error de la API", async () => {
    const { onSubmit } = renderDialog({ pending: true, error: { message: "Se superpone con otra franja", code: "OVERLAP", status: 409, timestamp: "", path: "", fields: [{ field: "startTime", message: "Revisá el inicio" }] } })
    expect(screen.getByText("Se superpone con otra franja")).toBeInTheDocument()
    expect(screen.getByText("Revisá el inicio")).toBeInTheDocument()
    submit()
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
