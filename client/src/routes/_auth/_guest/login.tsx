import { createFileRoute } from '@tanstack/react-router'

import { LoginForm } from '@/components/auth/LoginForm'

export const Route = createFileRoute('/_auth/_guest/login')({
  component: RouteComponent,
})

function RouteComponent() {
  return <LoginForm />
}
