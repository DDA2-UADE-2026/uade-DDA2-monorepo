import { IconArrowRight, IconCalendarEvent, IconClipboardHeart } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"

import { InterventionStatusBadge } from "@/components/interventions/InterventionUi"
import { SectionHeading } from "@/components/layout/SectionHeading"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { listCitizenAppointmentsOptions, listOwnInterventionPlansOptions } from "@/generated/@tanstack/react-query.gen"
import { formatAppointmentDate, formatAppointmentRange, splitAppointments } from "@/lib/appointment-flow"

export function PortalOverview({ permissions }: { permissions: readonly string[] }) {
  const canViewAppointments = permissions.includes("appointments:own:view")
  const canViewInterventions = permissions.includes("applications:own:interventions:view")
  const appointments = useQuery({ ...listCitizenAppointmentsOptions(), enabled: canViewAppointments })
  const plans = useQuery({ ...listOwnInterventionPlansOptions({ query: { page: 0, size: 1 } }), enabled: canViewInterventions })
  const nextAppointment = splitAppointments(appointments.data ?? []).upcoming[0]
  const plan = plans.data?.content?.[0]

  if (!canViewAppointments && !canViewInterventions) return null

  return (
    <section aria-labelledby="portal-overview-title" className="space-y-3">
      <SectionHeading id="portal-overview-title" title="Tu seguimiento" description="Consultá tus próximos turnos y planes de acompañamiento." />
      <div className="grid gap-4 md:grid-cols-2">
        {canViewAppointments && <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><IconCalendarEvent className="size-5 text-primary" /> Mis turnos</CardTitle>
            <CardDescription>Próxima atención programada</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col items-start gap-4">
            {appointments.isPending ? <Skeleton className="h-12 w-full" /> : appointments.isError ? (
              <p className="text-sm text-muted-foreground">No pudimos cargar tus turnos. Consultalos desde el listado.</p>
            ) : nextAppointment ? (
              <div className="text-sm"><p className="font-medium">{nextAppointment.serviceName ?? "Turno"} · {nextAppointment.centerName}</p><p className="text-muted-foreground">{formatAppointmentDate(nextAppointment.startsAt?.slice(0, 10))} · {formatAppointmentRange(nextAppointment.startsAt, nextAppointment.endsAt)}</p></div>
            ) : <p className="text-sm text-muted-foreground">No tenés próximos turnos programados.</p>}
            <Button className="mt-auto" size="sm" variant="outline" render={<Link to="/portal/turnos" />}>Ver mis turnos <IconArrowRight /></Button>
          </CardContent>
        </Card>}
        {canViewInterventions && <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><IconClipboardHeart className="size-5 text-primary" /> Mis intervenciones</CardTitle>
            <CardDescription>Objetivos y acciones de acompañamiento</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col items-start gap-4">
            {plans.isPending ? <Skeleton className="h-12 w-full" /> : plans.isError ? (
              <p className="text-sm text-muted-foreground">No pudimos cargar tus intervenciones. Consultalas desde el listado.</p>
            ) : plan ? (
              <div className="flex flex-wrap items-center gap-2 text-sm"><span className="font-medium">{plan.programName ?? "Plan de intervención"}</span><InterventionStatusBadge status={plan.status} /><span className="text-muted-foreground">Solicitud N.º {plan.applicationNumber ?? "—"}</span></div>
            ) : <p className="text-sm text-muted-foreground">Todavía no tenés planes de intervención.</p>}
            <Button className="mt-auto" size="sm" variant="outline" render={<Link to="/portal/intervenciones" search={{ page: 1 }} />}>Ver mis intervenciones <IconArrowRight /></Button>
          </CardContent>
        </Card>}
      </div>
    </section>
  )
}
