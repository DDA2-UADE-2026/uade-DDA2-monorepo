import { IconCalendarEvent, IconMapPin, IconUsers } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { useState } from "react"
import { ActivityEnrollDialog } from "@/components/activities/ActivityEnrollDialog"
import { ActivityPage } from "@/components/activities/ActivityPage"
import { AttendanceBadge } from "@/components/activities/AttendanceBadge"
import { ActivityStatusBadge, availableSeats, formatActivityDateRange, formatActivityDateTime, seatsLabel } from "@/components/activities/activity-ui"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { get5Options } from "@/generated/@tanstack/react-query.gen"

export function CitizenActivityDetail({ activityId, canEnroll }: { activityId: string; canEnroll: boolean }) {
  const [enrolling, setEnrolling] = useState(false)
  const query = useQuery(get5Options({ path: { id: activityId } }))
  const activity = query.data
  return (
    <ActivityPage portal title={activity?.name ?? "Detalle"}>
      {query.isPending ? <p role="status">Cargando actividad…</p> : query.isError ? (
        <div role="alert" className="space-y-3"><h1 className="font-heading text-xl">{query.error.status === 404 ? "Esta actividad no está disponible" : "No pudimos cargar la actividad"}</h1><p className="text-sm text-muted-foreground">{query.error.message}</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button><Button variant="ghost" render={<Link to="/portal/actividades" search={{ page: 1, vista: "disponibles" }} />}>Volver a actividades</Button></div>
      ) : activity && (
        <>
          <header className="flex flex-wrap items-center justify-between gap-3"><h1 className="font-heading text-2xl font-medium">{activity.name}</h1><ActivityStatusBadge status={activity.status} /></header>
          <Card><CardHeader><CardTitle>Sobre la actividad</CardTitle></CardHeader><CardContent className="space-y-5">
            <p className="whitespace-pre-line">{activity.description}</p>
            <div className="grid gap-3 text-sm sm:grid-cols-3">
              <p className="flex items-start gap-2"><IconMapPin className="size-5 shrink-0 text-muted-foreground" />{activity.location}</p>
              <p className="flex items-start gap-2"><IconCalendarEvent className="size-5 shrink-0 text-muted-foreground" />{formatActivityDateRange(activity.startDate, activity.endDate)}</p>
              <p className="flex items-start gap-2"><IconUsers className="size-5 shrink-0 text-muted-foreground" />{activity.status === "OPEN" ? seatsLabel(activity) : "Inscripciones cerradas"}</p>
            </div>
          </CardContent></Card>
          {activity.myEnrollment ? (
            <Card><CardHeader><CardTitle>Tu inscripción está confirmada</CardTitle></CardHeader><CardContent className="space-y-3">
              <p className="text-sm text-muted-foreground">Te inscribiste el {formatActivityDateTime(activity.myEnrollment.enrolledAt)}.</p>
              <div className="flex items-center gap-2 text-sm">Tu asistencia: <AttendanceBadge attendance={activity.myEnrollment.attendance} /></div>
              <Button variant="outline" render={<Link to="/portal/actividades" search={{ page: 1, vista: "inscripciones" }} />}>Ver mis inscripciones</Button>
            </CardContent></Card>
          ) : canEnroll && activity.status === "OPEN" ? (
            <div className="space-y-2"><Button disabled={availableSeats(activity) === 0} onClick={() => setEnrolling(true)}>{availableSeats(activity) === 0 ? "Sin cupo disponible" : "Inscribirme en esta actividad"}</Button><p className="text-xs text-muted-foreground">La inscripción es personal. El cupo se confirma al completar la operación.</p></div>
          ) : null}
          {enrolling && <ActivityEnrollDialog activity={activity} onOpenChange={setEnrolling} />}
        </>
      )}
    </ActivityPage>
  )
}
