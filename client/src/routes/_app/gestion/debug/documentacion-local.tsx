import { createFileRoute } from "@tanstack/react-router"
import { SidebarTrigger } from "@/components/ui/sidebar"

export const Route = createFileRoute("/_app/gestion/debug/documentacion-local")({
  component: RouteComponent,
})

const SWAGGER_URL = `${import.meta.env.VITE_SERVER_URL}/swagger-ui/index.html`

function RouteComponent() {
  return (
    <div className="h-full w-full relative">
      <iframe title="Documentación local" src={SWAGGER_URL} className="h-full w-full" />
      <SidebarTrigger variant={"secondary"} className="absolute top-3 left-3 z-50" />
    </div>
  )
}
