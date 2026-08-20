import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import type { QueryClient } from '@tanstack/react-query'
import { AppLoadingBar } from '@/components/AppLoadingBar'
import { Toaster } from 'sonner'
import { lazy, Suspense, type JSX, createElement } from 'react'

let CommandMenu: React.LazyExoticComponent<() => JSX.Element> | null = null
if (import.meta.env.DEV) {
  CommandMenu = lazy(() => import('@/components/AppCommandShortcut').then(module => ({
    default: () => createElement(module.CommandMenu || module)
  })))
}

interface RouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<RouterContext>()({
  component: () => (
    <>
      <AppLoadingBar />
      {CommandMenu && (
        <Suspense fallback={null}>
          <CommandMenu />
        </Suspense>
      )}
      <Outlet />
      <TanStackRouterDevtools />
      <ReactQueryDevtools />
      <Toaster />
    </>
  ),
})