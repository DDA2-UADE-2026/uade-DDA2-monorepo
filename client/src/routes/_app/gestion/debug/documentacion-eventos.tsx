import { IconAlertTriangle, IconExternalLink } from "@tabler/icons-react"
import { createFileRoute } from "@tanstack/react-router"

import { CoreContract } from "@/components/core-events/CoreContract"
import { SectionTitle } from "@/components/indicators/IndicatorUi"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/_app/gestion/debug/documentacion-eventos")({
  component: RouteComponent,
})

const CORE_VIEW_PERMISSION = "integrations:core:view"
// Springwolf publica el contrato AsyncAPI del backend (solo lectura, GET público).
const ASYNCAPI_UI_URL = `${import.meta.env.VITE_SERVER_URL}/springwolf/asyncapi-ui.html`

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const canViewCore = (user.permissions ?? []).includes(CORE_VIEW_PERMISSION)

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Debug" }, { label: "Eventos async" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-auto">
          <main className="mx-auto w-full max-w-7xl space-y-5 p-4 lg:p-6">
            <header>
              <h1 className="font-heading text-2xl font-medium">Eventos async</h1>
              <p className="text-sm text-muted-foreground">Contrato de mensajería de Desarrollo Social con Core sobre AMQPS.</p>
            </header>

            <SectionTitle title="Contrato con Core" description="Lo que declaramos publicar y los eventos a los que estamos suscriptos, según Core." />
            {canViewCore ? <CoreContract /> : (
              <Alert>
                <IconAlertTriangle />
                <AlertTitle>Sin acceso</AlertTitle>
                <AlertDescription>Tu rol activo no tiene el permiso {CORE_VIEW_PERMISSION} para consultar el contrato con Core.</AlertDescription>
              </Alert>
            )}

            <div className="flex flex-wrap items-end justify-between gap-3">
              <SectionTitle title="Especificación AsyncAPI" description="Canales, mensajes y esquemas generados por Springwolf a partir del código del backend." />
              <Button size="sm" variant="outline" render={<a href={ASYNCAPI_UI_URL} target="_blank" rel="noopener noreferrer" />} nativeButton={false}>
                <IconExternalLink />
                Abrir en otra pestaña
              </Button>
            </div>
            <div className="overflow-hidden rounded-xl border bg-card">
              <iframe title="Especificación AsyncAPI" src={ASYNCAPI_UI_URL} className="h-[80vh] w-full" />
            </div>
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
