import { createFileRoute, Outlet } from '@tanstack/react-router'

import { redirectAuthenticatedUser } from '@/lib/auth-route-guards'

export const Route = createFileRoute('/_auth/_guest')({
  beforeLoad: ({ context }) => redirectAuthenticatedUser(context.queryClient),
  component: Outlet,
})
