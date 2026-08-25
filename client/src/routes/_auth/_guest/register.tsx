import { createFileRoute } from '@tanstack/react-router'

import { RegisterForm } from '@/components/auth/RegisterForm'

export const Route = createFileRoute('/_auth/_guest/register')({
  component: RouteComponent,
})

function RouteComponent() {
  return <RegisterForm />
}
