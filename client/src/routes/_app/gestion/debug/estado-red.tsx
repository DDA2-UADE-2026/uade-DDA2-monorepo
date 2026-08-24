import { createFileRoute } from "@tanstack/react-router"
import { IconCircleCheck, IconCircleX, IconLoader2 } from "@tabler/icons-react"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { PageHeader } from "@/components/layout/PageHeader"
import { useBackendHealth } from "@/hooks/use-backend-health"

export const Route = createFileRoute("/_app/gestion/debug/estado-red")({
  component: RouteComponent,
})

const SERVER_URL = import.meta.env.VITE_SERVER_URL

// TODO: hoy solo chequea el backend principal. Sumar acá el resto de los
// módulos/servicios a medida que existan.
function RouteComponent() {
  const { data, isPending, isError, error } = useBackendHealth()

  return (
    <div className="flex h-full w-full flex-col">
      <PageHeader />
      <Card className="max-w-sm m-6">
        <CardHeader>
          <CardTitle>Backend</CardTitle>
          <CardDescription>{SERVER_URL}/actuator/health</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center gap-2">
          {isPending ? (
            <>
              <IconLoader2 className="size-4 animate-spin text-muted-foreground" />
              <span className="text-sm text-muted-foreground">Consultando…</span>
            </>
          ) : isError ? (
            <>
              <IconCircleX className="size-4 text-destructive" />
              <span className="text-sm text-destructive">
                {error instanceof Error ? error.message : "Sin conexión"}
              </span>
            </>
          ) : (
            <>
              <IconCircleCheck className="size-4 text-emerald-500" />
              <span className="text-sm">{String(data.status ?? "UP")}</span>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
