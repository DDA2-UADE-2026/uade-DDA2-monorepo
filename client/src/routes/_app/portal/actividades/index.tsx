import { IconAlertCircle, IconCheck, IconSpeakerphone } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"

import { ActivityCard, ActivityCardSkeleton } from "@/components/activities/ActivityCard"
import { ActivityEnrollDialog } from "@/components/activities/ActivityEnrollDialog"
import { PAGE_SIZE, activitySearchSchema, availableSeats } from "@/components/activities/activity-ui"
import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { list8Options } from "@/generated/@tanstack/react-query.gen"
import type { CitizenActivityResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/portal/actividades/")({
  validateSearch: activitySearchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [enrolling, setEnrolling] = useState<CitizenActivityResponse | null>(null)
  // Confirmación de lo que se acaba de hacer en esta pantalla. No se persiste:
  // la API todavía no expone las inscripciones propias del ciudadano.
  const [justEnrolled, setJustEnrolled] = useState<ReadonlySet<string>>(new Set())

  const query = useQuery(list8Options({ query: { page: page - 1, size: PAGE_SIZE } }))
  const activities = query.data?.content ?? []
  const totalItems = Number(query.data?.totalElements ?? 0)
  const totalPages = Math.max(1, query.data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Actividades" }]} />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="flex w-full flex-col gap-6 p-4 lg:p-6">
            <header className="space-y-1">
              <h1 className="font-heading text-2xl font-medium tracking-tight">
                Actividades comunitarias
              </h1>
              <p className="text-sm text-muted-foreground">
                Talleres, jornadas y campañas abiertas a la comunidad. Inscribite mientras haya cupo.
              </p>
            </header>

            {query.isPending ? (
              <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3" aria-label="Cargando actividades">
                {Array.from({ length: 6 }).map((_, index) => <ActivityCardSkeleton key={index} />)}
              </div>
            ) : query.isError ? (
              <Alert variant="destructive">
                <IconAlertCircle />
                <AlertTitle>No pudimos cargar las actividades</AlertTitle>
                <AlertDescription>
                  Ocurrió un problema al consultar las actividades disponibles.
                </AlertDescription>
                <AlertAction>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={query.isFetching}
                    onClick={() => query.refetch()}
                  >
                    Reintentar
                  </Button>
                </AlertAction>
              </Alert>
            ) : activities.length === 0 ? (
              <Empty className="min-h-80 border">
                <EmptyHeader>
                  <EmptyMedia variant="icon">
                    <IconSpeakerphone />
                  </EmptyMedia>
                  <EmptyTitle>
                    {page > 1 ? "No hay actividades en esta página" : "No hay actividades abiertas"}
                  </EmptyTitle>
                  <EmptyDescription>
                    {page > 1
                      ? "Volvé a la primera página para ver las actividades vigentes."
                      : "Volvé a consultar más adelante: publicamos nuevas actividades seguido."}
                  </EmptyDescription>
                </EmptyHeader>
                {page > 1 && (
                  <Button variant="outline" onClick={() => navigate({ search: { page: 1 } })}>
                    Primera página
                  </Button>
                )}
              </Empty>
            ) : (
              <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
                {activities.map((activity) => {
                  const enrolled = activity.id !== undefined && justEnrolled.has(activity.id)
                  const full = availableSeats(activity) === 0

                  return (
                    <ActivityCard
                      key={activity.id ?? activity.name}
                      activity={activity}
                      action={
                        <Button
                          className="w-full"
                          variant={enrolled ? "outline" : "default"}
                          disabled={enrolled || full || !activity.id}
                          onClick={() => setEnrolling(activity)}
                        >
                          {enrolled && <IconCheck />}
                          {enrolled ? "Te inscribiste" : full ? "Sin cupo" : "Inscribirme"}
                        </Button>
                      }
                    />
                  )
                })}
              </div>
            )}
          </main>
        </div>

        {!query.isPending && !query.isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={(nextPage) => navigate({ search: { page: nextPage } })}
          />
        )}
      </SidebarShellContent>

      {enrolling && (
        <ActivityEnrollDialog
          activity={enrolling}
          onOpenChange={(open) => { if (!open) setEnrolling(null) }}
          onEnrolled={(activityId) => setJustEnrolled((current) => new Set(current).add(activityId))}
        />
      )}
    </SidebarShell>
  )
}
