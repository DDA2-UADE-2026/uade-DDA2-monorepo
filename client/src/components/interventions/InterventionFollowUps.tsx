import { IconAlertTriangle, IconEye, IconEyeOff, IconMessagePlus, IconPencil } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"

import { formatInterventionDate, formatInterventionDateTime, InterventionStatusBadge, todayLocal } from "@/components/interventions/InterventionUi"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { UserChip } from "@/components/UserChip"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import {
  followUpMutation,
  get4QueryKey,
  historyOptions,
  historyQueryKey,
  publishMutation,
  withdrawMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { InterventionActionResponse, InterventionFollowUpResponse } from "@/generated/types.gen"

type Props = {
  applicationId: string
  action: InterventionActionResponse
  canCreate: boolean
  canPublish: boolean
  onClose: () => void
}

export function InterventionFollowUps({ applicationId, action, canCreate, canPublish, onClose }: Props) {
  const queryClient = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [publication, setPublication] = useState<InterventionFollowUpResponse | null>(null)
  const history = useQuery(historyOptions({ path: { id: applicationId, actionId: action.id ?? "" } }))
  const refresh = () => queryClient.invalidateQueries({ queryKey: historyQueryKey({ path: { id: applicationId, actionId: action.id ?? "" } }) })

  return <>
    <Dialog open onOpenChange={(open) => { if (!open) onClose() }}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-5xl">
        <DialogHeader>
          <DialogTitle>Seguimientos de la acción</DialogTitle>
          <DialogDescription>{action.description}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-muted/30 p-4">
          <div className="flex items-center gap-2"><InterventionStatusBadge status={action.status} /><span className="text-sm text-muted-foreground">{formatInterventionDate(action.startDate)} — {formatInterventionDate(action.dueDate)}</span></div>
          {canCreate && action.status !== "COMPLETED" && <Button size="sm" onClick={() => setShowForm(true)}><IconMessagePlus />Registrar seguimiento</Button>}
        </div>
        {history.isPending ? <div className="space-y-3"><Skeleton className="h-16 w-full" /><Skeleton className="h-16 w-full" /></div> : history.isError ? <Alert variant="destructive"><IconAlertTriangle /><AlertTitle>No se pudo cargar el historial</AlertTitle><AlertDescription className="flex flex-col items-start gap-3"><span>{history.error.message}</span><Button variant="outline" size="sm" onClick={() => history.refetch()}>Reintentar</Button></AlertDescription></Alert> : !history.data.length ? <Empty className="min-h-48 border"><EmptyHeader><EmptyMedia variant="icon"><IconMessagePlus /></EmptyMedia><EmptyTitle>Sin seguimientos</EmptyTitle><EmptyDescription>La acción todavía no tiene avances registrados.</EmptyDescription></EmptyHeader></Empty> : <div className="overflow-hidden rounded-xl border"><Table>
          <TableHeader><TableRow><TableHead>#</TableHead><TableHead>Fecha real</TableHead><TableHead>Estado</TableHead><TableHead className="min-w-72">Notas y resultado</TableHead><TableHead>Autor</TableHead><TableHead>Visibilidad</TableHead><TableHead className="text-right">Acciones</TableHead></TableRow></TableHeader>
          <TableBody>{history.data.map((entry) => <TableRow key={entry.id}>
            <TableCell className="font-medium">{entry.sequenceNumber}</TableCell>
            <TableCell>{formatInterventionDate(entry.occurredOn)}</TableCell>
            <TableCell><InterventionStatusBadge status={entry.status} /></TableCell>
            <TableCell><p>{entry.notes}</p>{entry.result && <p className="mt-1 text-xs text-muted-foreground"><span className="font-medium text-foreground">Resultado:</span> {entry.result}</p>}</TableCell>
            <TableCell className="min-w-44"><UserChip user={{ id: entry.authorUserId, name: entry.authorUserName }} secondary={formatInterventionDateTime(entry.recordedAt)} /></TableCell>
            <TableCell>{entry.citizenPublication?.visible ? <Badge variant="secondary"><IconEye />Visible</Badge> : entry.citizenPublication ? <Badge variant="outline"><IconEyeOff />Retirada</Badge> : <span className="text-sm text-muted-foreground">Privada</span>}</TableCell>
            <TableCell className="text-right">{canPublish && entry.id && <div className="flex justify-end gap-1">
              <Button variant="ghost" size="icon-sm" title={entry.citizenPublication ? "Editar publicación" : "Publicar resumen"} onClick={() => setPublication(entry)}>{entry.citizenPublication ? <IconPencil /> : <IconEye />}</Button>
              {entry.citizenPublication?.visible && <WithdrawButton applicationId={applicationId} actionId={action.id!} followUp={entry} onSuccess={refresh} />}
            </div>}</TableCell>
          </TableRow>)}</TableBody>
        </Table></div>}
        <DialogFooter><DialogClose render={<Button variant="outline" />}>Cerrar</DialogClose></DialogFooter>
      </DialogContent>
    </Dialog>
    {showForm && <FollowUpForm applicationId={applicationId} action={action} onClose={() => setShowForm(false)} onSuccess={() => { setShowForm(false); refresh(); queryClient.invalidateQueries({ queryKey: get4QueryKey({ path: { id: applicationId } }) }) }} />}
    {publication && <PublicationForm applicationId={applicationId} actionId={action.id!} followUp={publication} onClose={() => setPublication(null)} onSuccess={() => { setPublication(null); refresh() }} />}
  </>
}

function FollowUpForm({ applicationId, action, onClose, onSuccess }: { applicationId: string; action: InterventionActionResponse; onClose: () => void; onSuccess: () => void }) {
  const [status, setStatus] = useState<"IN_PROGRESS" | "COMPLETED">("IN_PROGRESS")
  const [occurredOn, setOccurredOn] = useState(todayLocal())
  const [notes, setNotes] = useState("")
  const [result, setResult] = useState("")
  const create = useMutation({ ...followUpMutation(), onSuccess: () => { toast.success("Seguimiento registrado"); onSuccess() } })
  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    create.mutate({ path: { id: applicationId, actionId: action.id! }, body: { status, occurredOn, notes: notes.trim(), result: result.trim() || undefined } })
  }
  return <Dialog open onOpenChange={(open) => { if (!open) onClose() }}><DialogContent className="sm:max-w-xl">
    <DialogHeader><DialogTitle>Registrar seguimiento</DialogTitle><DialogDescription>El registro profesional es inmutable. Al completar la acción, no podrá reabrirse.</DialogDescription></DialogHeader>
    {create.error && <Alert variant="destructive"><IconAlertTriangle /><AlertTitle>No se pudo registrar</AlertTitle><AlertDescription>{create.error.message}</AlertDescription></Alert>}
    <form id="follow-up-form" onSubmit={submit} className="space-y-4">
      <Field><FieldLabel htmlFor="follow-up-status">Estado resultante</FieldLabel><Select value={status} onValueChange={(value) => setStatus(value as typeof status)}><SelectTrigger id="follow-up-status" className="w-full"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="IN_PROGRESS">En curso</SelectItem><SelectItem value="COMPLETED">Completada</SelectItem></SelectContent></Select><FieldDescription>Completar la acción es definitivo.</FieldDescription></Field>
      <Field>
        <FieldLabel htmlFor="follow-up-date">Fecha real</FieldLabel>
        <ProgramDatePicker
          id="follow-up-date"
          value={occurredOn}
          onChange={setOccurredOn}
          disabled={[
            ...(action.startDate ? [{ before: parseLocalDate(action.startDate)! }] : []),
            { after: parseLocalDate(todayLocal())! },
          ]}
        />
      </Field>
      <Field><FieldLabel htmlFor="follow-up-notes">Notas profesionales</FieldLabel><Textarea id="follow-up-notes" minLength={1} maxLength={2000} className="min-h-28" value={notes} onChange={(event) => setNotes(event.target.value)} required placeholder="Describí el avance realizado." /></Field>
      <Field><FieldLabel htmlFor="follow-up-result">Resultado {status === "COMPLETED" ? "(obligatorio)" : "(opcional)"}</FieldLabel><Textarea id="follow-up-result" minLength={1} maxLength={2000} value={result} onChange={(event) => setResult(event.target.value)} required={status === "COMPLETED"} placeholder="Resultado observado o cierre de la acción." /></Field>
    </form>
    <DialogFooter><DialogClose render={<Button variant="outline" disabled={create.isPending} />}>Cancelar</DialogClose><Button type="submit" form="follow-up-form" disabled={create.isPending || !notes.trim() || (status === "COMPLETED" && !result.trim())}>{create.isPending ? "Registrando…" : "Registrar"}</Button></DialogFooter>
  </DialogContent></Dialog>
}

