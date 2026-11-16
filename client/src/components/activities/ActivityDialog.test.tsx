import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, expect, it, vi } from "vitest"

import { ActivityDialog } from "@/components/activities/ActivityDialog"
import { client } from "@/generated/client.gen"

const originalConfig = client.getConfig()
afterEach(() => { client.setConfig(originalConfig); vi.restoreAllMocks() })

it("permite elegir un centro y editar el lugar manualmente", async () => {
  client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async () => Response.json({ content: [{ id: "centro-1", name: "Centro Norte", address: "Calle 123" }], totalElements: 1, totalPages: 1 })) })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const user = userEvent.setup()
  render(<QueryClientProvider client={queryClient}><ActivityDialog onOpenChange={vi.fn()} /></QueryClientProvider>)
  await user.click(screen.getByRole("button", { name: "Seleccionar centro existente" }))
  const row = (await screen.findByText("Centro Norte")).closest("tr")!
  await user.click(within(row).getByRole("button", { name: "Seleccionar" }))
  const location = screen.getByLabelText("Lugar")
  expect(location).toHaveValue("Centro Norte · Calle 123")
  await user.clear(location)
  await user.type(location, "Plaza del barrio")
  expect(location).toHaveValue("Plaza del barrio")
})
