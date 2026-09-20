import {
  IconAlertTriangle,
  IconCalendarEvent,
  IconLock,
  IconMapPin,
  IconPencil,
  IconSpeakerphone,
  IconUser,
  IconUsers,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { createFileRoute, redirect } from "@tanstack/react-router"
import type { ReactNode } from "react"
import { useState } from "react"

import { ActivityDialog } from "@/components/activities/ActivityDialog"
import { ActivityStatusDialog, type ActivityStatusAction } from "@/components/activities/ActivityStatusDialog"
import {
  ActivityStatusBadge,
  type ActivityStatus,
  canCloseActivity,
  canEditActivity,
  canPublishActivity,
  formatActivityDateRange,
  formatActivityDateTime,
} from "@/components/activities/activity-ui"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { get1Options } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute("/_app/gestion/actividades/$actividadId/")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { actividadId } = Route.useParams()
  const [editing, setEditing] = useState(false)
  const [statusChange, setStatusChange] = useState<ActivityStatusAction | null>(null)
  const query = useQuery(get1Options({ path: { id: actividadId } }))
  const activity = query.data

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs
          items={[
            { label: "Actividades", to: "/gestion/actividades" },
            { label: activity?.name ?? "Actividad" },
          ]}
        />
        {activity && (
          <OutletNavRightButton>
            <ActivityStatusBadge status={activity.status} />
            {canEditActivity(activity.status) && (
              <Button size="sm" variant="outline" onClick={() => setEditing(true)}>
                <IconPencil />
                Editar
              </Button>
            )}
            {canPublishActivity(activity.status) && (
              <Button size="sm" onClick={() => setStatusChange("publish")}>
                <IconSpeakerphone />
                Publicar
              </Button>
            )}
            {canCloseActivity(activity.status) && (
              <Button size="sm" variant="outline" onClick={() => setStatusChange("close")}>
                <IconLock />
                Cerrar
              </Button>
            )}
          </OutletNavRightButton>
        )}
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full max-w-4xl space-y-6 p-4 lg:p-6">
            {query.isPending ? (
              <ActivityDetailSkeleton />
            ) : query.isError ? (
              <Alert variant="destructive">
                <IconAlertTriangle />
                <AlertTitle>
                  {query.error.status === 404
                    ? "No encontramos la actividad"
                    : "No pudimos cargar la actividad"}
                </AlertTitle>
                <AlertDescription className="flex flex-col items-start gap-3">
                  <span>{query.error.message ?? "Intentá nuevamente en unos instantes."}</span>
                  <Button size="sm" variant="outline" onClick={() => query.refetch()}>Reintentar</Button>
                </AlertDescription>
              </Alert>
            ) : activity ? (
              <>
                <header className="space-y-1">
                  <h1 className="font-heading text-2xl font-medium tracking-tight">
                    {activity.name || "Sin nombre"}
                  </h1>
                  <p className="text-sm text-muted-foreground">
                    {statusHint(activity.status)}
                  </p>
                </header>

                <Card>
                  <CardHeader className="border-b">
                    <CardTitle>Detalle</CardTitle>
                    <CardDescription>Datos de la actividad comunitaria.</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    <div className="grid gap-3 sm:grid-cols-3">
                      <DetailItem icon={<IconMapPin />} label="Lugar" value={activity.location || "—"} />
                      <DetailItem
                        icon={<IconCalendarEvent />}
                        label="Fechas"
                        value={formatActivityDateRange(activity.startDate, activity.endDate)}
                      />
                      <DetailItem
                        icon={<IconUsers />}
                        label="Cupo"
                        value={activity.capacity != null ? `${activity.capacity} personas` : "—"}
                      />
                    </div>

                    <div className="space-y-1.5">
                      <p className="text-xs font-medium text-muted-foreground">Descripción</p>
                      <p className="text-sm whitespace-pre-line">{activity.description || "—"}</p>
                    </div>
                  </CardContent>
                </Card>

                <Card size="sm">
                  <CardContent className="grid gap-3 sm:grid-cols-3">
                    <DetailItem
                      icon={<IconUser />}
                      label="Creada por"
                      value={activity.createdBy?.name || "—"}
                    />
                    <DetailItem
                      icon={<IconCalendarEvent />}
                      label="Creada"
                      value={formatActivityDateTime(activity.createdAt)}
                    />
                    <DetailItem
                      icon={<IconCalendarEvent />}
                      label="Última actualización"
                      value={formatActivityDateTime(activity.updatedAt)}
                    />
                  </CardContent>
                </Card>
              </>
            ) : null}
          </main>
        </div>
      </SidebarShellContent>

      {editing && (
        <ActivityDialog
          activityId={actividadId}
          onOpenChange={(open) => { if (!open) setEditing(false) }}
        />
      )}
      {statusChange && activity && (
        <ActivityStatusDialog
          activity={activity}
          action={statusChange}
          onOpenChange={(open) => { if (!open) setStatusChange(null) }}
        />
      )}
    </SidebarShell>
  )
}

function statusHint(status?: ActivityStatus) {
  if (status === "DRAFT") return "En borrador: todavía no es visible para la ciudadanía. Publicala para abrir las inscripciones."
  if (status === "OPEN") return "Publicada: recibe inscripciones y ya no se puede editar."
  if (status === "CLOSED") return "Cerrada: finalizó administrativamente y no admite cambios."
  return "Actividad comunitaria."
}

function DetailItem({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="flex min-w-0 items-start gap-2 rounded-lg bg-muted/60 p-3">
      <span className="mt-0.5 shrink-0 text-primary [&_svg]:size-4">{icon}</span>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-sm font-medium">{value}</p>
      </div>
    </div>
  )
}

function ActivityDetailSkeleton() {
  return (
    <div className="space-y-6" role="status" aria-label="Cargando actividad">
      <Skeleton className="h-8 w-72" />
      <Skeleton className="h-64 w-full rounded-2xl" />
      <Skeleton className="h-24 w-full rounded-2xl" />
      <span className="sr-only">Cargando actividad…</span>
    </div>
  )
}