function PublicationForm({ applicationId, actionId, followUp, onClose, onSuccess }: { applicationId: string; actionId: string; followUp: InterventionFollowUpResponse; onClose: () => void; onSuccess: () => void }) {
  const [summary, setSummary] = useState(followUp.citizenPublication?.summary ?? "")
  const publish = useMutation({ ...publishMutation(), onSuccess: () => { toast.success(followUp.citizenPublication ? "Resumen actualizado" : "Resumen publicado"); onSuccess() } })
  return <Dialog open onOpenChange={(open) => { if (!open) onClose() }}><DialogContent className="sm:max-w-xl">
    <DialogHeader><DialogTitle>{followUp.citizenPublication ? "Editar resumen ciudadano" : "Publicar resumen ciudadano"}</DialogTitle><DialogDescription>Redactá un texto claro para el ciudadano. Las notas y el resultado profesional nunca se copian automáticamente.</DialogDescription></DialogHeader>
    {publish.error && <Alert variant="destructive"><IconAlertTriangle /><AlertTitle>No se pudo publicar</AlertTitle><AlertDescription>{publish.error.message}</AlertDescription></Alert>}
    <form id="publication-form" onSubmit={(event) => { event.preventDefault(); publish.mutate({ path: { id: applicationId, actionId, followUpId: followUp.id! }, body: { summary: summary.trim() } }) }}>
      <Field><FieldLabel htmlFor="citizen-summary">Resumen visible</FieldLabel><Textarea id="citizen-summary" autoFocus className="min-h-32" minLength={1} maxLength={2000} value={summary} onChange={(event) => setSummary(event.target.value)} required placeholder="Ej. Se acordó continuar el acompañamiento la próxima semana." /><FieldDescription>Se mostrará junto con la fecha real y el estado alcanzado.</FieldDescription></Field>
    </form>
    <DialogFooter><DialogClose render={<Button variant="outline" disabled={publish.isPending} />}>Cancelar</DialogClose><Button type="submit" form="publication-form" disabled={publish.isPending || !summary.trim()}>{publish.isPending ? "Guardando…" : followUp.citizenPublication?.visible ? "Guardar cambios" : "Publicar"}</Button></DialogFooter>
  </DialogContent></Dialog>
}

function WithdrawButton({ applicationId, actionId, followUp, onSuccess }: { applicationId: string; actionId: string; followUp: InterventionFollowUpResponse; onSuccess: () => void }) {
  const withdraw = useMutation({ ...withdrawMutation(), onSuccess: () => { toast.success("Publicación retirada"); onSuccess() } })
  return <AlertDialog>
    <AlertDialogTrigger render={<Button variant="ghost" size="icon-sm" title="Retirar del portal" disabled={withdraw.isPending} />}><IconEyeOff /></AlertDialogTrigger>
    <AlertDialogContent>
      <AlertDialogHeader><AlertDialogTitle>¿Retirar este resumen?</AlertDialogTitle><AlertDialogDescription>Dejará de verse en el portal ciudadano. El seguimiento y el resumen profesional se conservarán.</AlertDialogDescription></AlertDialogHeader>
      <AlertDialogFooter><AlertDialogCancel>Cancelar</AlertDialogCancel><AlertDialogAction variant="destructive" disabled={withdraw.isPending} onClick={() => withdraw.mutate({ path: { id: applicationId, actionId, followUpId: followUp.id! } })}>{withdraw.isPending ? "Retirando…" : "Retirar"}</AlertDialogAction></AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
}
