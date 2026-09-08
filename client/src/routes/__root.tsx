import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { TanStackDevtools } from "@tanstack/react-devtools"
import { TanStackRouterDevtoolsPanel } from '@tanstack/react-router-devtools'
import { ReactQueryDevtoolsPanel } from '@tanstack/react-query-devtools'
import { FormDevtoolsPanel } from "@tanstack/react-form-devtools"
import type { QueryClient } from '@tanstack/react-query'
import { AppLoadingBar } from '@/components/AppLoadingBar'
import { RouteErrorPage } from '@/components/errors/RouteErrorPage'
import { Toaster } from '@/components/ui/sonner'
import { Fragment, Suspense, lazy } from 'react'
const AppCommandShortcut = lazy(() => import('@/components/AppCommandShortcut'))

interface RouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<RouterContext>()({
  errorComponent: RouteErrorPage,
  component: () => (
    <Fragment>
      <AppLoadingBar />
      <Suspense fallback={<></>}>
        <AppCommandShortcut />
      </Suspense>
      <Outlet />
      <TanStackDevtools
        plugins={[
          {
            name: 'TanStack Query',
            render: <ReactQueryDevtoolsPanel />,
          },
          {
            name: 'TanStack Router',
            render: <TanStackRouterDevtoolsPanel />,
          },
          {
            name: 'TanStack Form',
            render: <FormDevtoolsPanel />,
          },
        ]}
      />
      <Toaster />
    </Fragment>
  ),
})
