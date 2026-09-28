import { IconAlertTriangle, IconArrowLeft, IconCalendarCheck, IconCalendarX } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute, redirect } from "@tanstack/react-router"
import { useState } from "react"

import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
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
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  cancelAdminAppointmentMutation,
  getAdminAppointmentQueryKey,
  getAdminAppointmentOptions,
  listAdminAppointmentSlotsOptions,
  releaseAdminAppointmentSlotMutation,
  rescheduleAdminAppointmentMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { AdminAppointmentResponse, AvailableAppointmentSlotResponse } from "@/generated/types.gen"
import {
  apiErrorCode,
  appointmentStatusLabels,
  formatAppointmentDate,
  formatAppointmentRange,
  formatAppointmentTime,
  isUuid,
  slotFingerprint,
} from "@/lib/appointment-flow"

export const Route = createFileRoute("/_app/gestion/turnos/$turnoId")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

const RESCHEDULE_ERROR_MESSAGES: Record<string, string> = {
  APPOINTMENT_SLOT_UNAVAILABLE: "El horario dejó de estar disponible. Elegí otro de la lista actualizada.",
  APPOINTMENT_CITIZEN_OVERLAP: "El ciudadano ya tiene otro turno en ese horario.",
  APPOINTMENT_NOT_MANAGEABLE: "El turno ya no se puede modificar.",
}

function errorMessage(code: string | undefined, fallback: string) {
  if (!code) return fallback
  return RESCHEDULE_ERROR_MESSAGES[code] ?? fallback
}

