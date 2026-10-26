import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { IconRefresh } from "@tabler/icons-react"
import { DataPagination } from "@/components/DataPagination"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { SeverityBadge, SeverityDot } from "@/components/applications/SeverityBadge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatSocialDate, levelLabel, priorityLabel, socialQueries, type Severity } from "@/lib/social-management"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

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
          <div className="space-y-1 text-sm"><Label htmlFor="cases-priority">Prioridad</Label><Select items={[{ value: null, label: "Todas" }, { value: "LOW", label: "Baja" }, { value: "MEDIUM", label: "Media" }, { value: "HIGH", label: "Alta" }, { value: "URGENT", label: "Urgente" }]} value={priority || null} onValueChange={v => { setPriority(v ?? ""); setPage(1) }}><SelectTrigger id="cases-priority" className="min-w-36"><SelectValue>{(v: string | null) => v ? <span className="flex items-center gap-2"><SeverityDot severity={v as Severity} />{priorityLabel[v as Severity & keyof typeof priorityLabel]}</span> : "Todas"}</SelectValue></SelectTrigger><SelectContent>{([{ value: null, label: "Todas" }, { value: "LOW", label: "Baja" }, { value: "MEDIUM", label: "Media" }, { value: "HIGH", label: "Alta" }, { value: "URGENT", label: "Urgente" }]).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.value && <SeverityDot severity={item.value as Severity} />}{item.label}</SelectItem>)}</SelectContent></Select></div>
          <Button type="submit" variant="outline">Buscar</Button><Button type="button" variant="ghost" onClick={() => query.refetch()} disabled={query.isFetching}><IconRefresh /><span className="sr-only sm:not-sr-only">Refrescar</span></Button>
        </form>
        {query.isPending ? <p>Cargando evaluaciones…</p> : query.isError ? <div role="alert" className="space-y-2"><p>No se pudieron cargar las evaluaciones.</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button></div> : !query.data.content.length ? <p className="rounded-xl border p-8 text-center text-muted-foreground">No hay evaluaciones con estos filtros.</p> : <Table><TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Titular</TableHead><TableHead>Programa</TableHead><TableHead>Vulnerabilidad</TableHead><TableHead>Prioridad</TableHead><TableHead>Profesional</TableHead><TableHead>Evaluada</TableHead></TableRow></TableHeader><TableBody>{query.data.content.map(item => <TableRow key={item.id}><TableCell><Link className="font-medium hover:underline" to="/gestion/casos/$solicitudId/evaluacion" params={{ solicitudId: item.applicationId }}>N.º {item.applicationNumber}</Link></TableCell><TableCell>{item.applicantName}</TableCell><TableCell>{item.programName}</TableCell><TableCell><SeverityBadge severity={item.vulnerability}>{levelLabel[item.vulnerability]}</SeverityBadge></TableCell><TableCell><SeverityBadge severity={item.priority}>{priorityLabel[item.priority]}</SeverityBadge></TableCell><TableCell>{item.professionalName}</TableCell><TableCell>{formatSocialDate(item.evaluatedAt)}</TableCell></TableRow>)}</TableBody></Table>}
      </div>
      {query.data && query.data.totalElements > 0 && <DataPagination page={page} totalPages={Math.max(1, query.data.totalPages)} totalItems={query.data.totalElements} pageSize={20} onPageChange={setPage} />}
    </SidebarShellContent>
  </SidebarShell>
}
