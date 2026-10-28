import { IconArrowRight, IconRefresh, IconSearch } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"
import { ApplicationError, ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { benefitAmount, benefitName, benefitPeriod, BenefitStatusBadge, benefitStatusLabels, type BenefitStatus } from "@/components/benefits/BenefitUi"
import { DataPagination } from "@/components/DataPagination"
import { UserChip } from "@/components/UserChip"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listAdminBenefitGrantsOptions } from "@/generated/@tanstack/react-query.gen"

const schema = z.object({ page: z.coerce.number().int().positive().catch(1).default(1), numero: z.coerce.number().int().positive().optional().catch(undefined), estado: z.enum(["ACTIVE", "SUSPENDED", "FINALIZED", "EXPIRED"]).optional().catch(undefined) })
export const Route = createFileRoute("/_app/gestion/beneficios/")({ validateSearch: schema, component: BenefitsPage })

function BenefitsPage() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [number, setNumber] = useState(search.numero?.toString() ?? "")
  const query = useQuery({ ...listAdminBenefitGrantsOptions({ query: { page: search.page - 1, size: 20, status: search.estado, applicationNumber: search.numero } }), placeholderData: keepPreviousData })
  return <ApplicationPage breadcrumbs={[{ label: "Beneficios" }]}>
    <ApplicationHeading title="Beneficios otorgados" description="Consultá y gestioná los beneficios asignados a solicitudes." action={<div className="flex flex-wrap gap-2"><Button size="sm" variant="outline" render={<Link to="/gestion/solicitudes" search={{ page: 1 }} />}>Ir a solicitudes</Button><Button size="sm" variant="outline" onClick={() => query.refetch()} disabled={query.isFetching}><IconRefresh />Actualizar</Button></div>} />
    <form className="flex flex-wrap items-end gap-3 rounded-xl border p-4" onSubmit={(event) => { event.preventDefault(); navigate({ search: { ...search, page: 1, numero: number ? Number(number) : undefined } }) }}>
      <label className="grid gap-1 text-sm">N.º de solicitud<Input type="number" min={1} value={number} onChange={(event) => setNumber(event.target.value)} placeholder="Todas" /></label>
      <label className="grid gap-1 text-sm">Estado<Select value={search.estado ?? null} onValueChange={(value) => navigate({ search: { ...search, page: 1, estado: (value || undefined) as BenefitStatus | undefined } })}><SelectTrigger className="w-44"><SelectValue placeholder="Todos" /></SelectTrigger><SelectContent><SelectItem value={null}>Todos</SelectItem>{(Object.keys(benefitStatusLabels) as BenefitStatus[]).map((status) => <SelectItem key={status} value={status}>{benefitStatusLabels[status]}</SelectItem>)}</SelectContent></Select></label>
      <Button type="submit" variant="outline"><IconSearch />Buscar</Button>
    </form>
    {query.isPending ? <ApplicationLoading /> : query.isError ? <ApplicationError error={query.error} title="No pudimos cargar los beneficios" retry={() => query.refetch()} /> : !query.data.content?.length ? <div className="space-y-3 rounded-xl border p-8 text-center text-sm text-muted-foreground"><p>No hay beneficios para los filtros seleccionados. Para otorgar uno, abrí una solicitud aprobada.</p>{search.page > 1 && <Button variant="outline" onClick={() => navigate({ search: { ...search, page: 1 } })}>Primera página</Button>}</div> : <>
      <div className="overflow-hidden rounded-xl border"><Table><TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Titular</TableHead><TableHead>Beneficio</TableHead><TableHead>Estado</TableHead><TableHead>Vigencia</TableHead><TableHead>Monto</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data.content.map(({ benefit, userId, userName }) => benefit && <TableRow key={benefit.id}><TableCell>N.º {benefit.applicationNumber}</TableCell><TableCell><UserChip user={{ id: userId, name: userName }} /></TableCell><TableCell><p>{benefitName(benefit.benefitType)}</p><p className="text-xs text-muted-foreground">{benefit.programName}</p></TableCell><TableCell><BenefitStatusBadge status={benefit.status} /></TableCell><TableCell>{benefitPeriod(benefit.startDate, benefit.endDate)}</TableCell><TableCell>{benefitAmount(benefit.amount)}</TableCell><TableCell>{benefit.id && benefit.applicationId && <Button size="sm" variant="outline" render={<Link to="/gestion/beneficios/$beneficioId" params={{ beneficioId: benefit.id }} search={{ solicitud: benefit.applicationId }} />}>Gestionar<IconArrowRight /></Button>}</TableCell></TableRow>)}</TableBody></Table></div>
      <DataPagination page={search.page} totalPages={Math.max(1, query.data.totalPages ?? 1)} totalItems={query.data.totalElements ?? 0} pageSize={20} onPageChange={(page) => navigate({ search: { ...search, page } })} />
    </>}
  </ApplicationPage>
}
