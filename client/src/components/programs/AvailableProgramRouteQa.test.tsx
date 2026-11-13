import { Suspense, type ReactNode, type ComponentProps } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { cleanup, render, screen } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"
const state = vi.hoisted(() => ({ empty: false }))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  createFileRoute: () => (options: { component: () => ReactNode }) => ({ options, fullPath: "/portal/programas", useSearch: () => ({ page: 1 }) }),
  useNavigate: () => vi.fn(),
  Link: ({ children, to, params, ...props }: ComponentProps<"a"> & { to: string; params?: Record<string, string> }) =>
    <a {...props} href={to.replace(/\$(\w+)/g, (_, key: string) => params?.[key] ?? "")}>{children}</a>,
}))
vi.mock("@/components/layout/OutletNav", () => ({
  SidebarShell: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  SidebarShellContent: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  OutletNavSticky: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  OutletNavSidebarTrigger: () => null,
}))
vi.mock("@/components/layout/OutletNavBreadcrumbs", () => ({ OutletNavBreadcrumbs: () => null }))
vi.mock("@/generated/@tanstack/react-query.gen", () => ({
  listAvailableProgramsOptions: () => ({ queryKey: ["qa-programs"], queryFn: async () => ({
    content: state.empty ? [] : [{ id: "program-qa", name: "Becas QA", objective: "Objetivo de prueba", availableEditions: 1 }],
    totalElements: state.empty ? 0 : 1, totalPages: state.empty ? 0 : 1,
  }) }),
}))
import { Route } from "@/routes/_app/portal/programas/index"
afterEach(() => { cleanup(); state.empty = false })
async function show() {
  const Component = Route.options.component!
  await (Component as typeof Component & { preload?: () => Promise<unknown> }).preload?.()
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><Suspense fallback={<p>Cargando ruta</p>}><Component /></Suspense></QueryClientProvider>)
}
describe("catálogo ciudadano QA", () => {
  it("muestra el nombre y el enlace al detalle del programa correcto", async () => {
    await show()
    expect(await screen.findByText("Becas QA")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: /Ver programa/ })).toHaveAttribute("href", "/portal/programas/program-qa")
  }, 30000)
  it("informa que no hay programas cuando la API devuelve una lista vacía", async () => {
    state.empty = true
    await show()
    expect(await screen.findByText("No hay programas disponibles")).toBeInTheDocument()
    expect(screen.getByText(/Volvé a consultar más adelante/)).toBeInTheDocument()
  }, 30000)
  // Expected failure records the missing acceptance feature; it is NOT a PASS of M8-8-CA03.
  it.fails("M8-8-CA03 exige controles para buscar programas y filtrar resultados", async () => {
    await show()
    await screen.findByText("Becas QA")
    const searchControls = [...screen.queryAllByRole("searchbox"), ...screen.queryAllByRole("textbox")]
    expect(searchControls.length).toBeGreaterThan(0)
  }, 30000)
})
