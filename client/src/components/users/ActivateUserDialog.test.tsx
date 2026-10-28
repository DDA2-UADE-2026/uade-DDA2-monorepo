import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, expect, it, vi } from "vitest"

import { ActivateUserDialog } from "@/components/users/ActivateUserDialog"
import { client } from "@/generated/client.gen"

const originalConfig = client.getConfig()
afterEach(() => { client.setConfig(originalConfig); vi.restoreAllMocks() })

it("habilita con los roles y datos actuales obtenidos del servidor", async () => {
  const requests: { method: string; body?: unknown }[] = []
  client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async (input) => {
    const request = input as Request
    requests.push({ method: request.method, body: request.method === "PUT" ? await request.json() : undefined })
    return Response.json({ id: 7, name: "Nombre actualizado", email: "actual@example.com", active: request.method === "PUT", roles: ["CIUDADANO", "OPERADOR"] })
  }) })
  const onClose = vi.fn()
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={queryClient}><ActivateUserDialog user={{ id: 7, name: "Nombre anterior", active: false, roles: ["CIUDADANO"] }} onClose={onClose} /></QueryClientProvider>)
  await userEvent.setup().click(screen.getByRole("button", { name: "Habilitar usuario" }))
  await waitFor(() => expect(onClose).toHaveBeenCalled())
  expect(requests).toEqual([{ method: "GET", body: undefined }, { method: "PUT", body: { name: "Nombre actualizado", email: "actual@example.com", roles: ["CIUDADANO", "OPERADOR"], active: true } }])
})
