/* eslint-disable react-refresh/only-export-components -- test-only helper, never hot-reloaded */
import { Suspense, type ComponentType, type ReactNode } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render } from "@testing-library/react"
import { vi } from "vitest"

/**
 * Helpers to render a file route's component without a real router.
 *
 * Usage in a test file:
 *   vi.mock("@tanstack/react-router", async (importOriginal) =>
 *     (await import("@/test/route-harness")).mockRouterModule(await importOriginal()))
 *   vi.mock("@/components/layout/OutletNav", async () => (await import("@/test/route-harness")).outletNavMock)
 */
export const routerState = {
  context: {} as Record<string, unknown>,
  search: {} as Record<string, unknown>,
  params: {} as Record<string, string>,
  navigate: vi.fn(),
}

export function mockRouterModule(actual: object) {
  return {
    ...actual,
    createFileRoute: (path: string) => (options: object) => ({
      options,
      fullPath: path,
      useRouteContext: () => routerState.context,
      useSearch: () => routerState.search,
      useParams: () => routerState.params,
    }),
    useNavigate: () => routerState.navigate,
    Link: ({ children, to }: { children?: ReactNode; to: string }) => <a href={to}>{children}</a>,
  }
}

const Pass = ({ children }: { children?: ReactNode }) => <div>{children}</div>

export const outletNavMock = {
  SidebarShell: Pass,
  SidebarShellContent: Pass,
  OutletNavSticky: Pass,
  OutletNavRightButton: Pass,
  OutletNavContent: Pass,
  OutletNavSidebarTrigger: () => null,
}

type LazyComponent = ComponentType & { preload?: () => Promise<void> }
type FileRoute = { options: { component: LazyComponent; validateSearch?: { parse: (input: unknown) => unknown } } }

/** Route options as built by the mocked createFileRoute. */
export const routeOptions = (route: unknown) => (route as FileRoute).options

/** With autoCodeSplitting the route component is lazy: preload it before rendering. */
export async function preloadRouteComponent(route: unknown) {
  const component = routeOptions(route).component
  await component.preload?.()
  return component
}

export function renderRouteComponent(Component: ComponentType) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <Suspense fallback={null}><Component /></Suspense>
    </QueryClientProvider>,
  )
}
