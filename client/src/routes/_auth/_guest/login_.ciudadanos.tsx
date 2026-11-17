import { createFileRoute } from '@tanstack/react-router'

import { CitizenLoginForm } from '@/components/auth/CitizenLoginForm'

export const Route = createFileRoute('/_auth/_guest/login_/ciudadanos')({
  component: RouteComponent,
})

function RouteComponent() {
  return <CitizenLoginForm />
}
