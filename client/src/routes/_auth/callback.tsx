import { useEffect } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { IconLoader2, IconShieldCheck } from "@tabler/icons-react"

import { AuthCard } from "@/components/auth/AuthCard"

export const Route = createFileRoute("/_auth/callback")({
  component: RouteComponent,
})

// TODO: replace the fake delay with a real token exchange / session check
// once the backend flow is wired up, then decide whether seleccionar-rol is
// even needed (single-role users should skip straight to their app).
const REDIRECT_DELAY_MS = 1200

function RouteComponent() {
  const navigate = useNavigate()

  useEffect(() => {
    const timeout = setTimeout(() => {
      navigate({ to: "/seleccionar-rol" })
    }, REDIRECT_DELAY_MS)
    return () => clearTimeout(timeout)
  }, [navigate])

  return (
    <AuthCard
      title="Verificando tu sesión"
      description="Te estamos redirigiendo, un momento…"
      icon={IconShieldCheck}
    >
      <div className="flex flex-col items-center gap-3 py-2">
        <IconLoader2 className="size-6 animate-spin text-muted-foreground" aria-hidden="true" />
      </div>
    </AuthCard>
  )
}
