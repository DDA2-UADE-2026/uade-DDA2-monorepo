import { IconArrowLeft, IconCalendarCheck, IconCheck } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, useNavigate } from "@tanstack/react-router"
import { useRef, useState } from "react"

import { ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { AppointmentError } from "@/components/appointments/AppointmentUi"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { formatLocalDate } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Item, ItemContent, ItemDescription, ItemGroup, ItemMedia, ItemTitle } from "@/components/ui/item"
import {
  createCitizenAppointmentMutation,
  getCitizenAppointmentQueryKey,
  listCitizenAppointmentCentersOptions,
  listCitizenAppointmentServicesOptions,
  listCitizenAppointmentSlotsOptions,
} from "@/generated/@tanstack/react-query.gen"
import type {
  AppointmentCenterResponse,
  AppointmentServiceResponse,
  AvailableAppointmentSlotResponse,
} from "@/generated/types.gen"
import {
  apiErrorCode,
  appointmentToday,
  clearAppointmentAttemptKey,
  formatAppointmentDate,
  formatAppointmentRange,
  getAppointmentAttemptKey,
  isUuid,
  slotFingerprint,
} from "@/lib/appointment-flow"

export function AppointmentWizard({ userId }: { userId: number }) {
  const [service, setService] = useState<AppointmentServiceResponse | null>(null)
  const [center, setCenter] = useState<AppointmentCenterResponse | null>(null)
  const [date, setDate] = useState("")
  const [slot, setSlot] = useState<AvailableAppointmentSlotResponse | null>(null)
  const [staleNotice, setStaleNotice] = useState(false)

  const services = useQuery(listCitizenAppointmentServicesOptions())
  const centers = useQuery({
    ...listCitizenAppointmentCentersOptions({ path: { serviceId: service?.id ?? "" } }),
    enabled: Boolean(service?.id),
  })
  const slots = useQuery({
    ...listCitizenAppointmentSlotsOptions({
      query: { centerServiceId: center?.centerServiceId ?? "", date },
    }),
    enabled: Boolean(center?.centerServiceId && date),
  })

  const pickService = (value: AppointmentServiceResponse) => {
    setService(value)
    setCenter(null)
    setDate("")
    setSlot(null)
    setStaleNotice(false)
  }
  const pickCenter = (value: AppointmentCenterResponse) => {
    setCenter(value)
    setDate("")
    setSlot(null)
    setStaleNotice(false)
  }
  const pickDate = (value: string) => {
    setDate(value)
    setSlot(null)
    setStaleNotice(false)
  }
  const pickSlot = (value: AvailableAppointmentSlotResponse) => {
    setSlot(value)
    setStaleNotice(false)
  }

  return (
    <ApplicationPage breadcrumbs={[{ label: "Mis turnos", to: "/portal/turnos" }, { label: "Solicitar turno" }]}>
      <ApplicationHeading title="Solicitar turno" description="Elegí el servicio, el centro, la fecha y el horario para tu atención." />
      <section aria-label="Servicio">
        <h2 className="mb-3 font-medium">1. Servicio</h2>
        {services.isPending ? <ApplicationLoading /> : services.isError ? (
          <AppointmentError error={services.error} title="No pudimos consultar los servicios" retry={() => services.refetch()} />
        ) : !services.data?.length ? (
          <Card><CardHeader><CardTitle>No hay servicios disponibles</CardTitle><CardDescription>Todavía no hay servicios municipales habilitados para solicitar turnos.</CardDescription></CardHeader></Card>
        ) : (
          <ItemGroup>
            {services.data.map((item) => (
              <Item key={item.id} variant={service?.id === item.id ? "default" : "outline"}>
                <ItemContent>
                  <ItemTitle>{item.name}</ItemTitle>
                  <ItemDescription className="line-clamp-none">{item.description} · Duración estimada: {item.durationMinutes} min.</ItemDescription>
                </ItemContent>
                <Button
                  variant={service?.id === item.id ? "default" : "outline"}
                  aria-pressed={service?.id === item.id}
                  onClick={() => pickService(item)}
                >
                  {service?.id === item.id ? "Seleccionado" : "Elegir"}
                </Button>
              </Item>
            ))}
          </ItemGroup>
        )}
      </section>

      {service && (
        <section aria-label="Centro">
          <h2 className="mb-3 font-medium">2. Centro</h2>
          {centers.isPending ? <ApplicationLoading /> : centers.isError ? (
            <AppointmentError error={centers.error} title="No pudimos consultar los centros" retry={() => centers.refetch()} />
          ) : !centers.data?.length ? (
            <Card><CardHeader><CardTitle>Sin centros para este servicio</CardTitle><CardDescription>Ningún centro ofrece actualmente este servicio.</CardDescription></CardHeader></Card>
          ) : (
            <ItemGroup>
              {centers.data.map((item) => (
                <Item key={item.centerServiceId} variant={center?.centerServiceId === item.centerServiceId ? "default" : "outline"}>
                  <ItemContent>
                    <ItemTitle>{item.name}</ItemTitle>
                    <ItemDescription className="line-clamp-none">{item.address}{item.phone ? ` · ${item.phone}` : ""}</ItemDescription>
                  </ItemContent>
                  <Button
                    variant={center?.centerServiceId === item.centerServiceId ? "default" : "outline"}
                    aria-pressed={center?.centerServiceId === item.centerServiceId}
                    onClick={() => pickCenter(item)}
                  >
                    {center?.centerServiceId === item.centerServiceId ? "Seleccionado" : "Elegir"}
                  </Button>
                </Item>
              ))}
            </ItemGroup>
          )}
        </section>
      )}

      {center && (
        <section aria-label="Fecha">
          <h2 className="mb-3 font-medium">3. Fecha</h2>
          <ProgramDatePicker
            value={date}
            onChange={pickDate}
            placeholder="Seleccioná la fecha del turno"
            disabled={(day) => formatLocalDate(day) < appointmentToday()}
          />
        </section>
      )}

      {center && date && (
        <section aria-label="Horario">
          <h2 className="mb-3 font-medium">4. Horario y profesional</h2>
          {staleNotice && (
            <Alert variant="destructive" className="mb-3">
              <AlertTitle>El horario dejó de estar disponible</AlertTitle>
              <AlertDescription>Otra persona lo obtuvo o cambió la agenda. Consultamos nuevamente los horarios de la fecha: elegí otra opción.</AlertDescription>
            </Alert>
          )}
          {slots.isPending ? <ApplicationLoading /> : slots.isError ? (
            <AppointmentError error={slots.error} title="No pudimos consultar los horarios" retry={() => slots.refetch()} />
          ) : !slots.data?.length ? (
            <Card><CardHeader><CardTitle>Sin horarios disponibles</CardTitle><CardDescription>No hay horarios otorgables para esta fecha. Probá con otra fecha.</CardDescription></CardHeader></Card>
          ) : (
            <ItemGroup>
              {slots.data.map((item) => {
                const key = slotFingerprint(item)
                const selected = slot !== null && slotFingerprint(slot) === key
                return (
                  <Item key={key} variant={selected ? "default" : "outline"}>
                    <ItemMedia variant="icon"><IconCalendarCheck /></ItemMedia>
                    <ItemContent>
                      <ItemTitle>{formatAppointmentRange(item.startsAt, item.endsAt)}</ItemTitle>
                      <ItemDescription>Profesional: {item.professionalName}</ItemDescription>
                    </ItemContent>
                    <Button variant={selected ? "default" : "outline"} aria-pressed={selected} onClick={() => pickSlot(item)}>
                      {selected ? "Seleccionado" : "Elegir"}
                    </Button>
                  </Item>
                )
              })}
            </ItemGroup>
          )}
        </section>
      )}

      {slot && service && center && date && (
        <AppointmentConfirmation
          key={slotFingerprint(slot)}
          userId={userId}
          service={service}
          center={center}
          date={date}
          slot={slot}
          onStaleSlot={() => {
            setSlot(null)
            setStaleNotice(true)
            slots.refetch()
          }}
        />
      )}
    </ApplicationPage>
  )
}

