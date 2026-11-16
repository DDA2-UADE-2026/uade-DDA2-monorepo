import { IconArrowLeft, IconCalendar, IconMessages, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { useState } from "react"

import { formatInterventionDate, formatInterventionDateTime, InterventionError, InterventionHeading, InterventionLoading, InterventionPage, InterventionProgress, InterventionStatusBadge } from "@/components/interventions/InterventionUi"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { getOwnInterventionPlanOptions, listOwnPublishedInterventionFollowUpsOptions } from "@/generated/@tanstack/react-query.gen"
import type { CitizenInterventionActionResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/portal/intervenciones/$planId")({ component: RouteComponent })

function RouteComponent() {
  const { planId } = Route.useParams()
  const [selectedAction, setSelectedAction] = useState<CitizenInterventionActionResponse | null>(null)
  const plan = useQuery(getOwnInterventionPlanOptions({ path: { planId } }))
  const followUps = useQuery({
    ...listOwnPublishedInterventionFollowUpsOptions({ path: { planId, actionId: selectedAction?.id ?? "" }, query: { page: 0, size: 100 } }),
    enabled: Boolean(selectedAction?.id),
  })
  return <InterventionPage breadcrumbs={[{ label: "Mis intervenciones", to: "/portal/intervenciones" }, { label: plan.data?.applicationNumber ? `Solicitud N.º ${plan.data.applicationNumber}` : "Detalle" }]}>
    <InterventionHeading title={plan.data?.programName ?? "Plan de intervención"} description={plan.data ? `Solicitud N.º ${plan.data.applicationNumber}${plan.data.programEditionName ? ` · ${plan.data.programEditionName}` : ""}` : "Objetivos, acciones y novedades de tu acompañamiento."} action={<div className="flex gap-2"><Button variant="outline" size="sm" render={<Link to="/portal/intervenciones" search={{ page: 1 }} />}><IconArrowLeft />Volver</Button><Button variant="outline" size="icon-sm" aria-label="Refrescar" disabled={plan.isFetching} onClick={() => plan.refetch()}><IconRefresh className={plan.isFetching ? "animate-spin" : undefined} /></Button></div>} />
    {plan.isPending ? <InterventionLoading /> : plan.isError ? <InterventionError error={plan.error} title="No pudimos cargar el plan" retry={() => plan.refetch()} /> : <>
      <Card><CardHeader><div className="flex flex-col justify-between gap-4 sm:flex-row"><div><div className="mb-2"><InterventionStatusBadge status={plan.data.status} /></div><CardTitle>Tu plan de acompañamiento</CardTitle><CardDescription className="mt-1 inline-flex items-center gap-1"><IconCalendar className="size-4" />Creado el {formatInterventionDateTime(plan.data.createdAt)}</CardDescription></div><InterventionProgress statuses={(plan.data.objectives ?? []).flatMap((objective) => (objective.actions ?? []).map((action) => action.status))} /></div></CardHeader></Card>
      <div className="space-y-5">{(plan.data.objectives ?? []).map((objective, index) => <Card key={objective.id ?? index}>
        <CardHeader><CardTitle>Objetivo {index + 1}</CardTitle><CardDescription className="text-foreground">{objective.description}</CardDescription></CardHeader>
        <CardContent><div className="overflow-hidden rounded-xl border"><Table>
          <TableHeader><TableRow><TableHead className="min-w-72">Acción</TableHead><TableHead>Fechas acordadas</TableHead><TableHead>Estado</TableHead><TableHead className="text-right">Novedades</TableHead></TableRow></TableHeader>
          <TableBody>{(objective.actions ?? []).map((action) => <TableRow key={action.id} data-state={selectedAction?.id === action.id ? "selected" : undefined}>
            <TableCell className="font-medium">{action.description}</TableCell>
            <TableCell>{formatInterventionDate(action.startDate)} — {formatInterventionDate(action.dueDate)}</TableCell>
            <TableCell><InterventionStatusBadge status={action.status} /></TableCell>
            <TableCell className="text-right"><Button variant="outline" size="sm" onClick={() => setSelectedAction(action)}><IconMessages />Ver novedades</Button></TableCell>
          </TableRow>)}</TableBody>
        </Table></div></CardContent>
      </Card>)}</div>
      {selectedAction && <Card>
        <CardHeader><CardTitle>Novedades de la acción</CardTitle><CardDescription>{selectedAction.description}</CardDescription></CardHeader>
        <CardContent>{followUps.isPending ? <div className="space-y-3"><Skeleton className="h-20 w-full" /><Skeleton className="h-20 w-full" /></div> : followUps.isError ? <InterventionError error={followUps.error} title="No pudimos cargar las novedades" retry={() => followUps.refetch()} /> : !followUps.data.content?.length ? <Empty className="min-h-44 border"><EmptyHeader><EmptyMedia variant="icon"><IconMessages /></EmptyMedia><EmptyTitle>Sin novedades publicadas</EmptyTitle><EmptyDescription>El equipo todavía no publicó un resumen para esta acción.</EmptyDescription></EmptyHeader></Empty> : <div className="space-y-3">{followUps.data.content.map((entry) => <div key={entry.id} className="rounded-xl border p-4">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2"><div className="flex items-center gap-2"><InterventionStatusBadge status={entry.status} /><span className="text-sm text-muted-foreground">{formatInterventionDate(entry.occurredOn)}</span></div><span className="text-xs text-muted-foreground">Publicado {formatInterventionDateTime(entry.publishedAt)}</span></div>
          <p className="text-sm leading-relaxed">{entry.summary}</p>
        </div>)}</div>}</CardContent>
      </Card>}
    </>}
  </InterventionPage>
}
