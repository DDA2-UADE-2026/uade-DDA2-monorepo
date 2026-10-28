import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, Link } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"
import { toast } from "sonner"
import { ApplicationError, ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { benefitAmount, benefitName, benefitPeriod, BenefitStatusBadge } from "@/components/benefits/BenefitUi"
import { formatInterventionDateTime } from "@/components/interventions/InterventionUi"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { formatRequirementValue, requirementLabels } from "@/components/programs/ProgramRouteUi"
import { UserChip } from "@/components/UserChip"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"
import { finalizeMutation, findAll2Options, get3Options, history1Options, list6Options, renewMutation, suspendMutation } from "@/generated/@tanstack/react-query.gen"
import type { BenefitGrantResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/gestion/beneficios/$beneficioId")({ validateSearch: z.object({ solicitud: z.string().catch("").default("") }), component: BenefitDetail })
function BenefitDetail() {
  const { beneficioId } = Route.useParams()
  const { solicitud } = Route.useSearch()
  const { user } = Route.useRouteContext()
  const [action, setAction] = useState<"renew" | "suspend" | "finalize" | null>(null)
  const grants = useQuery({ ...list6Options({ path: { id: solicitud } }), enabled: !!solicitud })
  const application = useQuery({ ...get3Options({ path: { id: solicitud } }), enabled: !!solicitud })
  const history = useQuery({ ...history1Options({ path: { id: solicitud, grantId: beneficioId } }), enabled: !!solicitud })
  const grant = grants.data?.find((item) => item.id === beneficioId)
  const canManage = user.permissions?.includes("applications:management:benefits:manage") && application.data?.status === "APPROVED"
  return <ApplicationPage breadcrumbs={[{ label: "Beneficios", to: "/gestion/beneficios" }, { label: "Detalle" }]}>
    <ApplicationHeading title="Detalle del beneficio" description="Vigencia, cambios y actuaciones del otorgamiento." />
    {!solicitud ? <p>Falta identificar la solicitud. Volvé al listado para abrir el beneficio.</p> : grants.isPending || application.isPending ? <ApplicationLoading /> : grants.isError ? <ApplicationError error={grants.error} title="No pudimos cargar el beneficio" retry={() => grants.refetch()} /> : application.isError ? <ApplicationError error={application.error} title="No pudimos cargar la solicitud" retry={() => application.refetch()} /> : !grant ? <p>No se encontró el beneficio en esta solicitud.</p> : <>
      <Card><CardHeader><div className="flex flex-wrap items-center justify-between gap-2"><CardTitle>{benefitName(grant.benefitType)}</CardTitle><BenefitStatusBadge status={grant.status} /></div></CardHeader><CardContent className="space-y-3 text-sm"><UserChip user={{ id: application.data.userId, name: application.data.userName, email: application.data.userEmail }} /><p>{application.data.programName} · {application.data.programEditionName}</p><p>Solicitud N.º {application.data.applicationNumber} · {benefitPeriod(grant.startDate, grant.endDate)}</p><p>Monto: {benefitAmount(grant.amount)}</p>{grant.description && <p>{grant.description}</p>}<p className="text-muted-foreground">Otorgado el {formatInterventionDateTime(grant.assignedAt)}</p><UserChip user={{ id: grant.assignedByUserId, name: grant.assignedByUserName }} secondary="Otorgado por" /><Button variant="outline" size="sm" render={<Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: solicitud }} />}>Ver solicitud</Button></CardContent></Card>
      {canManage && (grant.status === "ACTIVE" || grant.status === "SUSPENDED") && <div className="flex flex-wrap gap-2"><Button onClick={() => setAction("renew")}>Renovar / reactivar</Button>{grant.status === "ACTIVE" && <Button variant="outline" onClick={() => setAction("suspend")}>Suspender</Button>}<Button variant="destructive" onClick={() => setAction("finalize")}>Finalizar</Button></div>}
      <section className="space-y-3"><h2 className="font-heading text-lg font-medium">Historial</h2>{history.isPending ? <p>Cargando historial…</p> : history.isError ? <ApplicationError error={history.error} title="No pudimos cargar el historial" retry={() => history.refetch()} /> : <div className="divide-y rounded-xl border">{history.data.map((entry) => <div key={entry.id} className="space-y-2 p-4 text-sm"><div className="flex flex-wrap items-center justify-between gap-2"><span className="font-medium">{{ ASSIGNED: "Otorgado", RENEWED: "Renovado", SUSPENDED: "Suspendido", FINALIZED: "Finalizado", EXPIRED: "Vencido" }[entry.action ?? "ASSIGNED"]} · {formatInterventionDateTime(entry.changedAt)}</span><BenefitStatusBadge status={entry.status} /></div><UserChip user={{ id: entry.responsibleUserId ?? undefined, name: entry.responsibleUserName }} /><p>Vigencia: {benefitPeriod(entry.startDate, entry.endDate)}</p>{entry.reason && <p>Motivo: {entry.reason}</p>}{!!entry.requirementChecks?.length && <ul className="list-disc pl-5 text-muted-foreground">{entry.requirementChecks.map((check) => <li key={check.requirementId}>{check.type ? requirementLabels[check.type] : "Requisito"}: {formatRequirementValue(check.type, check.value)} · {check.fulfilled ? "Cumplido" : "No cumplido"}</li>)}</ul>}</div>)}</div>}</section>
      {action && <BenefitActionDialog action={action} grant={grant} applicationId={solicitud} editionId={application.data.programEditionId ?? ""} onClose={() => setAction(null)} />}
    </>}
  </ApplicationPage>
}

