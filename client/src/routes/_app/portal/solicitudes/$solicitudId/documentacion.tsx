import { createFileRoute, redirect } from "@tanstack/react-router"

export const Route = createFileRoute(
  "/_app/portal/solicitudes/$solicitudId/documentacion",
)({
  beforeLoad: ({ params }) => {
    throw redirect({ to: "/portal/solicitudes/$solicitudId", params, hash: "documentacion", replace: true })
  },
})
