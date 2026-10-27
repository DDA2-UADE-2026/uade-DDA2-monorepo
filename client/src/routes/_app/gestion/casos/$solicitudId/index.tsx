import { createFileRoute, redirect } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/casos/$solicitudId/")({
  beforeLoad: ({ params }) => { throw redirect({ to: "/gestion/casos/$solicitudId/evaluacion", params }) },
})
