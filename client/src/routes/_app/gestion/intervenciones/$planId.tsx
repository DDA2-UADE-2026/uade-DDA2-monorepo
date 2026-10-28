import { IconArrowLeft, IconCalendar, IconHistory, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { useState } from "react"

import { ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { InterventionFollowUps } from "@/components/interventions/InterventionFollowUps"
import { InterventionPlanForm } from "@/components/interventions/InterventionPlanForm"
import { formatInterventionDate, formatInterventionDateTime, InterventionError, InterventionHeading, InterventionLoading, InterventionPage, InterventionProgress, InterventionStatusBadge } from "@/components/interventions/InterventionUi"
import { UserChip, UserInline } from "@/components/UserChip"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { get3Options, get4Options } from "@/generated/@tanstack/react-query.gen"
import type { InterventionActionResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/gestion/intervenciones/$planId")({ component: RouteComponent })

function RouteComponent() {
  const { planId: applicationId } = Route.useParams()
  const { user } = Route.useRouteContext()
  const [selectedAction, setSelectedAction] = useState<InterventionActionResponse | null>(null)
  const application = useQuery(get3Options({ path: { id: applicationId } }))
  const plan = useQuery({ ...get4Options({ path: { id: applicationId } }), retry: false })
  const permissions = user.permissions ?? []
  const canCreatePlan = permissions.includes("applications:interventions:create")
  const canCreateFollowUp = permissions.includes("applications:interventions:follow-ups:create")
  const canPublish = permissions.includes("applications:interventions:follow-ups:publish")
  const planMissing = plan.isError && plan.error.status === 404

  return <InterventionPage breadcrumbs={[{ label: "Intervenciones", to: "/gestion/intervenciones" }, { label: application.data?.applicationNumber ? `Solicitud N.º ${application.data.applicationNumber}` : "Detalle" }]}>
    <InterventionHeading title={application.data?.applicationNumber ? `Intervención · Solicitud N.º ${application.data.applicationNumber}` : "Intervención"} description={application.data ? `${application.data.programName ?? "Programa"}${application.data.programEditionName ? ` · ${application.data.programEditionName}` : ""}` : "Plan y seguimientos profesionales del caso."} action={<div className="flex gap-2"><Button variant="outline" size="sm" render={<Link to="/gestion/intervenciones" search={{ page: 1 }} />}><IconArrowLeft />Volver</Button><Button variant="outline" size="icon-sm" aria-label="Refrescar" disabled={plan.isFetching} onClick={() => { application.refetch(); plan.refetch() }}><IconRefresh className={plan.isFetching ? "animate-spin" : undefined} /></Button></div>} />
    {application.isPending ? <InterventionLoading /> : application.isError ? <InterventionError error={application.error} title="No pudimos cargar la solicitud" retry={() => application.refetch()} /> : <Card size="sm"><CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div><p className="text-xs text-muted-foreground">Titular</p><UserChip className="mt-1" user={{ id: application.data.userId, name: application.data.userName, email: application.data.userEmail }} /></div>
      <div><p className="text-xs text-muted-foreground">Estado del caso</p><div className="mt-1"><ApplicationStatusBadge status={application.data.status} /></div></div>
      <div><p className="text-xs text-muted-foreground">Trabajador asignado</p><UserChip className="mt-1" user={{ id: application.data.assignedWorkerUserId, name: application.data.assignedWorkerName }} fallback="Sin asignar" /></div>
      <div><p className="text-xs text-muted-foreground">Última actualización</p><p className="mt-1 font-medium">{formatInterventionDateTime(application.data.updatedAt)}</p></div>
    </CardContent></Card>}
    {plan.isPending ? <InterventionLoading label="Cargando plan" /> : planMissing ? canCreatePlan ? <InterventionPlanForm applicationId={applicationId} /> : <Card><CardHeader><CardTitle>El caso todavía no tiene un plan</CardTitle><CardDescription>Necesitás el permiso de creación de intervenciones para registrarlo.</CardDescription></CardHeader></Card> : plan.isError ? <InterventionError error={plan.error} title="No pudimos cargar el plan" retry={() => plan.refetch()} /> : plan.data && <>
      <Card>
        <CardHeader><div className="flex flex-col justify-between gap-4 sm:flex-row"><div><CardTitle>Plan de intervención</CardTitle><CardDescription className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-2"><span className="inline-flex items-center gap-1.5">Responsable: <UserInline user={{ id: plan.data.responsibleUserId, name: plan.data.responsibleUserName }} fallback="Sin responsable" /></span><span className="inline-flex items-center gap-1"><IconCalendar className="size-4" />Creado el {formatInterventionDateTime(plan.data.createdAt)}</span></CardDescription></div><InterventionProgress statuses={(plan.data.objectives ?? []).flatMap((objective) => (objective.actions ?? []).map((action) => action.status))} /></div></CardHeader>
      </Card>
      <div className="space-y-5">{(plan.data.objectives ?? []).map((objective, objectiveIndex) => <Card key={objective.id ?? objectiveIndex}>
        <CardHeader><CardTitle>Objetivo {objectiveIndex + 1}</CardTitle><CardDescription className="text-foreground">{objective.description}</CardDescription></CardHeader>
        <CardContent><div className="overflow-hidden rounded-xl border"><Table>
          <TableHeader><TableRow><TableHead className="min-w-72">Acción</TableHead><TableHead>Vigencia prevista</TableHead><TableHead>Estado</TableHead><TableHead>Responsable</TableHead><TableHead className="text-right">Seguimientos</TableHead></TableRow></TableHeader>
          <TableBody>{(objective.actions ?? []).map((action) => <TableRow key={action.id}>
            <TableCell className="font-medium">{action.description}</TableCell>
            <TableCell>{formatInterventionDate(action.startDate)} — {formatInterventionDate(action.dueDate)}</TableCell>
            <TableCell><InterventionStatusBadge status={action.status} /></TableCell>
            <TableCell className="min-w-44"><UserChip user={{ id: action.responsibleUserId, name: action.responsibleUserName }} /></TableCell>
            <TableCell className="text-right"><Button variant="outline" size="sm" onClick={() => setSelectedAction(action)}><IconHistory />Ver historial</Button></TableCell>
          </TableRow>)}</TableBody>
        </Table></div></CardContent>
      </Card>)}</div>
    </>}
    {selectedAction && <InterventionFollowUps applicationId={applicationId} action={selectedAction} canCreate={canCreateFollowUp} canPublish={canPublish} onClose={() => setSelectedAction(null)} />}
  </InterventionPage>
}
