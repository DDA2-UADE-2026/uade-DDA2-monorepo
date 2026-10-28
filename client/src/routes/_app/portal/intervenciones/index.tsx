import { IconArrowRight, IconClipboardHeart, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { formatInterventionDateTime, InterventionError, InterventionHeading, InterventionLoading, InterventionPage, InterventionStatusBadge } from "@/components/interventions/InterventionUi"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { listOwnInterventionPlansOptions } from "@/generated/@tanstack/react-query.gen"

const PAGE_SIZE = 10
const searchSchema = z.object({ page: z.coerce.number().int().positive().catch(1).default(1) })

export const Route = createFileRoute("/_app/portal/intervenciones/")({ validateSearch: searchSchema, component: RouteComponent })

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const plans = useQuery({ ...listOwnInterventionPlansOptions({ query: { page: page - 1, size: PAGE_SIZE } }), placeholderData: keepPreviousData })
  return <InterventionPage breadcrumbs={[{ label: "Mis intervenciones" }]}>
    <InterventionHeading title="Mis intervenciones" description="Consultá los objetivos y acciones acordados para acompañar tu situación." action={<Button variant="outline" size="sm" disabled={plans.isFetching} onClick={() => plans.refetch()}><IconRefresh className={plans.isFetching ? "animate-spin" : undefined} />Refrescar</Button>} />
    {plans.isPending ? <InterventionLoading /> : plans.isError ? <InterventionError error={plans.error} title="No pudimos cargar tus intervenciones" retry={() => plans.refetch()} /> : !plans.data.content?.length ? <Empty className="min-h-72 border"><EmptyHeader><EmptyMedia variant="icon"><IconClipboardHeart /></EmptyMedia><EmptyTitle>{page > 1 ? "No hay planes en esta página" : "Todavía no tenés planes de intervención"}</EmptyTitle><EmptyDescription>{page > 1 ? "Volvé a la primera página para consultar tus planes." : "Cuando el equipo profesional registre un plan para una de tus solicitudes, aparecerá acá."}</EmptyDescription></EmptyHeader>{page > 1 && <Button variant="outline" onClick={() => navigate({ search: { page: 1 } })}>Primera página</Button>}</Empty> : <>
      <div className="grid gap-4 md:grid-cols-2" aria-busy={plans.isFetching}>{plans.data.content.map((plan) => <Card key={plan.id}>
        <CardHeader>
          <div className="flex flex-wrap items-center justify-between gap-2"><p className="text-xs font-medium text-muted-foreground">Solicitud N.º {plan.applicationNumber}</p><InterventionStatusBadge status={plan.status} /></div>
          <CardTitle className="text-lg">{plan.programName ?? "Plan de intervención"}</CardTitle>
          <CardDescription>{plan.programEditionName ?? "Acompañamiento social"}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <p className="text-sm text-muted-foreground">Creado el {formatInterventionDateTime(plan.createdAt)}</p>
          {plan.id && <Button variant="outline" render={<Link to="/portal/intervenciones/$planId" params={{ planId: plan.id }} />}>Ver plan<IconArrowRight /></Button>}
        </CardContent>
      </Card>)}</div>
      <DataPagination className="rounded-xl border" page={page} totalPages={Math.max(1, plans.data.totalPages ?? 1)} totalItems={plans.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={(nextPage) => navigate({ search: { page: nextPage } })} />
    </>}
  </InterventionPage>
}
