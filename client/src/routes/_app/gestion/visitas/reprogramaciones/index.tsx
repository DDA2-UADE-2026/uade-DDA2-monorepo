import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { SocialRescheduleInbox } from "@/components/applications/SocialRescheduleInbox"
import { VisitsSectionLayout } from "@/components/applications/VisitsSectionLayout"

const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
  // "" = todos los estados.
  estado: z.enum(["PENDING", "APPROVED", "REJECTED", ""]).catch("PENDING").default("PENDING"),
})

export const Route = createFileRoute("/_app/gestion/visitas/reprogramaciones/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const { page, estado } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const permissions = user.permissions ?? []
  return <VisitsSectionLayout tab="requests" permissions={permissions}>
    <SocialRescheduleInbox
      permissions={permissions}
      page={page}
      status={estado}
      onPageChange={(next) => void navigate({ search: { estado, page: next } })}
      onStatusChange={(next) => void navigate({ search: { estado: next, page: 1 } })}
    />
  </VisitsSectionLayout>
}
