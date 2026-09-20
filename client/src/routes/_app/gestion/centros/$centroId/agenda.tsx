import { IconAlertTriangle, IconCalendarClock, IconPencil, IconPlus, IconPower, IconRefresh, IconStethoscope } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"

import { TimeRangeDialog } from "@/components/centros/TimeRangeDialog"
import { dayIndex, dayLabel, shortTime, type TimeRangeValue } from "@/components/centros/timeRanges"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  activateCenterOpeningHourMutation,
  activateProfessionalAvailabilityMutation,
  createCenterOpeningHourMutation,
  createProfessionalAvailabilityMutation,
  deactivateCenterOpeningHourMutation,
  deactivateProfessionalAvailabilityMutation,
  listCenterOpeningHoursOptions,
  listCenterOpeningHoursQueryKey,
  listCenterServicesOptions,
  listProfessionalAssignmentsOptions,
  listProfessionalAvailabilityOptions,
  listProfessionalAvailabilityQueryKey,
  updateCenterOpeningHourMutation,
  updateProfessionalAvailabilityMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { CenterOpeningHourResponse, ProfessionalAvailabilityResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/gestion/centros/$centroId/agenda")({
  component: RouteComponent,
})

function RouteComponent() {
  const { centroId } = Route.useParams()

  return (
    <div className="mx-auto w-full max-w-4xl space-y-6 p-4 lg:p-6">
      <OpeningHoursSection centroId={centroId} />
      <AvailabilitySection centroId={centroId} />
    </div>
  )
}