function BenefitActionDialog({ action, grant, applicationId, editionId, onClose }: { action: "renew" | "suspend" | "finalize"; grant: BenefitGrantResponse; applicationId: string; editionId: string; onClose: () => void }) {
  const [reason, setReason] = useState("")
  const [endDate, setEndDate] = useState("")
  const [checked, setChecked] = useState<string[]>([])
  const queryClient = useQueryClient()
  const requirements = useQuery({ ...findAll2Options({ path: { editionId } }), enabled: action === "renew" && !!editionId })
  const renew = useMutation(renewMutation())
  const suspend = useMutation(suspendMutation())
  const finalize = useMutation(finalizeMutation())
  const pending = renew.isPending || suspend.isPending || finalize.isPending
  const error = renew.error ?? suspend.error ?? finalize.error
  const title = action === "renew" ? "Renovar o reactivar" : action === "suspend" ? "Suspender" : "Finalizar"
  const items = requirements.data ?? []
  const submit = async () => {
    if (!grant.id) return
    const path = { id: applicationId, grantId: grant.id }
    try {
      if (action === "renew") await renew.mutateAsync({ path, body: { reason: reason.trim(), endDate, requirementChecks: items.map((item) => ({ requirementId: item.id!, fulfilled: checked.includes(item.id!) })) } })
      else if (action === "suspend") await suspend.mutateAsync({ path, body: { reason: reason.trim() } })
      else await finalize.mutateAsync({ path, body: { reason: reason.trim() } })
      await queryClient.invalidateQueries()
      toast.success("Beneficio actualizado")
      onClose()
    } catch { /* El error de la API se muestra en el diálogo. */ }
  }
  return <Dialog open onOpenChange={(open) => { if (!open && !pending) onClose() }}><DialogContent className="max-h-[90vh] overflow-y-auto"><DialogHeader><DialogTitle>{title} beneficio</DialogTitle><DialogDescription>{action === "finalize" ? "La finalización es definitiva y puede cerrar la solicitud si no quedan otros beneficios vigentes." : action === "renew" ? "Confirmá la nueva vigencia y los requisitos actuales. La renovación está sujeta a las validaciones de la solicitud." : "El beneficio quedará suspendido hasta una posible renovación."}</DialogDescription></DialogHeader>
    <label className="grid gap-2 text-sm">Motivo<Textarea value={reason} maxLength={1000} onChange={(event) => setReason(event.target.value)} /></label>
    {action === "renew" && <><label className="grid gap-2 text-sm">Nueva fecha de fin<ProgramDatePicker value={endDate} onChange={setEndDate} /></label><div className="space-y-2 text-sm"><p className="font-medium">Requisitos vigentes</p>{requirements.isPending ? <p>Cargando requisitos…</p> : requirements.isError ? <p className="text-destructive">No se pudieron cargar los requisitos. Cerrá y volvé a intentar.</p> : items.length === 0 ? <p className="text-muted-foreground">Sin requisitos configurados.</p> : items.map((item) => <label key={item.id} className="flex items-start gap-2"><Checkbox checked={!!item.id && checked.includes(item.id)} onCheckedChange={(value) => { if (item.id) setChecked((current) => value ? [...current, item.id!] : current.filter((id) => id !== item.id)) }} /><span>{item.type ? requirementLabels[item.type] : "Requisito"}: {formatRequirementValue(item.type, item.value)} {item.description && `· ${item.description}`}</span></label>)}</div></>}
    {error && <p role="alert" className="text-sm text-destructive">{error.message ?? "No se pudo actualizar el beneficio."}</p>}
    <DialogFooter><Button variant="outline" disabled={pending} onClick={onClose}>Cancelar</Button><Button variant={action === "finalize" ? "destructive" : "default"} disabled={pending || !reason.trim() || (action === "renew" && (!editionId || !endDate || !requirements.isSuccess || items.some((item) => !item.id || !checked.includes(item.id))))} onClick={submit}>{pending ? "Guardando…" : title}</Button></DialogFooter>
  </DialogContent></Dialog>
}
