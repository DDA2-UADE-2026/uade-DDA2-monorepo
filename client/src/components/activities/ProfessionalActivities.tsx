import { IconClipboardCheck, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { ActivityPage } from "@/components/activities/ActivityPage"
import { ActivityStatusBadge, formatActivityDateRange, PAGE_SIZE } from "@/components/activities/activity-ui"
import { DataPagination } from "@/components/DataPagination"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listActivitiesOptions } from "@/generated/@tanstack/react-query.gen"

export function ProfessionalActivities({ page, onPageChange }: { page: number; onPageChange: (page: number) => void }) {
  const query = useQuery(listActivitiesOptions({ query: { page: page - 1, size: PAGE_SIZE } }))
  const activities = query.data?.content ?? []
  return (
    <ActivityPage pagination={query.data && (query.data.totalElements ?? 0) > 0 && <DataPagination page={page} totalPages={query.data.totalPages ?? 1} totalItems={query.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={onPageChange} />}>
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1">
          <h1 className="font-heading text-2xl font-medium">Asistencia a actividades</h1>
          <p className="text-sm text-muted-foreground">Consultá los inscriptos y registrá la asistencia de actividades publicadas o cerradas.</p>
        </div>
        <Button variant="outline" size="sm" disabled={query.isFetching} onClick={() => query.refetch()}><IconRefresh /><span className="sr-only sm:not-sr-only">Refrescar</span></Button>
      </header>
      {query.isPending ? <p role="status">Cargando actividades…</p> : query.isError ? (
        <div role="alert" className="space-y-3"><p>No se pudieron cargar las actividades.</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button></div>
      ) : activities.length === 0 ? (
        <div className="space-y-3 rounded-xl border p-6 text-sm text-muted-foreground">
          <p>{page > 1 ? "No hay actividades en esta página." : "No hay actividades publicadas o cerradas para consultar."}</p>
          {page > 1 && <Button variant="outline" onClick={() => onPageChange(1)}>Primera página</Button>}
        </div>
      ) : (
        <Table>
          <TableHeader><TableRow><TableHead>Actividad</TableHead><TableHead>Fechas</TableHead><TableHead>Inscriptos</TableHead><TableHead>Estado</TableHead><TableHead><span className="sr-only">Acciones</span></TableHead></TableRow></TableHeader>
          <TableBody>{activities.map((activity) => <TableRow key={activity.id}>
            <TableCell className="whitespace-normal"><p className="font-medium">{activity.name}</p><p className="text-xs text-muted-foreground">{activity.location}</p></TableCell>
            <TableCell>{formatActivityDateRange(activity.startDate, activity.endDate)}</TableCell>
            <TableCell>{activity.enrolledCount ?? 0}</TableCell>
            <TableCell><ActivityStatusBadge status={activity.status} /></TableCell>
            <TableCell><Button size="sm" variant="outline" render={<Link to="/gestion/actividades/$actividadId/asistencia" params={{ actividadId: activity.id ?? "" }} search={{ page: 1 }} />}><IconClipboardCheck />Ver asistencia</Button></TableCell>
          </TableRow>)}</TableBody>
        </Table>
      )}
    </ActivityPage>
  )
}