export function AppointmentConfirmation({ userId, service, center, date, slot, onStaleSlot }: {
  userId: number
  service: AppointmentServiceResponse
  center: AppointmentCenterResponse
  date: string
  slot: AvailableAppointmentSlotResponse
  onStaleSlot: () => void
}) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const submitting = useRef(false)
  const confirm = useMutation({ ...createCitizenAppointmentMutation(), retry: false, onError: showApiErrorToast })
  const errorCode = apiErrorCode(confirm.error)

  const request = {
    professionalAssignmentId: slot.professionalAssignmentId ?? "",
    startsAt: slot.startsAt ?? "",
    endsAt: slot.endsAt ?? "",
  }

  const book = async () => {
    if (submitting.current || confirm.isSuccess) return
    // Validación explícita en lugar del esquema generado: zCreateAppointmentRequest
    // usa z.iso.datetime() sin offsets y rechazaría los horarios -03:00 que el
    // propio backend devuelve y acepta.
    if (!isUuid(request.professionalAssignmentId) || !request.startsAt || !request.endsAt) return
    if (!(request.startsAt < request.endsAt)) return
    submitting.current = true
    const attemptKey = getAppointmentAttemptKey(userId, request.professionalAssignmentId, request.startsAt, request.endsAt)
    try {
      const appointment = await confirm.mutateAsync({ body: request, headers: { "Idempotency-Key": attemptKey } })
      if (appointment.id) {
        queryClient.setQueryData(getCitizenAppointmentQueryKey({ path: { appointmentId: appointment.id } }), appointment)
        clearAppointmentAttemptKey(userId, request.professionalAssignmentId, request.startsAt, request.endsAt)
        await navigate({ to: "/portal/turnos/$turnoId", params: { turnoId: appointment.id }, replace: true })
      }
    } catch (error) {
      // Keep the attempt key so a retry is safe. A stale slot clears the
      // selection and refreshes availability; other conflicts stay inline.
      if (apiErrorCode(error) === "APPOINTMENT_SLOT_UNAVAILABLE") onStaleSlot()
    } finally {
      submitting.current = false
    }
  }

  return (
    <section aria-label="Revisión y confirmación">
      <h2 className="mb-3 font-medium">5. Revisión y confirmación</h2>
      <Card>
        <CardHeader>
          <Badge variant="secondary" className="w-fit">{service.name}</Badge>
          <CardTitle className="font-heading text-xl">{center.name}</CardTitle>
          <CardDescription>{center.address}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <p><span className="font-medium">Fecha:</span> {formatAppointmentDate(date)}</p>
          <p><span className="font-medium">Horario:</span> {formatAppointmentRange(slot.startsAt, slot.endsAt)}</p>
          <p><span className="font-medium">Profesional:</span> {slot.professionalName}</p>
          <p className="text-muted-foreground">Los horarios se muestran en hora de Buenos Aires.</p>
        </CardContent>
      </Card>
      <Alert><IconCheck /><AlertTitle>Vas a solicitar este turno a tu nombre</AlertTitle><AlertDescription>El turno queda confirmado de inmediato y no se puede cancelar desde esta historia.</AlertDescription></Alert>
      {errorCode === "APPOINTMENT_CITIZEN_OVERLAP" && (
        <Alert variant="destructive"><AlertTitle>Ya tenés un turno en ese horario</AlertTitle><AlertDescription>Elegí un horario que no se superponga con tus otros turnos.</AlertDescription></Alert>
      )}
      {confirm.isSuccess && !confirm.data.id && (
        <Alert variant="destructive"><AlertTitle>La respuesta no incluyó el turno</AlertTitle><AlertDescription>Volvé a consultar los horarios antes de reintentar.</AlertDescription></Alert>
      )}
      <div className="flex flex-col-reverse justify-between gap-3 sm:flex-row">
        <Button variant="outline" disabled={confirm.isPending} render={<Link to="/portal/turnos" />}>
          <IconArrowLeft />Volver a mis turnos
        </Button>
        <Button onClick={book} disabled={confirm.isPending || confirm.isSuccess}>
          {confirm.isPending ? "Confirmando…" : "Confirmar turno"}
        </Button>
      </div>
    </section>
  )
}
