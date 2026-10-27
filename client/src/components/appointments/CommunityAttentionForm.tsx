import { useMutation } from "@tanstack/react-query"
import { useState, type FormEvent } from "react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  correctCommunityAttentionMutation,
  registerCommunityAttentionMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { CreateAttentionRequest, ProfessionalAppointmentResponse } from "@/generated/types.gen"
import { apiErrorCode, appointmentToday } from "@/lib/appointment-flow"
import { attentionRequest } from "@/lib/community-attention"

export function CommunityAttentionForm({ turnoId, row, onSaved, onConflict }: {
  turnoId: string
  row: ProfessionalAppointmentResponse
  onSaved: () => void
  onConflict: () => void
}) {
  const existing = row.attention
  const [result, setResult] = useState<CreateAttentionRequest["result"]>(existing?.result ?? "ATENDIDO")
  const [attendedOn, setAttendedOn] = useState(existing?.attendedOn ?? appointmentToday())
  const [description, setDescription] = useState(existing?.description ?? "")
  const [validationError, setValidationError] = useState<string | null>(null)

  const create = useMutation({ ...registerCommunityAttentionMutation(), onSuccess: onSaved, onError: onMutationError })
  const correct = useMutation({ ...correctCommunityAttentionMutation(), onSuccess: onSaved, onError: onMutationError })
  const pending = create.isPending || correct.isPending
  const error = create.error ?? correct.error

  function onMutationError(error: unknown) {
    if (["ATTENTION_CHANGED", "ATTENTION_ALREADY_REGISTERED", "ATTENTION_NOT_REGISTRABLE"].includes(apiErrorCode(error) ?? "")) {
      onConflict()
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setValidationError(null)
    create.reset()
    correct.reset()
    const payload = attentionRequest(result, attendedOn, description)
    if (!payload.body) {
      setValidationError(payload.error)
      return
    }
    const body = payload.body
    if (existing) {
      if (existing.version === undefined) {
        onConflict()
        return
      }
      correct.mutate({ path: { id: turnoId }, body: { ...body, version: existing.version } })
    } else {
      create.mutate({ path: { id: turnoId }, body })
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{existing ? "Corregir constancia" : "Registrar asistencia"}</CardTitle>
        <CardDescription>El registro describe el servicio comunitario prestado, sin datos clínicos.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={submit}>
          <div className="space-y-2">
            <Label>Resultado</Label>
            <div className="flex flex-wrap gap-2" role="group" aria-label="Resultado de asistencia">
              <Button type="button" variant={result === "ATENDIDO" ? "default" : "outline"} aria-pressed={result === "ATENDIDO"}
                disabled={pending} onClick={() => { setResult("ATENDIDO"); setValidationError(null) }}>Atendido</Button>
              <Button type="button" variant={result === "AUSENTE" ? "default" : "outline"} aria-pressed={result === "AUSENTE"}
                disabled={pending} onClick={() => { setResult("AUSENTE"); setValidationError(null) }}>Ausente</Button>
            </div>
          </div>

          {result === "ATENDIDO" && (
            <>
              <div className="space-y-2">
                <Label htmlFor="attention-date">Fecha de atención</Label>
                <Input id="attention-date" type="date" required max={appointmentToday()} value={attendedOn}
                  disabled={pending} onChange={(event) => setAttendedOn(event.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="attention-description">Servicio prestado</Label>
                <Textarea id="attention-description" required maxLength={500} rows={4} value={description} disabled={pending}
                  placeholder="Describí brevemente el servicio comunitario realizado"
                  onChange={(event) => setDescription(event.target.value)} />
                <p className="text-xs text-muted-foreground">No incluyas diagnósticos, síntomas, tratamientos ni datos de historia clínica. Máximo 500 caracteres.</p>
              </div>
            </>
          )}

          {validationError && <Alert variant="destructive"><AlertTitle>Revisá el registro</AlertTitle><AlertDescription>{validationError}</AlertDescription></Alert>}
          {error && <Alert variant="destructive"><AlertTitle>No se pudo guardar</AlertTitle><AlertDescription>
            {["ATTENTION_CHANGED", "ATTENTION_ALREADY_REGISTERED"].includes(apiErrorCode(error) ?? "")
              ? "La constancia cambió. Actualizamos el turno; revisá los datos vigentes antes de volver a guardar."
              : apiErrorCode(error) === "ATTENTION_NOT_REGISTRABLE"
                ? "El turno ya no admite un registro de atención. Actualizamos sus datos."
                : "Revisá los datos e intentá nuevamente."}
          </AlertDescription></Alert>}

          <Button type="submit" disabled={pending}>{pending ? "Guardando…" : existing ? "Guardar corrección" : "Registrar resultado"}</Button>
        </form>
      </CardContent>
    </Card>
  )
}
