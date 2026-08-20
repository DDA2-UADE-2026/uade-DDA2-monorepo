import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import type { QueryClient } from '@tanstack/react-query'
import { AppLoadingBar } from '@/components/AppLoadingBar'
import { RouteErrorPage } from '@/components/errors/RouteErrorPage'
import { Toaster } from 'sonner'
import { Suspense, lazy } from 'react'
const AppCommandShortcut = lazy(() => import('@/components/AppCommandShortcut'))

interface RouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<RouterContext>()({
  errorComponent: RouteErrorPage,
  component: () => (
    <>
      <AppLoadingBar />
        <Suspense fallback={<></>}>
          <AppCommandShortcut />
        </Suspense>
      <Outlet />
      <TanStackRouterDevtools />
      <ReactQueryDevtools />
      <Toaster />
    </>
  ),
})