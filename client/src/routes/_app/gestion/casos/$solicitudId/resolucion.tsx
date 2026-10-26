import { IconCircleCheck, IconCircleX, IconGavel } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { toast } from "sonner"

import { ApplicationProgramContext } from "@/components/applications/ApplicationProgramContext"
import { ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { SeverityBadge } from "@/components/applications/SeverityBadge"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { UserChip } from "@/components/UserChip"
import { get3Options, get3QueryKey, resolveApplicationMutation } from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationResponse } from "@/generated/types.gen"
import { formatApplicationDateTime } from "@/lib/application-flow"
import { invalidateSocialQueries, levelLabel, priorityLabel, socialQueries } from "@/lib/social-management"
import { cn } from "@/lib/utils"

type Decision = "APPROVED" | "WAITLISTED" | "REJECTED"

const DECISIONS: Record<Decision, { label: string; confirm: string; description: string }> = {
  APPROVED: {
    label: "Aprobar",
    confirm: "¿Aprobar la solicitud?",
    description: "La persona ingresa al programa y ocupa un cupo de la edición. La decisión es definitiva.",
  },
  WAITLISTED: {
    label: "Lista de espera",
    confirm: "¿Dejar la solicitud en lista de espera?",
    description: "Cumple las condiciones pero no ingresa todavía. Más adelante vas a poder aprobarla o rechazarla.",
  },
  REJECTED: {
    label: "Rechazar",
    confirm: "¿Rechazar la solicitud?",
    description: "La persona no ingresa al programa y queda habilitada para presentarse en otra convocatoria. La decisión es definitiva.",
  },
}

const RESOLVED_LABELS: Record<Decision, string> = { APPROVED: "aprobada", WAITLISTED: "en lista de espera", REJECTED: "rechazada" }

export const Route = createFileRoute(
  "/_app/gestion/casos/$solicitudId/resolucion",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useParams()
  const { user } = Route.useRouteContext()
  const permissions = user.permissions ?? []
  const application = useQuery(get3Options({ path: { id: solicitudId } }))
  const app = application.data

  return <SidebarShell>
    <OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={[{ label: "Evaluaciones", to: "/gestion/casos" }, { label: "Solicitud", to: `/gestion/casos/${solicitudId}/evaluacion` }, { label: "Resolución" }]} /></OutletNavSticky>
    <SidebarShellContent><div className="min-h-0 flex-1 overflow-auto p-4 lg:p-6"><div className="mx-auto max-w-3xl space-y-5">
      <header className="space-y-1">
        <h1 className="font-heading text-2xl font-medium">Resolución</h1>
        <p className="text-sm text-muted-foreground">Solicitud N.º {app?.applicationNumber ?? solicitudId} · {app?.userName ?? ""}</p>
        <div className="flex flex-wrap gap-2 pt-2">
          <Button variant="outline" size="sm" render={<Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId }} />}>Ver solicitud</Button>
          <Button variant="outline" size="sm" render={<Link to="/gestion/casos/$solicitudId/evaluacion" params={{ solicitudId }} />}>Evaluación</Button>
          <Button variant="outline" size="sm" render={<Link to="/gestion/casos/$solicitudId/visitas" params={{ solicitudId }} />}>Visitas</Button>
        </div>
      </header>
      {application.isPending && <p className="text-sm text-muted-foreground">Cargando solicitud…</p>}
      {application.isError && <div role="alert" className="space-y-2"><p className="text-destructive">No se pudo consultar la solicitud.</p><Button variant="outline" onClick={() => application.refetch()}>Reintentar</Button></div>}
      {app && <>
        <ApplicationProgramContext application={app} />
        {app.resolvedAt && <ResolutionResult application={app} />}
        <ResolutionPanel application={app} permissions={permissions} currentUserId={user.id} />
      </>}
    </div></div></SidebarShellContent>
  </SidebarShell>
}

function ResolutionResult({ application }: { application: AdminApplicationResponse }) {
  return <Card>
    <CardHeader>
      <CardTitle className="flex flex-wrap items-center gap-2">Resultado <ApplicationStatusBadge status={application.status} /></CardTitle>
      <CardDescription>Resuelta el {formatApplicationDateTime(application.resolvedAt ?? undefined)}</CardDescription>
    </CardHeader>
    <CardContent className="space-y-4 text-sm">
      {application.resolvedByUserId != null && <div className="space-y-1"><p className="text-muted-foreground">Resolvió</p><UserChip user={{ id: application.resolvedByUserId, name: application.resolvedByName }} /></div>}
      <div className="space-y-1"><p className="text-muted-foreground">Motivo informado al titular</p><p className="whitespace-pre-wrap">{application.resolutionReason || "Sin motivo informado."}</p></div>
    </CardContent>
  </Card>
}

