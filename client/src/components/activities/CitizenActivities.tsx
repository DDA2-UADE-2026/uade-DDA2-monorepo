import { IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { useState } from "react"
import { ActivityCard, ActivityCardSkeleton } from "@/components/activities/ActivityCard"
import { ActivityEnrollDialog } from "@/components/activities/ActivityEnrollDialog"
import { ActivityPage } from "@/components/activities/ActivityPage"
import { availableSeats, PAGE_SIZE } from "@/components/activities/activity-ui"
import { DataPagination } from "@/components/DataPagination"
import { Button } from "@/components/ui/button"
import { list10Options } from "@/generated/@tanstack/react-query.gen"
import type { CitizenActivityResponse } from "@/generated/types.gen"

export function CitizenActivities({ page, view, onSearchChange, canEnroll }: {
  page: number
  view: "disponibles" | "inscripciones"
  onSearchChange: (search: { page: number; vista: "disponibles" | "inscripciones" }) => void
  canEnroll: boolean
}) {
  const [enrolling, setEnrolling] = useState<CitizenActivityResponse | null>(null)
  const query = useQuery(list10Options({ query: { page: page - 1, size: PAGE_SIZE, enrolledOnly: view === "inscripciones" } }))
  const activities = query.data?.content ?? []
  return (
    <ActivityPage portal pagination={query.data && (query.data.totalElements ?? 0) > 0 && <DataPagination page={page} totalPages={query.data.totalPages ?? 1} totalItems={query.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={(nextPage) => onSearchChange({ page: nextPage, vista: view })} />}>
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1"><h1 className="font-heading text-2xl font-medium">Actividades comunitarias</h1><p className="text-sm text-muted-foreground">Encontrá talleres, jornadas y campañas. Consultá tus inscripciones y tu asistencia.</p></div>
        <Button size="sm" variant="outline" disabled={query.isFetching} onClick={() => query.refetch()}><IconRefresh /><span className="sr-only sm:not-sr-only">Refrescar</span></Button>
      </header>
      <div className="flex flex-wrap gap-2" role="group" aria-label="Vista de actividades">
        <Button variant={view === "disponibles" ? "default" : "outline"} aria-pressed={view === "disponibles"} onClick={() => onSearchChange({ page: 1, vista: "disponibles" })}>Actividades abiertas</Button>
        <Button variant={view === "inscripciones" ? "default" : "outline"} aria-pressed={view === "inscripciones"} onClick={() => onSearchChange({ page: 1, vista: "inscripciones" })}>Mis inscripciones</Button>
      </div>
      {query.isPending ? (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3" role="status" aria-label="Cargando actividades">{[0, 1, 2].map((key) => <ActivityCardSkeleton key={key} />)}</div>
      ) : query.isError ? (
        <div role="alert" className="space-y-3"><p>No pudimos cargar las actividades.</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button></div>
      ) : activities.length === 0 ? (
        <div className="space-y-3 rounded-xl border p-6 text-sm text-muted-foreground">
          <p>{page > 1 ? "No hay actividades en esta página." : view === "inscripciones" ? "Todavía no te inscribiste en ninguna actividad." : "No hay actividades abiertas en este momento."}</p>
          {page > 1 && <Button variant="outline" onClick={() => onSearchChange({ page: 1, vista: view })}>Primera página</Button>}
          {view === "inscripciones" && page === 1 && <Button variant="outline" onClick={() => onSearchChange({ page: 1, vista: "disponibles" })}>Explorar actividades</Button>}
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{activities.map((activity) => <ActivityCard key={activity.id} activity={activity} action={
          <div className="flex w-full flex-col gap-2">
            <Button className="w-full" variant="outline" render={<Link to="/portal/actividades/$actividadId" params={{ actividadId: activity.id ?? "" }} />}>Ver detalle</Button>
            {!activity.myEnrollment && canEnroll && activity.status === "OPEN" && <Button disabled={!activity.id || availableSeats(activity) === 0} onClick={() => setEnrolling(activity)}>{availableSeats(activity) === 0 ? "Sin cupo" : "Inscribirme"}</Button>}
          </div>
        } />)}</div>
      )}
      {enrolling && <ActivityEnrollDialog activity={enrolling} onOpenChange={(open) => { if (!open) setEnrolling(null) }} />}
    </ActivityPage>
  )
}