function OpeningHoursSection({ centroId }: { centroId: string }) {
  const queryClient = useQueryClient()
  const hours = useQuery(listCenterOpeningHoursOptions({ path: { centerId: centroId } }))
  const [dialog, setDialog] = useState<{ range: CenterOpeningHourResponse | null } | null>(null)

  const key = listCenterOpeningHoursQueryKey({ path: { centerId: centroId } })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: key })
  const create = useMutation({ ...createCenterOpeningHourMutation(), onSuccess: () => { invalidate(); setDialog(null) } })
  const update = useMutation({ ...updateCenterOpeningHourMutation(), onSuccess: () => { invalidate(); setDialog(null) } })
  const activate = useMutation({ ...activateCenterOpeningHourMutation(), onSuccess: invalidate })
  const deactivate = useMutation({ ...deactivateCenterOpeningHourMutation(), onSuccess: invalidate })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error ?? activate.error ?? deactivate.error
  const ranges = [...(hours.data ?? [])].sort((a, b) => dayIndex(a.dayOfWeek) - dayIndex(b.dayOfWeek))

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 text-base font-semibold"><IconCalendarClock className="size-4" />Horarios de apertura</h2>
        <Button size="sm" onClick={() => { create.reset(); update.reset(); setDialog({ range: null }) }}>
          <IconPlus />Nueva franja
        </Button>
      </div>

      {mutationError && (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>{mutationError.message ?? "No se pudo actualizar el horario."}</AlertTitle>
          <AlertDescription>
            Si una disponibilidad profesional quedaría sin cobertura, el cambio se rechaza completo y se conserva el horario anterior.
          </AlertDescription>
        </Alert>
      )}

      {hours.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando horarios…</p>
      ) : hours.isError ? (
        <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
          <p>No se pudieron cargar los horarios.</p>
          <Button size="sm" variant="outline" onClick={() => hours.refetch()}><IconRefresh />Reintentar</Button>
        </div>
      ) : ranges.length === 0 ? (
        <p className="text-sm text-muted-foreground">Este centro todavía no tiene horarios de apertura.</p>
      ) : (
        <Card>
          <CardContent className="divide-y px-4 py-0">
            {ranges.map((range) => (
              <div key={range.id} className="flex items-center justify-between gap-2 py-2.5 text-sm">
                <span className={range.active ? undefined : "text-muted-foreground"}>
                  <span className="font-medium">{dayLabel(range.dayOfWeek)}</span>
                  {" · "}
                  {shortTime(range.startTime)}–{shortTime(range.endTime)}
                  {!range.active && " (inactiva)"}
                </span>
                <span className="flex gap-1">
                  <Button
                    type="button"
                    size="icon-sm"
                    variant="ghost"
                    aria-label={`Editar franja ${dayLabel(range.dayOfWeek)} ${shortTime(range.startTime)}`}
                    onClick={() => { create.reset(); update.reset(); setDialog({ range }) }}
                  >
                    <IconPencil />
                  </Button>
                  <Button
                    type="button"
                    size="icon-sm"
                    variant="ghost"
                    aria-label={`${range.active ? "Desactivar" : "Activar"} franja`}
                    disabled={activate.isPending || deactivate.isPending}
                    onClick={() => {
                      if (!range.id) return
                      if (range.active) deactivate.mutate({ path: { id: range.id } })
                      else activate.mutate({ path: { id: range.id } })
                    }}
                  >
                    <IconPower />
                  </Button>
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {dialog && (
        <TimeRangeDialog
          title={dialog.range ? "Editar franja de apertura" : "Nueva franja de apertura"}
          description="Las franjas son semanales y semiabiertas [inicio, fin): 09:00–10:00 y 10:00–11:00 no se superponen."
          initial={{
            dayOfWeek: (dialog.range?.dayOfWeek ?? "MONDAY") as TimeRangeValue["dayOfWeek"],
            startTime: dialog.range?.startTime?.slice(0, 5) ?? "",
            endTime: dialog.range?.endTime?.slice(0, 5) ?? "",
          }}
          pending={isPending}
          error={create.error ?? update.error}
          submitLabel={dialog.range ? "Guardar cambios" : "Crear franja"}
          onSubmit={(value) => {
            create.reset()
            update.reset()
            if (dialog.range?.id) {
              update.mutate({ path: { id: dialog.range.id }, body: value })
            } else {
              create.mutate({ path: { centerId: centroId }, body: value })
            }
          }}
          onOpenChange={(open) => { if (!open) setDialog(null) }}
        />
      )}
    </section>
  )
}

function AvailabilitySection({ centroId }: { centroId: string }) {
  const services = useQuery(listCenterServicesOptions({ path: { centerId: centroId } }))
  const offered = (services.data ?? []).filter((relation) => relation.id && relation.active)

  return (
    <section className="space-y-3">
      <h2 className="flex items-center gap-2 text-base font-semibold"><IconStethoscope className="size-4" />Disponibilidades profesionales</h2>
      <p className="text-sm text-muted-foreground">
        Cada franja habilita un solo servicio. Un profesional no puede tener disponibilidades superpuestas el mismo día, aunque sean de distintos servicios o centros.
      </p>

      {services.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando asignaciones…</p>
      ) : services.isError ? (
        <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
          <p>No se pudieron cargar las asignaciones.</p>
          <Button size="sm" variant="outline" onClick={() => services.refetch()}>Reintentar</Button>
        </div>
      ) : offered.length === 0 ? (
        <p className="text-sm text-muted-foreground">Asigná un servicio al centro para configurar disponibilidades.</p>
      ) : (
        <div className="space-y-4">
          {offered.map((relation) => relation.id && (
            <ServiceAvailability key={relation.id} centerServiceId={relation.id} serviceName={relation.service?.name ?? "Servicio"} />
          ))}
        </div>
      )}
    </section>
  )
}

function ServiceAvailability({ centerServiceId, serviceName }: {
  centerServiceId: string
  serviceName: string
}) {
  const queryClient = useQueryClient()
  const assignments = useQuery(listProfessionalAssignmentsOptions({ path: { centerServiceId } }))
  const active = (assignments.data ?? []).filter((assignment) => assignment.id && assignment.active)

  if (assignments.isPending) {
    return <p className="text-sm text-muted-foreground">Cargando {serviceName}…</p>
  }
  if (assignments.isError) {
    return (
      <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
        <p>No se pudieron cargar las disponibilidades de {serviceName}.</p>
        <Button size="sm" variant="outline" onClick={() => assignments.refetch()}>Reintentar</Button>
      </div>
    )
  }
  if (active.length === 0) return null

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{serviceName}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {active.map((assignment) => assignment.id && (
          <AssignmentAvailability
            key={assignment.id}
            assignmentId={assignment.id}
            professionalName={assignment.professionalName ?? "Profesional"}
            onChanged={() => queryClient.invalidateQueries({
              queryKey: listProfessionalAvailabilityQueryKey({ path: { assignmentId: assignment.id! } }),
            })}
          />
        ))}
      </CardContent>
    </Card>
  )
}

