import { IconArrowRight, IconRefresh, IconSearch } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { SeverityBadge } from "@/components/applications/SeverityBadge"
import { FilterActions, FilterBar, FilterField } from "@/components/FilterBar"
import { formatInterventionDateTime, InterventionError, InterventionHeading, InterventionLoading, InterventionPage, InterventionProgressSummary } from "@/components/interventions/InterventionUi"
import { UserChip } from "@/components/UserChip"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listApplicationEvaluationsOptions } from "@/generated/@tanstack/react-query.gen"
import { priorityLabel, type Priority } from "@/lib/social-management"

const PAGE_SIZE = 20
const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
  numero: z.coerce.number().int().positive().optional().catch(undefined),
  prioridad: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]).optional().catch(undefined),
})

export const Route = createFileRoute("/_app/gestion/intervenciones/")({ validateSearch: searchSchema, component: RouteComponent })

function RouteComponent() {
  const search = Route.useSearch()
  const [number, setNumber] = useState(search.numero?.toString() ?? "")
  const navigate = useNavigate({ from: Route.fullPath })
  const evaluations = useQuery({
    ...listApplicationEvaluationsOptions({ query: { page: search.page - 1, size: PAGE_SIZE, applicationNumber: search.numero, priority: search.prioridad } }),
    placeholderData: keepPreviousData,
  })
  return <InterventionPage breadcrumbs={[{ label: "Intervenciones" }]}>
    <InterventionHeading title="Intervenciones" description="Gestioná planes y seguimientos de casos con evaluación social registrada." action={<Button variant="outline" size="sm" disabled={evaluations.isFetching} onClick={() => evaluations.refetch()}><IconRefresh className={evaluations.isFetching ? "animate-spin" : undefined} />Refrescar</Button>} />
    <FilterBar label="Filtrar casos" onSubmit={() => {
      const raw = number.trim()
      navigate({ search: { ...search, page: 1, numero: raw ? Number(raw) : undefined } })
    }}>
      <FilterField label="N.º de solicitud" htmlFor="intervention-number"><Input id="intervention-number" type="number" min={1} value={number} onChange={(event) => setNumber(event.target.value)} placeholder="Ej. 121" /></FilterField>
      <FilterField label="Prioridad" htmlFor="intervention-priority"><Select value={search.prioridad ?? null} onValueChange={(value) => navigate({ search: { ...search, page: 1, prioridad: (value || undefined) as Priority | undefined } })}><SelectTrigger id="intervention-priority" className="w-full"><SelectValue placeholder="Todas" /></SelectTrigger><SelectContent><SelectItem value={null}>Todas</SelectItem>{(["LOW", "MEDIUM", "HIGH", "URGENT"] as const).map((value) => <SelectItem key={value} value={value}>{priorityLabel[value]}</SelectItem>)}</SelectContent></Select></FilterField>
      <FilterActions><Button type="submit" variant="outline"><IconSearch />Buscar</Button></FilterActions>
    </FilterBar>
    {evaluations.isPending ? <InterventionLoading label="Cargando casos evaluados" /> : evaluations.isError ? <InterventionError error={evaluations.error} title="No pudimos cargar los casos" retry={() => evaluations.refetch()} /> : !evaluations.data.content?.length ? <Empty className="min-h-64 border"><EmptyHeader><EmptyMedia variant="icon"><IconSearch /></EmptyMedia><EmptyTitle>No hay casos para mostrar</EmptyTitle><EmptyDescription>Probá con otros filtros o verificá que exista una evaluación social registrada.</EmptyDescription></EmptyHeader></Empty> : <>
      <div className="overflow-hidden rounded-xl border" aria-busy={evaluations.isFetching}><Table>
        <TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Titular</TableHead><TableHead>Programa</TableHead><TableHead>Estado</TableHead><TableHead>Prioridad</TableHead><TableHead>Evaluada</TableHead><TableHead>Progreso</TableHead><TableHead className="text-right">Plan</TableHead></TableRow></TableHeader>
        <TableBody>{evaluations.data.content.map((item) => {
          const hasPlan = Boolean(item.interventionPlanId)
          return <TableRow key={item.id} className={hasPlan ? "bg-blue-50/80 hover:bg-blue-100/80 dark:bg-blue-950/30 dark:hover:bg-blue-900/30" : undefined}>
            <TableCell className="font-medium">N.º {item.applicationNumber}</TableCell>
            <TableCell className="min-w-48"><UserChip user={{ id: item.applicantUserId, name: item.applicantName }} /></TableCell>
            <TableCell><p>{item.programName ?? "—"}</p><p className="text-xs text-muted-foreground">{item.programEditionName}</p></TableCell>
            <TableCell><ApplicationStatusBadge status={item.applicationStatus} /></TableCell>
            <TableCell>{item.priority && <SeverityBadge severity={item.priority}>{priorityLabel[item.priority]}</SeverityBadge>}</TableCell>
            <TableCell>{formatInterventionDateTime(item.evaluatedAt)}</TableCell>
            <TableCell>{hasPlan
              ? <InterventionProgressSummary completed={item.interventionCompletedActionCount ?? 0} total={item.interventionActionCount ?? 0} compact />
              : <span className="text-sm text-muted-foreground">Sin plan</span>}
            </TableCell>
            <TableCell className="text-right">{item.applicationId && <Button variant={hasPlan ? "default" : "outline"} size="sm" render={<Link to="/gestion/intervenciones/$planId" params={{ planId: item.applicationId }} />}>{hasPlan ? "Gestionar" : "Crear plan"}<IconArrowRight /></Button>}</TableCell>
          </TableRow>
        })}</TableBody>
      </Table></div>
      <DataPagination className="rounded-xl border" page={search.page} totalPages={Math.max(1, evaluations.data.totalPages ?? 1)} totalItems={evaluations.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={(page) => navigate({ search: { ...search, page } })} />
    </>}
  </InterventionPage>
}
