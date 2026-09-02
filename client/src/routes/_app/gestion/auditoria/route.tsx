import { Outlet, createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/auditoria")({
  component: Outlet,
})
