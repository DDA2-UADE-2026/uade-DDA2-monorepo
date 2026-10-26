import { IconCheck, IconRefresh, IconX } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { ActivityPage } from "@/components/activities/ActivityPage"
import { AttendanceBadge } from "@/components/activities/AttendanceBadge"
import { ActivityStatusBadge, formatActivityDateRange, formatActivityDateTime, PAGE_SIZE } from "@/components/activities/activity-ui"
import { DataPagination } from "@/components/DataPagination"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { getProfessionalActivityOptions, get4QueryKey, list8QueryKey, listEnrollmentsOptions, listEnrollmentsQueryKey, updateAttendanceMutation } from "@/generated/@tanstack/react-query.gen"
import type { ProfessionalActivityEnrollmentResponse } from "@/generated/types.gen"

export function ActivityAttendance({ activityId, page, onPageChange, canManage }: {
  activityId: string
  page: number
  onPageChange: (page: number) => void
  canManage: boolean
}) {
  const activity = useQuery(getProfessionalActivityOptions({ path: { activityId } }))
  const enrollments = useQuery({ ...listEnrollmentsOptions({ path: { activityId }, query: { page: page - 1, size: PAGE_SIZE } }), enabled: activity.isSuccess })
  const rows = enrollments.data?.content ?? []
  return (
    <ActivityPage title="Asistencia" pagination={enrollments.data && (enrollments.data.totalElements ?? 0) > 0 && <DataPagination page={page} totalPages={enrollments.data.totalPages ?? 1} totalItems={enrollments.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={onPageChange} />}>
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1">
          <h1 className="font-heading text-2xl font-medium">{activity.data?.name ?? "Asistencia a la actividad"}</h1>
          <p className="text-sm text-muted-foreground">{canManage ? "Marcá presentes y ausentes. Podés corregir un registro; cada cambio queda auditado." : "Consulta de inscripciones y asistencia."}</p>
        </div>
        <Button variant="outline" size="sm" disabled={activity.isFetching || enrollments.isFetching} onClick={() => { void activity.refetch(); if (activity.isSuccess) void enrollments.refetch() }}><IconRefresh /><span className="sr-only sm:not-sr-only">Refrescar</span></Button>
      </header>
      {activity.isPending ? <p role="status">Cargando actividad…</p> : activity.isError ? (
        <div role="alert" className="space-y-3"><p>{activity.error.message ?? "No se pudo cargar la actividad."}</p><Button variant="outline" onClick={() => activity.refetch()}>Reintentar</Button></div>
      ) : (
        <>
          <Card size="sm"><CardContent className="flex flex-wrap items-center justify-between gap-3">
            <div><p className="font-medium">{activity.data.location}</p><p className="text-sm text-muted-foreground">{formatActivityDateRange(activity.data.startDate, activity.data.endDate)}</p></div>
            <p className="text-sm">{activity.data.enrolledCount ?? 0} inscriptos</p>
            <ActivityStatusBadge status={activity.data.status} />
          </CardContent></Card>
          {enrollments.isPending ? <p role="status">Cargando inscriptos…</p> : enrollments.isError ? (
            <div role="alert" className="space-y-3"><p>No se pudieron cargar los inscriptos.</p><Button variant="outline" onClick={() => enrollments.refetch()}>Reintentar</Button></div>
          ) : rows.length === 0 ? (
            <div className="space-y-3 rounded-xl border p-6 text-sm text-muted-foreground"><p>{page > 1 ? "No hay inscripciones en esta página." : "Todavía no hay personas inscriptas."}</p>{page > 1 && <Button variant="outline" onClick={() => onPageChange(1)}>Primera página</Button>}</div>
          ) : (
            <Table>
              <TableHeader><TableRow><TableHead>Participante</TableHead><TableHead>Asistencia</TableHead><TableHead>Último registro</TableHead>{canManage && <TableHead>Registrar</TableHead>}</TableRow></TableHeader>
              <TableBody>{rows.map((enrollment) => <AttendanceRow key={enrollment.id} enrollment={enrollment} activityId={activityId} canManage={canManage} />)}</TableBody>
            </Table>
          )}
        </>
      )}
    </ActivityPage>
  )
}

function AttendanceRow({ enrollment, activityId, canManage }: { enrollment: ProfessionalActivityEnrollmentResponse; activityId: string; canManage: boolean }) {
  const client = useQueryClient()
  const mutation = useMutation({
    ...updateAttendanceMutation(),
    onSuccess: async () => {
      toast.success("Asistencia guardada")
      await Promise.all([
        client.invalidateQueries({ queryKey: listEnrollmentsQueryKey({ path: { activityId } }) }),
        client.invalidateQueries({ queryKey: get4QueryKey({ path: { id: activityId } }) }),
        client.invalidateQueries({ queryKey: list8QueryKey() }),
      ])
    },
  })
  const save = (attendance: "PRESENT" | "ABSENT") => {
    if (enrollment.id) mutation.mutate({ path: { activityId, enrollmentId: enrollment.id }, body: { attendance } })
  }
  return (
    <TableRow>
      <TableCell className="whitespace-normal"><p className="font-medium">{enrollment.citizenName}</p><p className="text-xs text-muted-foreground">Inscripción: {formatActivityDateTime(enrollment.enrolledAt)}</p></TableCell>
      <TableCell><AttendanceBadge attendance={enrollment.attendance} /></TableCell>
      <TableCell className="whitespace-normal text-xs text-muted-foreground">
        {enrollment.attendanceRecordedAt ? <><p>{enrollment.attendanceRecordedBy?.name}</p><p>{formatActivityDateTime(enrollment.attendanceRecordedAt)}</p></> : "Sin registro"}
      </TableCell>
      {canManage && <TableCell>
        <div className="flex gap-2">
          <Button size="sm" variant="outline" aria-label={`Marcar presente a ${enrollment.citizenName}`} disabled={mutation.isPending || !enrollment.id || enrollment.attendance === "PRESENT"} onClick={() => save("PRESENT")}><IconCheck />Presente</Button>
          <Button size="sm" variant="outline" aria-label={`Marcar ausente a ${enrollment.citizenName}`} disabled={mutation.isPending || !enrollment.id || enrollment.attendance === "ABSENT"} onClick={() => save("ABSENT")}><IconX />Ausente</Button>
        </div>
        {mutation.isPending && <p role="status" className="mt-2 text-xs text-muted-foreground">Guardando…</p>}
        {mutation.isError && <p role="alert" className="mt-2 max-w-xs whitespace-normal text-xs text-destructive">{mutation.error.message ?? "No se pudo guardar. Intentá nuevamente."}</p>}
      </TableCell>}
    </TableRow>
  )
}
