import { Outlet, createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/casos/$solicitudId")({ component: Outlet })
