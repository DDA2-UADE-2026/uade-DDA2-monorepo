import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { IconRefresh } from "@tabler/icons-react"
import { DataPagination } from "@/components/DataPagination"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatSocialDate, levelLabel, priorityLabel, socialQueries } from "@/lib/social-management"

export const Route = createFileRoute("/_app/gestion/casos/")({ component: RouteComponent })

function RouteComponent() {
  const [page, setPage] = useState(1)
  const [number, setNumber] = useState("")
  const [appliedNumber, setAppliedNumber] = useState("")
  const [priority, setPriority] = useState("")
  const query = useQuery(socialQueries.evaluations({ page: page - 1, size: 20, applicationNumber: appliedNumber || undefined, priority: priority || undefined }))
  return <SidebarShell>
    <OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={[{ label: "Evaluaciones sociales" }]} /></OutletNavSticky>
    <SidebarShellContent>
      <div className="min-h-0 flex-1 overflow-auto p-4 lg:p-6 space-y-5">
        <div><h1 className="font-heading text-2xl font-medium">Evaluaciones sociales</h1><p className="text-sm text-muted-foreground">Historial de evaluaciones registradas y solicitudes relacionadas.</p></div>
        <form className="flex flex-wrap items-end gap-2" onSubmit={e => { e.preventDefault(); setPage(1); setAppliedNumber(number.trim()) }}>
          <label className="space-y-1 text-sm">N.º de solicitud<Input type="number" min={1} value={number} onChange={e => setNumber(e.target.value)} placeholder="Buscar número" /></label>
          <label className="space-y-1 text-sm">Prioridad<select className="h-9 rounded-full border border-input bg-input/30 px-3" value={priority} onChange={e => { setPriority(e.target.value); setPage(1) }}><option value="">Todas</option><option value="LOW">Baja</option><option value="MEDIUM">Media</option><option value="HIGH">Alta</option><option value="URGENT">Urgente</option></select></label>
          <Button type="submit" variant="outline">Buscar</Button><Button type="button" variant="ghost" onClick={() => query.refetch()} disabled={query.isFetching}><IconRefresh />Actualizar</Button>
        </form>
        {query.isPending ? <p>Cargando evaluaciones…</p> : query.isError ? <div role="alert" className="space-y-2"><p>No se pudieron cargar las evaluaciones.</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button></div> : !query.data.content.length ? <p className="rounded-xl border p-8 text-center text-muted-foreground">No hay evaluaciones con estos filtros.</p> : <Table><TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Titular</TableHead><TableHead>Programa</TableHead><TableHead>Vulnerabilidad</TableHead><TableHead>Prioridad</TableHead><TableHead>Profesional</TableHead><TableHead>Evaluada</TableHead></TableRow></TableHeader><TableBody>{query.data.content.map(item => <TableRow key={item.id}><TableCell><Link className="font-medium hover:underline" to="/gestion/casos/$solicitudId/evaluacion" params={{ solicitudId: item.applicationId }}>N.º {item.applicationNumber}</Link></TableCell><TableCell>{item.applicantName}</TableCell><TableCell>{item.programName}</TableCell><TableCell><Badge variant="outline">{levelLabel[item.vulnerability]}</Badge></TableCell><TableCell><Badge variant="secondary">{priorityLabel[item.priority]}</Badge></TableCell><TableCell>{item.professionalName}</TableCell><TableCell>{formatSocialDate(item.evaluatedAt)}</TableCell></TableRow>)}</TableBody></Table>}
      </div>
      {query.data && query.data.totalElements > 0 && <DataPagination page={page} totalPages={Math.max(1, query.data.totalPages)} totalItems={query.data.totalElements} pageSize={20} onPageChange={setPage} />}
    </SidebarShellContent>
  </SidebarShell>
}
