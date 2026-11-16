import { IconAlertTriangle, IconList } from "@tabler/icons-react"
import { createFileRoute } from "@tanstack/react-router"

import { CoreContract } from "@/components/core-events/CoreContract"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { SidebarTrigger } from "@/components/ui/sidebar"

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
    <div className="relative h-full w-full">
      <iframe title="Documentación de eventos async" src={ASYNCAPI_UI_URL} className="h-full w-full" />
      <div className="absolute top-3 left-3 z-50 flex items-center gap-2">
        <SidebarTrigger variant="secondary" />
        <Sheet>
          <SheetTrigger render={<Button size="sm" variant="secondary" />}>
            <IconList />
            Contrato con Core
          </SheetTrigger>
          <SheetContent className="w-full gap-0 data-[side=right]:sm:max-w-3xl">
            <SheetHeader className="border-b">
              <SheetTitle className="pr-8">Contrato con Core</SheetTitle>
              <SheetDescription>Lo que declaramos publicar y los eventos a los que estamos suscriptos, según Core.</SheetDescription>
            </SheetHeader>
            <div className="min-h-0 flex-1 overflow-y-auto p-4">
              {canViewCore ? <CoreContract /> : (
                <Alert>
                  <IconAlertTriangle />
                  <AlertTitle>Sin acceso</AlertTitle>
                  <AlertDescription>Tu rol activo no tiene el permiso {CORE_VIEW_PERMISSION} para consultar el contrato con Core.</AlertDescription>
                </Alert>
              )}
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </div>
  )
}
