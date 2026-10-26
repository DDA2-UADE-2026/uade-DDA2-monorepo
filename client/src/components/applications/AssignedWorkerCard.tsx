import { IconUserCheck, IconWand } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"

import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { UserChip } from "@/components/UserChip"
import { UserSelectionDialog } from "@/components/users/UserSelectionDialog"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { assignApplicationWorkerMutation, get3QueryKey, unassignApplicationWorkerMutation } from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationResponse } from "@/generated/types.gen"
import { invalidateSocialQueries } from "@/lib/social-management"

// Mismos estados que acepta el backend para asignar o reasignar.
const ASSIGNABLE_STATUSES = ["SUBMITTED", "IN_VALIDATION", "PENDING_DOCUMENTATION", "IN_EVALUATION", "IN_VISIT"]

/** Trabajador asignado a la solicitud: evalúa el caso y es responsable de sus visitas. */
export function AssignedWorkerCard({ application, permissions, evaluated }: { application: AdminApplicationResponse; permissions: string[]; evaluated: boolean }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const assignedId = application.assignedWorkerUserId ?? undefined
  const allowed = permissions.includes("applications:management:assign") && ASSIGNABLE_STATUSES.includes(application.status ?? "")
  const canAssign = allowed && permissions.includes("users:view")
  // Antes de la evaluación se puede volver a que quede asignado quien evalúe; después las visitas necesitan un asignado.
  const canUnassign = allowed && !!assignedId && !evaluated
  const onAssigned = async (data: AdminApplicationResponse, message: string) => {
    queryClient.setQueryData(get3QueryKey({ path: { id: application.id! } }), data)
    await queryClient.invalidateQueries(invalidateSocialQueries("social-evaluations"))
    setOpen(false)
    toast.success(message)
  }
  const assign = useMutation({
    ...assignApplicationWorkerMutation(),
    onSuccess: (data) => onAssigned(data, `Solicitud asignada a ${data.assignedWorkerName ?? "el trabajador seleccionado"}`),
    onError: showApiErrorToast,
  })
  const unassign = useMutation({
    ...unassignApplicationWorkerMutation(),
    onSuccess: (data) => onAssigned(data, "Asignación automática: quien registre la evaluación quedará asignado"),
    onError: showApiErrorToast,
  })
  const pending = assign.isPending || unassign.isPending

  return <Card size="sm">
    <CardHeader>
      <CardTitle>Trabajador asignado</CardTitle>
      <CardDescription>Evalúa la solicitud y es responsable de sus visitas y entrevistas.</CardDescription>
      {(canAssign || canUnassign) && <CardAction className="flex flex-wrap justify-end gap-2">
        {canUnassign && <Button size="sm" variant="ghost" disabled={pending} onClick={() => unassign.mutate({ path: { id: application.id! } })}>
          <IconWand />
          {unassign.isPending ? "Quitando…" : "Asignar automáticamente"}
        </Button>}
        {canAssign && <Button size="sm" variant="outline" disabled={pending} onClick={() => setOpen(true)}>
          <IconUserCheck />
          {assign.isPending ? "Asignando…" : assignedId ? "Reasignar" : "Asignar"}
        </Button>}
      </CardAction>}
    </CardHeader>
    <CardContent className="text-sm">
      {assignedId
        ? <UserChip user={{ id: assignedId, name: application.assignedWorkerName }} />
        : <p className="text-muted-foreground">Sin asignar. Quien registre la evaluación queda asignado automáticamente.</p>}
      {canAssign && application.status === "IN_VISIT" && <p className="mt-2 text-xs text-muted-foreground">Con visitas programadas no se puede reasignar: cancelalas o registrá su resultado antes.</p>}
    </CardContent>
    {open && <UserSelectionDialog
      open
      onOpenChange={(next) => { if (!pending) setOpen(next) }}
      selectedUserId={assignedId}
      title={assignedId ? "Reasignar solicitud" : "Asignar solicitud"}
      description="Elegí a la persona que va a evaluar el caso y hacer sus visitas. Tiene que tener un rol habilitado para evaluaciones y visitas."
      onSelect={(selected) => {
        if (selected.id == null || selected.id === assignedId) { setOpen(false); return }
        assign.mutate({ path: { id: application.id! }, body: { userId: selected.id } })
      }}
    />}
  </Card>
}
