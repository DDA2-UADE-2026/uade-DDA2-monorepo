import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { toast } from "sonner"
import { get3Options } from "@/generated/@tanstack/react-query.gen"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { SeverityBadge, SeverityDot } from "@/components/applications/SeverityBadge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Textarea } from "@/components/ui/textarea"
import { apiErrorMessage, formatSocialDate, invalidateSocialQueries, levelLabel, priorityLabel, socialManagement, socialQueries, type Level, type Priority, type Severity } from "@/lib/social-management"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export const Route = createFileRoute(
  "/_app/gestion/casos/$solicitudId/evaluacion",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useParams()
  const { user } = Route.useRouteContext()
  const canView = user.permissions?.includes("applications:evaluations:view")
  const canCreate = user.permissions?.includes("applications:evaluations:create")
  const application = useQuery(get3Options({ path: { id: solicitudId } }))
  const evaluations = useQuery({ ...socialQueries.evaluations({ applicationId: solicitudId, page: 0, size: 1 }), enabled: !!canView })
  const existingId = evaluations.data?.content[0]?.id
  const detail = useQuery({ ...socialQueries.evaluation(existingId ?? ""), enabled: !!existingId })
  const queryClient = useQueryClient()
  const [vulnerability, setVulnerability] = useState<Level>("MEDIUM")
  const [priority, setPriority] = useState<Priority>("MEDIUM")
  const [recommendation, setRecommendation] = useState("")
  const mutation = useMutation({
    mutationFn: () => socialManagement.createEvaluation(solicitudId, { vulnerability, priority, recommendation: recommendation.trim() }),
    onSuccess: async () => { toast.success("Evaluación registrada"); await queryClient.invalidateQueries(invalidateSocialQueries("social-evaluations")); await queryClient.invalidateQueries({ queryKey: get3Options({ path: { id: solicitudId } }).queryKey }) },
    onError: error => toast.error(apiErrorMessage(error)),
  })
  const evaluation = detail.data
  return <SidebarShell>
    <OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={[{ label: "Evaluaciones", to: "/gestion/casos" }, { label: "Solicitud" }]} /></OutletNavSticky>
    <SidebarShellContent><div className="min-h-0 flex-1 overflow-auto p-4 lg:p-6"><div className="mx-auto max-w-3xl space-y-5">
      <header className="space-y-1"><h1 className="font-heading text-2xl font-medium">Evaluación social</h1><p className="text-sm text-muted-foreground">Solicitud N.º {application.data?.applicationNumber ?? solicitudId} · {application.data?.userName ?? ""}</p><div className="flex gap-2 pt-2"><Button variant="outline" size="sm" render={<Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId }} />}>Ver solicitud</Button><Button variant="outline" size="sm" render={<Link to="/gestion/casos/$solicitudId/visitas" params={{ solicitudId }} />}>Ver visitas</Button></div></header>
      {application.isError && <p role="alert" className="text-destructive">No se pudo consultar la solicitud.</p>}
      {!canView && !canCreate && <p role="alert">Tu rol no tiene permisos para consultar ni registrar evaluaciones.</p>}
      {canView && evaluations.isPending && <p>Cargando evaluación…</p>}
      {canView && evaluations.isError && <div role="alert" className="space-y-2"><p>No se pudo consultar la evaluación.</p><Button variant="outline" onClick={() => evaluations.refetch()}>Reintentar</Button></div>}
      {existingId && detail.isPending && <p>Cargando detalle…</p>}
      {detail.isError && <div role="alert" className="space-y-2"><p>No se pudo consultar el detalle.</p><Button variant="outline" onClick={() => detail.refetch()}>Reintentar</Button></div>}
      {evaluation && <Card><CardHeader><CardTitle>Evaluación registrada</CardTitle></CardHeader><CardContent className="space-y-4 text-sm"><div className="flex flex-wrap gap-2"><SeverityBadge severity={evaluation.vulnerability}>Vulnerabilidad {levelLabel[evaluation.vulnerability]}</SeverityBadge><SeverityBadge severity={evaluation.priority}>Prioridad {priorityLabel[evaluation.priority]}</SeverityBadge></div><p><span className="text-muted-foreground">Profesional:</span> {evaluation.professionalName}</p><p><span className="text-muted-foreground">Fecha:</span> {formatSocialDate(evaluation.evaluatedAt)}</p><div><p className="text-muted-foreground">Recomendación</p><p className="whitespace-pre-wrap">{evaluation.recommendation}</p></div></CardContent></Card>}
      {canCreate && !mutation.isSuccess && (!canView || (evaluations.isSuccess && !existingId)) && <Card><CardHeader><CardTitle>Registrar evaluación</CardTitle></CardHeader><CardContent><form className="space-y-4" onSubmit={e => { e.preventDefault(); mutation.mutate() }}><div className="grid gap-4 sm:grid-cols-2"><div className="space-y-1 text-sm"><Label htmlFor="evaluation-vulnerability">Vulnerabilidad</Label><Select items={(Object.keys(levelLabel) as Level[]).map(value => ({ value, label: levelLabel[value] }))} value={vulnerability} onValueChange={v => v && setVulnerability(v as Level)}><SelectTrigger id="evaluation-vulnerability" className="w-full"><SelectValue>{(v: string | null) => v ? <span className="flex items-center gap-2"><SeverityDot severity={v as Severity} />{levelLabel[v as Severity & keyof typeof levelLabel]}</span> : ""}</SelectValue></SelectTrigger><SelectContent>{((Object.keys(levelLabel) as Level[]).map(value => ({ value, label: levelLabel[value] }))).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.value && <SeverityDot severity={item.value as Severity} />}{item.label}</SelectItem>)}</SelectContent></Select></div><div className="space-y-1 text-sm"><Label htmlFor="evaluation-priority">Prioridad</Label><Select items={(Object.keys(priorityLabel) as Priority[]).map(value => ({ value, label: priorityLabel[value] }))} value={priority} onValueChange={v => v && setPriority(v as Priority)}><SelectTrigger id="evaluation-priority" className="w-full"><SelectValue>{(v: string | null) => v ? <span className="flex items-center gap-2"><SeverityDot severity={v as Severity} />{priorityLabel[v as Severity & keyof typeof priorityLabel]}</span> : ""}</SelectValue></SelectTrigger><SelectContent>{((Object.keys(priorityLabel) as Priority[]).map(value => ({ value, label: priorityLabel[value] }))).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.value && <SeverityDot severity={item.value as Severity} />}{item.label}</SelectItem>)}</SelectContent></Select></div></div><label className="block space-y-1 text-sm">Recomendación<Textarea required minLength={1} maxLength={1000} value={recommendation} onChange={e => setRecommendation(e.target.value)} placeholder="Describí la recomendación profesional" /></label><Button type="submit" disabled={mutation.isPending || !recommendation.trim() || !application.data || !["SUBMITTED", "IN_VALIDATION", "PENDING_DOCUMENTATION", "IN_EVALUATION"].includes(application.data.status ?? "") || (application.data.assignedWorkerUserId != null && application.data.assignedWorkerUserId !== user.id)}> {mutation.isPending ? "Guardando…" : "Registrar evaluación"}</Button>{application.data && (!["SUBMITTED", "IN_VALIDATION", "PENDING_DOCUMENTATION", "IN_EVALUATION"].includes(application.data.status ?? "") || (application.data.assignedWorkerUserId != null && application.data.assignedWorkerUserId !== user.id)) && <p className="text-xs text-muted-foreground">El estado o la asignación actual de la solicitud no permiten registrar la evaluación.</p>}</form></CardContent></Card>}
    </div></div></SidebarShellContent>
  </SidebarShell>
}
