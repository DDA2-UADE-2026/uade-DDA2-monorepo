import type { ReactNode } from "react"
import { Suspense } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, cleanup } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it, vi } from "vitest"

const state = vi.hoisted(() => ({ page: 1, navigate: vi.fn() }))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  createFileRoute: () => (options: { component: () => ReactNode }) => ({
    options, fullPath: "/portal/intervenciones", useSearch: () => ({ page: state.page }),
  }),
  useNavigate: () => state.navigate,
  Link: ({ children }: { children: ReactNode }) => <span>{children}</span>,
}))
vi.mock("@/generated/@tanstack/react-query.gen", () => ({
  listOwnInterventionPlansOptions: () => ({
    queryKey: ["own-plans", state.page],
    queryFn: async () => ({ content: [], totalPages: state.page > 1 ? 1 : 0, totalElements: state.page > 1 ? 1 : 0 }),
  }),
}))
vi.mock("@/components/interventions/InterventionUi", () => ({
  InterventionPage: ({ children }: { children: ReactNode }) => <main>{children}</main>,
  InterventionHeading: () => <h1>Mis intervenciones</h1>,
  InterventionLoading: () => <p>Cargando</p>,
  InterventionError: () => <p>Error</p>,
  InterventionStatusBadge: () => null,
  formatInterventionDateTime: (s: string) => s,
}))
import { Route } from "@/routes/_app/portal/intervenciones/index"

afterEach(() => { cleanup(); state.page = 1; vi.clearAllMocks() })
async function show() {
  const Component = Route.options.component!
  await (Component as typeof Component & { preload?: () => Promise<unknown> }).preload?.()
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={queryClient}><Suspense fallback={<p>Cargando ruta</p>}><Component /></Suspense></QueryClientProvider>)
}
describe("planes propios sin resultados", () => {
  it("explica la ausencia de planes sin ofrecer volver a una página anterior", async () => {
    await show()
    expect(await screen.findByText("Todavía no tenés planes de intervención")).toBeInTheDocument()
    expect(screen.getByText(/Cuando el equipo profesional registre un plan/)).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Primera página" })).not.toBeInTheDocument()
  }, 30000)
  it("distingue página vacía y permite regresar al inicio", async () => {
    state.page = 2
    await show()
    expect(await screen.findByText("No hay planes en esta página")).toBeInTheDocument()
    expect(screen.queryByText("Todavía no tenés planes de intervención")).not.toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole("button", { name: "Primera página" }))
    expect(state.navigate).toHaveBeenCalledWith({ search: { page: 1 } })
  }, 30000)
})
