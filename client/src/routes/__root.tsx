import { createRootRoute, Outlet } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'
import { AppLoadingBar } from '@/components/AppLoadingBar'

export const Route = createRootRoute({
  component: () => (
    <>
      <AppLoadingBar />
      <Outlet />
      <TanStackRouterDevtools />
    </>
  ),
})