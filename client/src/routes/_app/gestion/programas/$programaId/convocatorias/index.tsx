import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"
import { LoadingOrError, RoutePanel, statusLabels } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { list1Options } from "@/generated/@tanstack/react-query.gen"

const searchSchema = z.object({ page: z.coerce.number().int().positive().catch(1).default(1) })

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/",
)({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const query = useQuery(list1Options({ path: { programId: programaId }, query: { page: page - 1, size: 10 } }))
  const totalPages = Math.max(1, query.data?.totalPages ?? 1)
  return <RoutePanel>
    <div className="mb-5 flex justify-end"><Button render={<Link to="/gestion/programas/$programaId/convocatorias/nueva" params={{ programaId }} />}>Nueva convocatoria</Button></div>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
    {query.data?.content?.length === 0 ? <p className="text-sm text-muted-foreground">No hay convocatorias registradas.</p> : <Table><TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Vigencia</TableHead><TableHead>Cupos</TableHead><TableHead>Estado</TableHead></TableRow></TableHeader><TableBody>{query.data?.content?.map((edition) => <TableRow key={edition.id}><TableCell className="font-medium"><Link className="hover:underline" to="/gestion/programas/$programaId/convocatorias/$edicionId" params={{ programaId, edicionId: edition.id ?? "" }}>{edition.name}</Link></TableCell><TableCell>{edition.startDate ?? "—"} / {edition.endDate ?? "—"}</TableCell><TableCell>{edition.currentEnrollment ?? 0} / {edition.maxCapacity ?? "∞"}</TableCell><TableCell>{edition.status ? statusLabels[edition.status] : "—"}</TableCell></TableRow>)}</TableBody></Table>}
    {(query.data?.totalElements ?? 0) > 0 && <div className="mt-5 flex items-center justify-between text-sm"><span>{query.data?.totalElements} convocatorias</span><div className="flex gap-2"><Button size="sm" variant="outline" disabled={page <= 1} onClick={() => navigate({ search: { page: page - 1 } })}>Anterior</Button><span className="self-center">{Math.min(page, totalPages)} de {totalPages}</span><Button size="sm" variant="outline" disabled={page >= totalPages} onClick={() => navigate({ search: { page: page + 1 } })}>Siguiente</Button></div></div>}
  </RoutePanel>
}
