import { IconArrowRight, IconGift, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { ApplicationError, ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { benefitAmount, benefitName, benefitPeriod, BenefitStatusBadge } from "@/components/benefits/BenefitUi"
import { DataPagination } from "@/components/DataPagination"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { listOwnBenefitGrantsOptions } from "@/generated/@tanstack/react-query.gen"

const PAGE_SIZE = 10
export const Route = createFileRoute("/_app/portal/beneficios/")({ validateSearch: z.object({ page: z.coerce.number().int().positive().catch(1).default(1) }), component: BenefitsPage })

function BenefitsPage() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const query = useQuery({ ...listOwnBenefitGrantsOptions({ query: { page: page - 1, size: PAGE_SIZE } }), placeholderData: keepPreviousData })
  return <ApplicationPage breadcrumbs={[{ label: "Mis beneficios" }]}>
    <ApplicationHeading title="Mis beneficios" description="Consultá los beneficios otorgados, su estado y vigencia." action={<Button variant="outline" size="sm" disabled={query.isFetching} onClick={() => query.refetch()}><IconRefresh />Actualizar</Button>} />
    {query.isPending ? <ApplicationLoading /> : query.isError ? <ApplicationError error={query.error} title="No pudimos cargar tus beneficios" retry={() => query.refetch()} /> : !query.data.content?.length ? <Empty className="min-h-64 border"><EmptyHeader><EmptyMedia variant="icon"><IconGift /></EmptyMedia><EmptyTitle>{page > 1 ? "No hay beneficios en esta página" : "Todavía no tenés beneficios asignados"}</EmptyTitle><EmptyDescription>Cuando se otorgue un beneficio para una de tus solicitudes, aparecerá acá.</EmptyDescription></EmptyHeader>{page > 1 && <Button variant="outline" onClick={() => navigate({ search: { page: 1 } })}>Primera página</Button>}</Empty> : <>
      <div className="grid gap-4 md:grid-cols-2" aria-busy={query.isFetching}>{query.data.content.map((grant) => <Card key={grant.id}><CardHeader><div className="flex flex-wrap items-center justify-between gap-2"><CardTitle>{benefitName(grant.benefitType)}</CardTitle><BenefitStatusBadge status={grant.status} /></div><CardDescription>{grant.programName} {grant.programEditionName && `· ${grant.programEditionName}`}</CardDescription></CardHeader><CardContent className="space-y-3 text-sm">{grant.description && <p>{grant.description}</p>}<p><span className="text-muted-foreground">Monto: </span>{benefitAmount(grant.amount)}</p><p><span className="text-muted-foreground">Vigencia: </span>{benefitPeriod(grant.startDate, grant.endDate)}</p><p className="text-muted-foreground">Solicitud N.º {grant.applicationNumber ?? "—"}</p>{grant.applicationId && <Button size="sm" variant="outline" render={<Link to="/portal/solicitudes/$solicitudId" params={{ solicitudId: grant.applicationId }} />}>Ver solicitud <IconArrowRight /></Button>}</CardContent></Card>)}</div>
      <DataPagination className="rounded-xl border" page={page} totalPages={Math.max(1, query.data.totalPages ?? 1)} totalItems={query.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={(next) => navigate({ search: { page: next } })} />
    </>}
  </ApplicationPage>
}