function RouteComponent() {
  const { turnoId } = Route.useParams()
  const queryClient = useQueryClient()
  const valid = isUuid(turnoId)
  const detail = useQuery({ ...getAdminAppointmentOptions({ path: { id: turnoId } }), enabled: valid })
  const row = detail.isPending || detail.isError ? undefined : detail.data
  const appointment = row?.appointment
  const citizenName = row?.citizenName
  const slotRetained = row?.slotRetained ?? false

  const [confirmAction, setConfirmAction] = useState<"cancel" | "release" | null>(null)

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: getAdminAppointmentQueryKey({ path: { id: turnoId } }) })
    queryClient.invalidateQueries({
      predicate: (query) => (query.queryKey[0] as { _id?: unknown } | undefined)?._id === "listAdminAppointments",
    })
  }

  const cancel = useMutation({
    ...cancelAdminAppointmentMutation(),
    onSuccess: () => { invalidate(); setConfirmAction(null) },
  })
  const release = useMutation({
    ...releaseAdminAppointmentSlotMutation(),
    onSuccess: () => { invalidate(); setConfirmAction(null) },
  })
  const actionPending = cancel.isPending || release.isPending
  const actionError = cancel.error ?? release.error

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Turnos", to: "/gestion/turnos" }, { label: "Detalle" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-3xl space-y-4 p-4 lg:p-6">
            <Button size="sm" variant="ghost" render={<Link to="/gestion/turnos" />}>
              <IconArrowLeft />Volver a turnos
            </Button>
      {!valid ? (
        <Alert variant="destructive">
          <AlertTitle>No encontramos ese turno</AlertTitle>
          <AlertDescription>El identificador no es válido.</AlertDescription>
        </Alert>
      ) : detail.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando turno…</p>
      ) : detail.isError || !row || !appointment?.id ? (
        <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
          <p>No se pudo cargar el turno.</p>
          <Button size="sm" variant="outline" onClick={() => detail.refetch()}>Reintentar</Button>
        </div>
      ) : (
        <>
          <Card>
            <CardHeader>
              <span className="flex flex-wrap gap-1">
                <Badge variant={appointment.status === "CANCELLED" ? "secondary" : "default"}>
                  {appointment.status === "CANCELLED" ? <IconCalendarX /> : <IconCalendarCheck />}
                  {appointment.status ? appointmentStatusLabels[appointment.status] : "—"}
                </Badge>
                {slotRetained && <Badge variant="outline">Horario retenido</Badge>}
              </span>
              <CardTitle className="font-heading text-xl">{appointment.serviceName}</CardTitle>
              <CardDescription>{appointment.centerName} · {appointment.centerAddress}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <p><span className="font-medium">Ciudadano:</span> {citizenName ?? "—"}</p>
              <p><span className="font-medium">Fecha:</span> {formatAppointmentDate(appointment.startsAt?.slice(0, 10))}</p>
              <p><span className="font-medium">Horario:</span> {formatAppointmentRange(appointment.startsAt, appointment.endsAt)}</p>
              <p><span className="font-medium">Profesional:</span> {appointment.professionalName}</p>
              <p className="text-muted-foreground">Identificador: {appointment.id}</p>
            </CardContent>
          </Card>

          {slotRetained && (
            <Alert>
              <IconAlertTriangle />
              <AlertTitle>Horario retenido</AlertTitle>
              <AlertDescription>
                Este turno está cancelado y su horario todavía no vuelve a ofrecerse.
                Podés habilitarlo manualmente para que vuelva a estar disponible.
              </AlertDescription>
            </Alert>
          )}

          {actionError && (
            <Alert variant="destructive">
              <AlertTitle>No se pudo completar la operación</AlertTitle>
              <AlertDescription>
                {errorMessage(apiErrorCode(actionError), actionError.message ?? "Intentá nuevamente en unos instantes.")}
              </AlertDescription>
            </Alert>
          )}

          <div className="flex flex-wrap gap-2">
            {appointment.status === "CONFIRMED" && (
              <Button
                variant="destructive"
                disabled={actionPending}
                onClick={() => { cancel.reset(); release.reset(); setConfirmAction("cancel") }}
              >
                Cancelar turno
              </Button>
            )}
            {slotRetained && (
              <Button
                variant="outline"
                disabled={actionPending}
                onClick={() => { cancel.reset(); release.reset(); setConfirmAction("release") }}
              >
                {release.isPending ? "Habilitando…" : "Habilitar horario"}
              </Button>
            )}
          </div>

          {appointment.status === "CONFIRMED" && (
            <RescheduleSection turnoId={turnoId} current={row} onChanged={invalidate} />
          )}
        </>
      )}

      <AlertDialog open={confirmAction !== null} onOpenChange={(open) => { if (!open && !actionPending) setConfirmAction(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {confirmAction === "cancel" ? "Cancelar turno" : "Habilitar horario"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {confirmAction === "cancel"
                ? "El turno quedará cancelado y su horario retenido: no volverá a ofrecerse hasta que se habilite manualmente."
                : "El horario del turno cancelado volverá a ofrecerse si sigue cumpliendo las reglas de disponibilidad."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionPending}>Volver</AlertDialogCancel>
            <AlertDialogAction
              disabled={actionPending}
              onClick={(event) => {
                event.preventDefault()
                if (confirmAction === "cancel") cancel.mutate({ path: { id: turnoId } })
                else if (confirmAction === "release") release.mutate({ path: { id: turnoId } })
              }}
            >
              {actionPending ? "Guardando…" : confirmAction === "cancel" ? "Cancelar turno" : "Habilitar horario"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
          </div>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function RescheduleSection({ turnoId, current, onChanged }: {
  turnoId: string
  current: AdminAppointmentResponse
  onChanged: () => void
}) {
  const appointment = current.appointment
  const [date, setDate] = useState(appointment?.startsAt?.slice(0, 10) ?? "")
  const [selected, setSelected] = useState("")

  const slots = useQuery({
    ...listAdminAppointmentSlotsOptions({ path: { id: turnoId }, query: { date } }),
    enabled: date.length === 10,
  })
  const options = slots.data ?? []

  const reschedule = useMutation({
    ...rescheduleAdminAppointmentMutation(),
    onSuccess: () => { setSelected(""); onChanged(); slots.refetch() },
  })

  const apply = (slot: AvailableAppointmentSlotResponse) => {
    if (!slot.professionalAssignmentId || !slot.startsAt || !slot.endsAt) return
    reschedule.mutate({
      path: { id: turnoId },
      body: {
        professionalAssignmentId: slot.professionalAssignmentId,
        startsAt: slot.startsAt,
        endsAt: slot.endsAt,
      },
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Reprogramar turno</CardTitle>
        <CardDescription>
          Se mantiene el servicio y el centro. Elegí un nuevo horario disponible;
          el profesional puede cambiar según el horario elegido.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="max-w-xs space-y-1.5">
          <Label htmlFor="reschedule-fecha">Fecha</Label>
          <Input
            id="reschedule-fecha"
            type="date"
            value={date}
            onChange={(event) => { if (event.target.value) { setDate(event.target.value); setSelected("") } }}
          />
        </div>
        {slots.isPending ? (
          <p className="text-sm text-muted-foreground">Cargando horarios…</p>
        ) : slots.isError ? (
          <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
            <p>No se pudieron cargar los horarios.</p>
            <Button size="sm" variant="outline" onClick={() => slots.refetch()}>Reintentar</Button>
          </div>
        ) : options.length === 0 ? (
          <p className="text-sm text-muted-foreground">No hay horarios disponibles para esa fecha.</p>
        ) : (
          <ul className="grid gap-2 sm:grid-cols-2">
            {options.map((slot) => {
              const fingerprint = slotFingerprint({
                professionalAssignmentId: slot.professionalAssignmentId ?? "",
                startsAt: slot.startsAt ?? "",
                endsAt: slot.endsAt ?? "",
              })
              const active = selected === fingerprint
              return (
                <li key={fingerprint}>
                  <button
                    type="button"
                    aria-pressed={active}
                    onClick={() => { reschedule.reset(); setSelected(fingerprint) }}
                    className={`w-full rounded-lg border p-3 text-left text-sm transition-colors ${active ? "border-primary bg-primary/5" : "hover:border-muted-foreground"}`}
                  >
                    <span className="font-medium">{formatAppointmentTime(slot.startsAt)} – {formatAppointmentTime(slot.endsAt)}</span>
                    <span className="block text-muted-foreground">{slot.professionalName}</span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
        {reschedule.error && (
          <Alert variant="destructive">
            <AlertTitle>No se pudo reprogramar</AlertTitle>
            <AlertDescription>
              {errorMessage(apiErrorCode(reschedule.error), reschedule.error.message ?? "Intentá nuevamente en unos instantes.")}
            </AlertDescription>
          </Alert>
        )}
        <div>
          <Button
            disabled={!selected || reschedule.isPending || slots.isPending}
            onClick={() => {
              const slot = options.find((option) => slotFingerprint({
                professionalAssignmentId: option.professionalAssignmentId ?? "",
                startsAt: option.startsAt ?? "",
                endsAt: option.endsAt ?? "",
              }) === selected)
              if (slot) apply(slot)
            }}
          >
            {reschedule.isPending ? "Guardando…" : "Confirmar reprogramación"}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
