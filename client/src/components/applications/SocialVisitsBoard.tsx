import { FilterActions, FilterBar, FilterField } from "@/components/FilterBar"
import { DateTimePicker } from "@/components/DateTimePicker"
import { IconRefresh } from "@tabler/icons-react"
import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { toast } from "sonner"
import { DataPagination } from "@/components/DataPagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { apiErrorMessage, dateTimeLocal, formatSocialDate, invalidateSocialQueries, socialManagement, socialQueries, visitStatusLabel, visitTypeLabel, type VisitStatus, type VisitType } from "@/lib/social-management"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { UserChip } from "@/components/UserChip"

type Props = { applicationId?: string; permissions: string[]; currentUserId?: number; canSchedule?: boolean }
const newSchedule = () => ({ startsAt: "", durationMinutes: 60, location: "" })

export function SocialVisitsBoard({ applicationId, permissions, canSchedule = true }: Props) {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState("")
  const [number, setNumber] = useState("")
  const [appliedNumber, setAppliedNumber] = useState("")
  const [type, setType] = useState<VisitType>("VISIT")
  const [schedule, setSchedule] = useState(newSchedule)
  const canView = permissions.includes("applications:visits:view")
  const canCreate = permissions.includes("applications:visits:create")
  const list = useQuery({ ...socialQueries.visits({ applicationId, page: page - 1, size: 20, status: status || undefined, applicationNumber: appliedNumber || undefined }), enabled: canView })
  const create = useMutation({
    mutationFn: () => socialManagement.createVisit(applicationId!, { type, startsAt: new Date(schedule.startsAt).toISOString(), durationMinutes: schedule.durationMinutes, location: schedule.location.trim() }),
    onSuccess: async () => { toast.success("Actuación programada"); setSchedule(newSchedule()); await queryClient.invalidateQueries(invalidateSocialQueries("social-visits")); await queryClient.invalidateQueries(invalidateSocialQueries("social-evaluations")) },
    onError: error => toast.error(apiErrorMessage(error)),
  })
  return <div className="space-y-5">
    {applicationId && canCreate && canSchedule && <Card><CardHeader><CardTitle>Programar visita o entrevista</CardTitle></CardHeader><CardContent><form className="grid gap-3 sm:grid-cols-2" onSubmit={e => { e.preventDefault(); create.mutate() }}>
      <div className="space-y-1 text-sm"><Label htmlFor="visit-type">Tipo</Label><Select items={[{ value: "VISIT", label: "Visita" }, { value: "INTERVIEW", label: "Entrevista" }]} value={type} onValueChange={v => v && setType(v as VisitType)}><SelectTrigger id="visit-type" className="w-full"><SelectValue /></SelectTrigger><SelectContent>{([{ value: "VISIT", label: "Visita" }, { value: "INTERVIEW", label: "Entrevista" }]).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.label}</SelectItem>)}</SelectContent></Select></div>
      <div className="space-y-1 text-sm"><Label htmlFor="visit-starts-at">Inicio</Label><DateTimePicker id="visit-starts-at" required timeLabel="Hora de inicio" min={dateTimeLocal(new Date().toISOString())} value={schedule.startsAt} onChange={startsAt => setSchedule({ ...schedule, startsAt })} /></div>
      <label className="space-y-1 text-sm">Duración en minutos<Input required type="number" min={1} value={schedule.durationMinutes} onChange={e => setSchedule({ ...schedule, durationMinutes: Number(e.target.value) })} /></label>
      <label className="space-y-1 text-sm">Lugar<Input required minLength={1} maxLength={300} value={schedule.location} onChange={e => setSchedule({ ...schedule, location: e.target.value })} /></label>
      <div className="sm:col-span-2"><Button type="submit" disabled={create.isPending || !schedule.startsAt || !schedule.location.trim() || schedule.durationMinutes < 1}>{create.isPending ? "Programando…" : "Programar actuación"}</Button></div>
    </form></CardContent></Card>}
    {!canView ? <p role="alert">Tu rol no tiene permiso para consultar visitas.</p> : <>
      {!applicationId && <FilterBar label="Filtrar visitas" onSubmit={() => { setPage(1); setAppliedNumber(number.trim()) }}><FilterField label="N.º de solicitud" htmlFor="visit-number-filter"><Input id="visit-number-filter" type="number" inputMode="numeric" min={1} placeholder="Ej. 121" value={number} onChange={e => setNumber(e.target.value)} /></FilterField><FilterField label="Estado" htmlFor="visit-status-filter"><Select items={[{ value: null, label: "Todos" }, ...(Object.keys(visitStatusLabel) as VisitStatus[]).map(value => ({ value, label: visitStatusLabel[value] }))]} value={status || null} onValueChange={v => { setStatus(v ?? ""); setPage(1) }}><SelectTrigger id="visit-status-filter" className="w-full"><SelectValue /></SelectTrigger><SelectContent>{([{ value: null, label: "Todos" }, ...(Object.keys(visitStatusLabel) as VisitStatus[]).map(value => ({ value, label: visitStatusLabel[value] }))]).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.label}</SelectItem>)}</SelectContent></Select></FilterField><FilterActions><Button type="submit" variant="outline">Buscar</Button><Button type="button" variant="ghost" onClick={() => list.refetch()} disabled={list.isFetching}><IconRefresh className={list.isFetching ? "animate-spin" : undefined} /><span className="sr-only sm:not-sr-only">Refrescar</span></Button></FilterActions></FilterBar>}
      {list.isPending ? <p>Cargando actuaciones…</p> : list.isError ? <div role="alert" className="space-y-2"><p>No se pudieron cargar las actuaciones.</p><Button variant="outline" onClick={() => list.refetch()}>Reintentar</Button></div> : !list.data.content.length ? <p className="rounded-xl border p-8 text-center text-muted-foreground">No hay visitas o entrevistas con estos filtros.</p> : <Table><TableHeader><TableRow>{!applicationId && <TableHead>Solicitud</TableHead>}<TableHead>Tipo</TableHead><TableHead>Inicio</TableHead><TableHead>Titular</TableHead><TableHead>Responsable</TableHead><TableHead>Estado</TableHead><TableHead>Acciones</TableHead></TableRow></TableHeader><TableBody>{list.data.content.map(visit => <TableRow key={visit.id}>{!applicationId && <TableCell><Link className="hover:underline" to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: visit.applicationId }}>N.º {visit.applicationNumber}</Link></TableCell>}<TableCell>{visitTypeLabel[visit.type]}</TableCell><TableCell>{formatSocialDate(visit.startsAt)}</TableCell><TableCell className="min-w-44"><UserChip user={{ id: visit.applicantUserId, name: visit.applicantName, email: visit.applicantEmail }} /></TableCell><TableCell className="min-w-44"><UserChip user={{ id: visit.responsibleId, name: visit.responsibleName, email: visit.responsibleEmail }} /></TableCell><TableCell><Badge variant="outline">{visitStatusLabel[visit.status]}</Badge>{visit.latestRescheduleRequest?.status === "PENDING" && <Badge className="ml-1" variant="secondary">Pedido pendiente</Badge>}</TableCell><TableCell><Button size="xs" variant="outline" render={<Link to="/gestion/visitas/$visitaId" params={{ visitaId: visit.id }} />}>Ver detalle</Button></TableCell></TableRow>)}</TableBody></Table>}
      {list.data && list.data.totalElements > 0 && <DataPagination page={page} totalPages={Math.max(1, list.data.totalPages)} totalItems={list.data.totalElements} pageSize={20} onPageChange={setPage} />}
    </>}
  </div>
}
