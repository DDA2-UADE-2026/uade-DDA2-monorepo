import { useEffect } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { IconLoader2 } from "@tabler/icons-react"

import { AuthBrand } from "@/components/auth/AuthBrand"

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
    <div className="flex w-full flex-col items-center gap-6 text-center">
      <AuthBrand />
      <div className="flex flex-col items-center gap-3">
        <IconLoader2 className="size-6 animate-spin text-muted-foreground" aria-hidden="true" />
        <p className="text-sm text-muted-foreground">Verificando tu sesión, te estamos redirigiendo…</p>
      </div>
    </div>
  )
}
