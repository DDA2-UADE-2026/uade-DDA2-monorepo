import {
  IconCalendarEvent,
  IconLock,
  IconMapPin,
  IconPencil,
  IconPlus,
  IconRefresh,
  IconSpeakerphone,
  IconUsers,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { useState } from "react"

import { ActivityDialog } from "@/components/activities/ActivityDialog"
import { ActivityStatusDialog, type ActivityStatusAction } from "@/components/activities/ActivityStatusDialog"
import {
  ActivityStatusBadge,
  PAGE_SIZE,
  activitySearchSchema,
  canCloseActivity,
  canEditActivity,
  canPublishActivity,
  FormatActivityDateRange,
} from "@/components/activities/activity-ui"
import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { list5Options } from "@/generated/@tanstack/react-query.gen"
import type { ActivityListItemResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/gestion/actividades/")({
  validateSearch: activitySearchSchema,
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [editing, setEditing] = useState<{ id?: string } | null>(null)
  const [statusChange, setStatusChange] = useState<{
    activity: ActivityListItemResponse
    action: ActivityStatusAction
  } | null>(null)

  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    list5Options({ query: { page: page - 1, size: PAGE_SIZE } }),
  )
  const activities = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Actividades" }]} />
        <OutletNavRightButton>
          <Button size="sm" onClick={() => setEditing({})}>
            <IconPlus />
            Nueva actividad
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-2 sm:mx-4! lg:py-4">
            {dataUpdatedAt > 0 && (
              <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                <span>Última actualización: {new Date(dataUpdatedAt).toLocaleTimeString("es-AR")}</span>
                <Button size="xs" variant="ghost" onClick={() => refetch()} disabled={isFetching}>
                  <IconRefresh className={isFetching ? "animate-spin" : undefined} />
                  Actualizar
                </Button>
              </div>
            )}

            {isPending ? (
              <p className="text-sm text-muted-foreground">Cargando actividades…</p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar las actividades.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>Reintentar</Button>
              </div>
            ) : activities.length === 0 ? (
              <Empty className="min-h-72 border">
                <EmptyHeader>
                  <EmptyMedia variant="icon">
                    <IconSpeakerphone />
                  </EmptyMedia>
                  <EmptyTitle>
                    {page > 1 ? "No hay actividades en esta página" : "Todavía no hay actividades"}
                  </EmptyTitle>
                  <EmptyDescription>
                    {page > 1
                      ? "Volvé a la primera página para ver las actividades registradas."
                      : "Creá una actividad comunitaria; queda en borrador hasta que la publiques."}
                  </EmptyDescription>
                </EmptyHeader>
                {page > 1 ? (
                  <Button variant="outline" onClick={() => navigate({ search: { page: 1 } })}>
                    Primera página
                  </Button>
                ) : (
                  <Button variant="outline" onClick={() => setEditing({})}>
                    <IconPlus />
                    Nueva actividad
                  </Button>
                )}
              </Empty>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nombre</TableHead>
                    <TableHead>Lugar</TableHead>
                    <TableHead>Fechas</TableHead>
                    <TableHead>Cupo</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {activities.map((activity) => (
                    <TableRow key={activity.id}>
                      <TableCell className="font-medium">
                        <Link
                          to="/gestion/actividades/$actividadId"
                          params={{ actividadId: activity.id ?? "" }}
                          className="hover:underline"
                        >
                          {activity.name || "Sin nombre"}
                        </Link>
                      </TableCell>
                      <TableCell className="max-w-xs whitespace-normal text-muted-foreground">
                        <span className="flex items-start gap-1.5">
                          <IconMapPin className="mt-0.5 size-3.5 shrink-0" />
                          {activity.location || "—"}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        <span className="flex items-center gap-1.5">
                          <IconCalendarEvent className="size-3.5 shrink-0" />
                          <FormatActivityDateRange startDate={activity.startDate} endDate={activity.endDate} />
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        <span className="flex items-center gap-1.5">
                          <IconUsers className="size-3.5 shrink-0" />
                          {activity.capacity ?? "—"}
                        </span>
                      </TableCell>
                      <TableCell><ActivityStatusBadge status={activity.status} /></TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-1">
                          {canEditActivity(activity.status) && (
                            <Button
                              type="button"
                              size="icon-sm"
                              variant="ghost"
                              aria-label={`Editar actividad ${activity.name ?? ""}`}
                              onClick={() => setEditing({ id: activity.id })}
                            >
                              <IconPencil />
                            </Button>
                          )}
                          {canPublishActivity(activity.status) && (
                            <Button
                              type="button"
                              size="icon-sm"
                              variant="ghost"
                              aria-label={`Publicar actividad ${activity.name ?? ""}`}
                              onClick={() => setStatusChange({ activity, action: "publish" })}
                            >
                              <IconSpeakerphone />
                            </Button>
                          )}
                          {canCloseActivity(activity.status) && (
                            <Button
                              type="button"
                              size="icon-sm"
                              variant="ghost"
                              aria-label={`Cerrar actividad ${activity.name ?? ""}`}
                              onClick={() => setStatusChange({ activity, action: "close" })}
                            >
                              <IconLock />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>

        {!isPending && !isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={(nextPage) => navigate({ search: { page: nextPage } })}
          />
        )}
      </SidebarShellContent>

      {editing && (
        <ActivityDialog
          activityId={editing.id}
          onOpenChange={(open) => { if (!open) setEditing(null) }}
        />
      )}
      {statusChange && (
        <ActivityStatusDialog
          activity={statusChange.activity}
          action={statusChange.action}
          onOpenChange={(open) => { if (!open) setStatusChange(null) }}
        />
      )}
    </SidebarShell>
  )
}