function AssignmentAvailability({ assignmentId, professionalName, onChanged }: {
  assignmentId: string
  professionalName: string
  onChanged: () => void
}) {
  const queryClient = useQueryClient()
  const availability = useQuery(listProfessionalAvailabilityOptions({ path: { assignmentId } }))
  const [dialog, setDialog] = useState<{ range: ProfessionalAvailabilityResponse | null } | null>(null)

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: listProfessionalAvailabilityQueryKey({ path: { assignmentId } }) })
    onChanged()
  }
  const create = useMutation({ ...createProfessionalAvailabilityMutation(), onSuccess: () => { invalidate(); setDialog(null) } })
  const update = useMutation({ ...updateProfessionalAvailabilityMutation(), onSuccess: () => { invalidate(); setDialog(null) } })
  const activate = useMutation({ ...activateProfessionalAvailabilityMutation(), onSuccess: invalidate })
  const deactivate = useMutation({ ...deactivateProfessionalAvailabilityMutation(), onSuccess: invalidate })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error ?? activate.error ?? deactivate.error
  const ranges = [...(availability.data ?? [])]
    .filter((range) => range.active)
    .sort((a, b) => dayIndex(a.dayOfWeek) - dayIndex(b.dayOfWeek))

  return (
    <div className="space-y-2 border-t pt-3 first:border-t-0 first:pt-0">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold">{professionalName}</h3>
        <Button size="xs" variant="outline" onClick={() => { create.reset(); update.reset(); setDialog({ range: null }) }}>
          <IconPlus />Agregar
        </Button>
      </div>

      {mutationError && (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>{mutationError.message ?? "No se pudo actualizar la disponibilidad."}</AlertTitle>
        </Alert>
      )}

      {availability.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : availability.isError ? (
        <Button size="xs" variant="outline" onClick={() => availability.refetch()}>Reintentar</Button>
      ) : ranges.length === 0 ? (
        <p className="text-sm text-muted-foreground">Sin franjas activas.</p>
      ) : (
        <ul className="divide-y rounded-lg border px-3">
          {ranges.map((range) => (
            <li key={range.id} className="flex items-center justify-between gap-2 py-2 text-sm">
              <span>{dayLabel(range.dayOfWeek)} · {shortTime(range.startTime)}–{shortTime(range.endTime)}</span>
              <span className="flex gap-1">
                <Button
                  type="button"
                  size="icon-sm"
                  variant="ghost"
                  aria-label="Editar disponibilidad"
                  onClick={() => { create.reset(); update.reset(); setDialog({ range }) }}
                >
                  <IconPencil />
                </Button>
                <Button
                  type="button"
                  size="icon-sm"
                  variant="ghost"
                  aria-label="Desactivar disponibilidad"
                  disabled={deactivate.isPending}
                  onClick={() => range.id && deactivate.mutate({ path: { id: range.id } })}
                >
                  <IconPower />
                </Button>
              </span>
            </li>
          ))}
        </ul>
      )}

      {dialog && (
        <TimeRangeDialog
          title={dialog.range ? "Editar disponibilidad" : "Nueva disponibilidad"}
          description={`${professionalName}: la franja debe quedar cubierta por el horario del centro y no superponerse con otras del profesional.`}
          initial={{
            dayOfWeek: (dialog.range?.dayOfWeek ?? "MONDAY") as TimeRangeValue["dayOfWeek"],
            startTime: dialog.range?.startTime?.slice(0, 5) ?? "",
            endTime: dialog.range?.endTime?.slice(0, 5) ?? "",
          }}
          pending={isPending}
          error={create.error ?? update.error}
          submitLabel={dialog.range ? "Guardar cambios" : "Crear disponibilidad"}
          onSubmit={(value) => {
            create.reset()
            update.reset()
            if (dialog.range?.id) {
              update.mutate({ path: { id: dialog.range.id }, body: value })
            } else {
              create.mutate({ path: { assignmentId }, body: value })
            }
          }}
          onOpenChange={(open) => { if (!open) setDialog(null) }}
        />
      )}
    </div>
  )
}
