import { Suspense, type ReactNode } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { act, cleanup, render, screen, waitFor } from "@testing-library/react"
import { afterEach, expect, it, vi } from "vitest"
import { get2Options } from "@/generated/@tanstack/react-query.gen"
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  createFileRoute: () => (options: { component: () => ReactNode }) => ({ options,
    useParams: () => ({ solicitudId: "550e8400-e29b-41d4-a716-446655440000" }) }),
}))
vi.mock("@/hooks/use-auth", () => ({ useMe: () => ({ data: { user: { permissions: [] } } }) }))
vi.mock("@/components/applications/ApplicationDocuments", () => ({ ApplicationDocuments: () => null }))
vi.mock("@/components/applications/ApplicationUi", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/components/applications/ApplicationUi")>(),
  ApplicationPage: ({ children }: { children: ReactNode }) => <main>{children}</main>,
}))
const initial = { applicationNumber: 101, status: "SUBMITTED", submittedAt: "2026-10-20T12:00:00Z", updatedAt: "2026-10-21T12:00:00Z", pendingDocuments: [] }
vi.mock("@/generated/@tanstack/react-query.gen", () => ({ get2Options: () => ({ queryKey: ["qa-own-application"], queryFn: async () => initial }) }))
import { Route } from "@/routes/_app/portal/solicitudes/$solicitudId/index"
afterEach(cleanup)
it("muestra estado y fecha actualizados cuando se recibe la nueva solicitud", async () => {
  const Component = Route.options.component!
  await (Component as typeof Component & { preload?: () => Promise<unknown> }).preload?.()
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><Suspense fallback={<p>Cargando ruta</p>}><Component /></Suspense></QueryClientProvider>)
  const label = await screen.findByText("Última actualización")
  expect(label.parentElement).toHaveTextContent(/21.*oct.*2026/)
  await act(async () => { client.setQueryData(get2Options({ path: { id: "550e8400-e29b-41d4-a716-446655440000" } }).queryKey, { ...initial, status: "APPROVED", updatedAt: "2026-11-02T12:00:00Z" }) })
  await waitFor(() => expect(label.parentElement).toHaveTextContent(/2.*nov.*2026/))
  expect(screen.getByText("Tu solicitud fue aprobada")).toBeInTheDocument()
  expect(label.parentElement).not.toHaveTextContent(/21.*oct/)
}, 30000)
