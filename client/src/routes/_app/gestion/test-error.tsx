import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/test-error")({
  loader: () => {
    throw new Error("Error de prueba disparado desde el loader de /gestion/test-error")
  },
  component: RouteComponent,
})

function RouteComponent() {
  return null
}