function ResolutionPanel({ application, permissions, currentUserId }: { application: AdminApplicationResponse; permissions: string[]; currentUserId?: number }) {
  const queryClient = useQueryClient()
  const id = application.id!
  const canViewEvaluations = permissions.includes("applications:evaluations:view")
  const canViewVisits = permissions.includes("applications:visits:view")
  const evaluations = useQuery({ ...socialQueries.evaluations({ applicationId: id, page: 0, size: 1 }), enabled: canViewEvaluations })
  const evaluationId = evaluations.data?.content[0]?.id
  const evaluation = useQuery({ ...socialQueries.evaluation(evaluationId ?? ""), enabled: !!evaluationId })
  const scheduledVisits = useQuery({ ...socialQueries.visits({ applicationId: id, status: "SCHEDULED", page: 0, size: 1 }), enabled: canViewVisits })
  const [decision, setDecision] = useState<Decision | null>(null)
  const [reason, setReason] = useState("")
  const [confirming, setConfirming] = useState(false)
  const resolve = useMutation({
    ...resolveApplicationMutation(),
    onSuccess: async (data) => {
      queryClient.setQueryData(get3QueryKey({ path: { id } }), data)
      setConfirming(false)
      setDecision(null)
      setReason("")
      await queryClient.invalidateQueries(invalidateSocialQueries("social-evaluations"))
      toast.success(`Solicitud ${RESOLVED_LABELS[data.status as Decision] ?? "resuelta"}`)
    },
    onError: (error) => { setConfirming(false); showApiErrorToast(error) },
  })

  const waitlisted = application.status === "WAITLISTED"
  const resolvable = application.status === "IN_EVALUATION" || waitlisted
  if (!resolvable) {
    if (application.resolvedAt) return null
    return <Card size="sm"><CardHeader><CardTitle>Todavía no se puede resolver</CardTitle><CardDescription>
      {application.status === "IN_VISIT"
        ? "La solicitud tiene visitas o entrevistas en curso. Cuando se registre el resultado de la última, vuelve a evaluación."
        : "La solicitud tiene que pasar por la evaluación social antes de resolverse."}
    </CardDescription></CardHeader></Card>
  }

  const hasPermission = permissions.includes("applications:resolutions:create")
  const isAssigned = application.assignedWorkerUserId != null && application.assignedWorkerUserId === currentUserId
  const isOwn = application.userId === currentUserId
  const pendingDocuments = application.pendingDocuments?.length ?? 0
  const evaluated = canViewEvaluations ? (evaluations.isSuccess ? !!evaluationId : undefined) : undefined
  const visitsScheduled = canViewVisits ? (scheduledVisits.isSuccess ? scheduledVisits.data.totalElements > 0 : undefined) : undefined
  const options: Decision[] = waitlisted ? ["APPROVED", "REJECTED"] : ["APPROVED", "WAITLISTED", "REJECTED"]
  const reasonRequired = decision !== null && decision !== "APPROVED"
  const blocked = !hasPermission || !isAssigned || isOwn || evaluated === false || visitsScheduled === true
  const approveBlocked = pendingDocuments > 0
  const canSubmit = !blocked && decision !== null && !(decision === "APPROVED" && approveBlocked)
    && (!reasonRequired || reason.trim().length > 0) && reason.trim().length <= 1000 && !resolve.isPending

  return <>
    {evaluation.data && <Card size="sm">
      <CardHeader><CardTitle>Evaluación social</CardTitle><CardDescription>Registrada por {evaluation.data.professionalName} el {formatApplicationDateTime(evaluation.data.evaluatedAt)}</CardDescription></CardHeader>
      <CardContent className="space-y-3 text-sm">
        <div className="flex flex-wrap gap-2"><SeverityBadge severity={evaluation.data.vulnerability}>Vulnerabilidad {levelLabel[evaluation.data.vulnerability]}</SeverityBadge><SeverityBadge severity={evaluation.data.priority}>Prioridad {priorityLabel[evaluation.data.priority]}</SeverityBadge></div>
        <p className="whitespace-pre-wrap">{evaluation.data.recommendation}</p>
      </CardContent>
    </Card>}

    <Card size="sm">
      <CardHeader><CardTitle>Antes de resolver</CardTitle><CardDescription>Condiciones que valida el sistema al registrar la decisión.</CardDescription></CardHeader>
      <CardContent>
        <ul className="space-y-2 text-sm">
          <Check ok={hasPermission} label="Tu rol puede resolver solicitudes" />
          <Check ok={isAssigned} label={isAssigned ? "Sos el trabajador asignado" : `Solo resuelve el trabajador asignado${application.assignedWorkerName ? ` (${application.assignedWorkerName})` : ""}`} />
          {isOwn && <Check ok={false} label="No podés resolver una solicitud propia" />}
          {evaluated !== undefined && <Check ok={evaluated} label={evaluated ? "Evaluación social registrada" : "Falta registrar la evaluación social"} />}
          {visitsScheduled !== undefined && <Check ok={!visitsScheduled} label={visitsScheduled ? "Hay visitas programadas: cancelalas o registrá su resultado" : "Sin visitas programadas"} />}
          <Check ok={pendingDocuments === 0} warning label={pendingDocuments === 0 ? "Documentación obligatoria completa" : `${pendingDocuments} documento${pendingDocuments === 1 ? "" : "s"} obligatorio${pendingDocuments === 1 ? "" : "s"} faltante${pendingDocuments === 1 ? "" : "s"} u observado${pendingDocuments === 1 ? "" : "s"}: no se puede aprobar`} />
        </ul>
      </CardContent>
    </Card>

    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2"><IconGavel className="size-5" />{waitlisted ? "Resolver lista de espera" : "Registrar resolución"}</CardTitle>
        <CardDescription>El titular verá el resultado y el motivo en el portal.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); if (canSubmit) setConfirming(true) }}>
          <div className="space-y-1.5">
            <Label id="resolution-decision-label">Decisión</Label>
            <ToggleGroup
              variant="outline"
              aria-labelledby="resolution-decision-label"
              disabled={blocked || resolve.isPending}
              value={decision ? [decision] : []}
              onValueChange={(value: string[]) => setDecision((value[0] as Decision | undefined) ?? null)}
            >
              {options.map((option) => <ToggleGroupItem key={option} value={option} disabled={option === "APPROVED" && approveBlocked}>{DECISIONS[option].label}</ToggleGroupItem>)}
            </ToggleGroup>
            {decision && <p className="text-xs text-muted-foreground">{DECISIONS[decision].description}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="resolution-reason">Motivo {reasonRequired ? "(obligatorio)" : "(opcional)"}</Label>
            <Textarea id="resolution-reason" maxLength={1000} rows={4} value={reason} disabled={blocked || resolve.isPending} onChange={(event) => setReason(event.target.value)} placeholder="Explicá la decisión con lenguaje claro: lo va a leer el titular." />
            <p className="text-right text-xs text-muted-foreground">{reason.length}/1000</p>
          </div>
          <Button type="submit" disabled={!canSubmit}>{resolve.isPending ? "Guardando…" : "Registrar resolución"}</Button>
        </form>
      </CardContent>
    </Card>

    <AlertDialog open={confirming} onOpenChange={(open) => { if (!resolve.isPending) setConfirming(open) }}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{decision ? DECISIONS[decision].confirm : ""}</AlertDialogTitle>
          <AlertDialogDescription>{decision ? DECISIONS[decision].description : ""}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={resolve.isPending}>Volver</AlertDialogCancel>
          <AlertDialogAction
            type="button"
            variant={decision === "REJECTED" ? "destructive" : "default"}
            disabled={resolve.isPending}
            onClick={() => { if (decision) resolve.mutate({ path: { id }, body: { decision, reason: reason.trim() || undefined } }) }}
          >
            {resolve.isPending ? "Guardando…" : decision ? DECISIONS[decision].label : "Confirmar"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </>
}

function Check({ ok, label, warning = false }: { ok: boolean; label: string; warning?: boolean }) {
  const Icon = ok ? IconCircleCheck : IconCircleX
  return <li className="flex items-start gap-2">
    <Icon aria-hidden="true" className={cn("mt-0.5 size-4 shrink-0", ok ? "text-emerald-600 dark:text-emerald-400" : warning ? "text-amber-600 dark:text-amber-400" : "text-destructive")} />
    <span>{label}</span>
  </li>
}
