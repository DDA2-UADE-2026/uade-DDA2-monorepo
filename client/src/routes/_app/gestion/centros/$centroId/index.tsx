import { IconAlertTriangle, IconBriefcase, IconMail, IconMapPin, IconPencil, IconPhone, IconPlus, IconPower, IconRefresh, IconStethoscope } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"

import { CenterDialog } from "@/components/centros/CenterDialog"
import { Alert, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  activateCenterServiceMutation,
  assignProfessionalMutation,
  assignServiceToCenterMutation,
  deactivateCenterServiceMutation,
  findAllOptions,
  getMunicipalCenterOptions,
  getMunicipalCenterQueryKey,
  listCenterServicesOptions,
  listCenterServicesQueryKey,
  listMunicipalServicesOptions,
  listProfessionalAssignmentsOptions,
  listProfessionalAssignmentsQueryKey,
} from "@/generated/@tanstack/react-query.gen"
import type { CenterServiceResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/gestion/centros/$centroId/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { centroId } = Route.useParams()
  const queryClient = useQueryClient()
  const center = useQuery(getMunicipalCenterOptions({ path: { id: centroId } }))
  const [editing, setEditing] = useState(false)
  const invalidateCenter = () => queryClient.invalidateQueries({
    queryKey: getMunicipalCenterQueryKey({ path: { id: centroId } }),
  })

  if (center.isPending) {
    return <p className="p-4 text-sm text-muted-foreground lg:p-6">Cargando centro…</p>
  }
  if (center.isError || !center.data) {
    return (
      <div className="flex flex-col items-start gap-2 p-4 text-sm text-muted-foreground lg:p-6">
        <p>No se pudo cargar el centro.</p>
        <Button size="sm" variant="outline" onClick={() => center.refetch()}>Reintentar</Button>
      </div>
    )
  }

  const data = center.data
  return (
    <div className="mx-auto w-full max-w-4xl space-y-6 p-4 lg:p-6">
      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-2">
            <div>
              <CardTitle>{data.name}</CardTitle>
              <CardDescription className="mt-1 flex flex-col gap-1">
                <span className="flex items-center gap-1.5"><IconMapPin className="size-3.5 shrink-0" />{data.address}</span>
                {data.phone && <span className="flex items-center gap-1.5"><IconPhone className="size-3.5 shrink-0" />{data.phone}</span>}
                {data.email && <span className="flex items-center gap-1.5"><IconMail className="size-3.5 shrink-0" />{data.email}</span>}
              </CardDescription>
            </div>
            <Button size="sm" variant="outline" onClick={() => setEditing(true)}>
              <IconPencil />Editar
            </Button>
          </div>
        </CardHeader>
      </Card>

      <CenterServicesSection centroId={centroId} />

      {editing && (
        <CenterDialog
          center={data}
          onOpenChange={(open) => { if (!open) setEditing(false) }}
          onSaved={invalidateCenter}
        />
      )}
    </div>
  )
}

function CenterServicesSection({ centroId }: { centroId: string }) {
  const queryClient = useQueryClient()
  const services = useQuery(listCenterServicesOptions({ path: { centerId: centroId } }))
  const catalog = useQuery(listMunicipalServicesOptions({ query: { size: 100, active: true } }))
  const [selectedServiceId, setSelectedServiceId] = useState<string>("")

  const invalidate = () => queryClient.invalidateQueries({ queryKey: listCenterServicesQueryKey({ path: { centerId: centroId } }) })
  const assign = useMutation({ ...assignServiceToCenterMutation(), onSuccess: () => { invalidate(); setSelectedServiceId("") } })
  const activate = useMutation({ ...activateCenterServiceMutation(), onSuccess: invalidate })
  const deactivate = useMutation({ ...deactivateCenterServiceMutation(), onSuccess: invalidate })
  const mutationError = assign.error ?? activate.error ?? deactivate.error

  const offered = services.data ?? []
  const offeredServiceIds = new Set(offered.map((relation) => relation.service?.id).filter(Boolean))
  const available = (catalog.data?.content ?? []).filter((service) => service.id && !offeredServiceIds.has(service.id))

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 text-base font-semibold"><IconBriefcase className="size-4" />Servicios ofrecidos</h2>
      </div>

      {mutationError && (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>{mutationError.message ?? "No se pudo actualizar el servicio del centro."}</AlertTitle>
        </Alert>
      )}

      <Card>
        <CardContent className="flex flex-col gap-2 pt-6 sm:flex-row">
          <Select value={selectedServiceId} onValueChange={(value) => setSelectedServiceId(value ?? "")} disabled={!catalog.data}>
            <SelectTrigger className="flex-1" aria-label="Servicio del catálogo para asignar">
              <SelectValue placeholder={catalog.isPending ? "Cargando catálogo…" : "Seleccioná un servicio del catálogo"} />
            </SelectTrigger>
            <SelectContent>
              {available.map((service) => (
                <SelectItem key={service.id} value={service.id ?? ""}>{service.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            size="sm"
            disabled={!selectedServiceId || assign.isPending}
            onClick={() => selectedServiceId && assign.mutate({ path: { centerId: centroId }, body: { serviceId: selectedServiceId } })}
          >
            <IconPlus />{assign.isPending ? "Asignando…" : "Asignar servicio"}
          </Button>
        </CardContent>
      </Card>

      {services.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando servicios…</p>
      ) : services.isError ? (
        <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
          <p>No se pudieron cargar los servicios del centro.</p>
          <Button size="sm" variant="outline" onClick={() => services.refetch()}><IconRefresh />Reintentar</Button>
        </div>
      ) : offered.length === 0 ? (
        <p className="text-sm text-muted-foreground">Este centro todavía no ofrece ningún servicio.</p>
      ) : (
        <div className="space-y-4">
          {offered.map((relation) => (
            <CenterServiceCard
              key={relation.id}
              relation={relation}
              onToggle={() => {
                if (!relation.id) return
                if (relation.active) deactivate.mutate({ path: { id: relation.id } })
                else activate.mutate({ path: { id: relation.id } })
              }}
              toggling={activate.isPending || deactivate.isPending}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function CenterServiceCard({ relation, onToggle, toggling }: {
  relation: CenterServiceResponse
  onToggle: () => void
  toggling: boolean
}) {
  return (
    <Card className={relation.active ? undefined : "opacity-70"}>
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <div>
            <CardTitle className="text-base">{relation.service?.name ?? "Servicio"}</CardTitle>
            <CardDescription className="mt-1">
              {relation.service?.description ?? ""}
              {relation.service?.durationMinutes != null && ` · ${relation.service.durationMinutes} min`}
              {!relation.active && " · Inactivo en este centro"}
            </CardDescription>
          </div>
          <Button size="sm" variant="outline" disabled={toggling} onClick={onToggle}>
            <IconPower />{relation.active ? "Retirar" : "Reactivar"}
          </Button>
        </div>
      </CardHeader>
      {relation.id && (
        <CardContent>
          <ServiceProfessionals centerServiceId={relation.id} serviceActive={relation.active ?? false} />
        </CardContent>
      )}
    </Card>
  )
}

function ServiceProfessionals({ centerServiceId, serviceActive }: {
  centerServiceId: string
  serviceActive: boolean
}) {
  const queryClient = useQueryClient()
  const assignments = useQuery(listProfessionalAssignmentsOptions({ path: { centerServiceId } }))
  const users = useQuery(findAllOptions())
  const [selectedProfessionalId, setSelectedProfessionalId] = useState<string>("")

  const invalidate = () => queryClient.invalidateQueries({
    queryKey: listProfessionalAssignmentsQueryKey({ path: { centerServiceId } }),
  })
  const assign = useMutation({ ...assignProfessionalMutation(), onSuccess: () => { invalidate(); setSelectedProfessionalId("") } })
  const mutationError = assign.error

  const eligible = (users.data ?? []).filter(
    (user) =>
      user.active &&
      user.roles?.some((role) => role.toUpperCase() === "PROFESIONAL_CENTRO") &&
      !(assignments.data ?? []).some((assignment) => assignment.professionalId === user.id && assignment.active),
  )

  return (
    <div className="space-y-3 border-t pt-4">
      <h3 className="flex items-center gap-2 text-sm font-semibold"><IconStethoscope className="size-4" />Profesionales asignados</h3>

      {mutationError && (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>{mutationError.message ?? "No se pudo asignar el profesional."}</AlertTitle>
        </Alert>
      )}

      <div className="flex flex-col gap-2 sm:flex-row">
        <Select value={selectedProfessionalId} onValueChange={(value) => setSelectedProfessionalId(value ?? "")} disabled={!users.data || !serviceActive}>
          <SelectTrigger className="flex-1" aria-label="Profesional para asignar">
            <SelectValue placeholder={
              !serviceActive ? "Reactivá el servicio para asignar profesionales" : users.isPending ? "Cargando profesionales…" : "Seleccioná un profesional activo"
            } />
          </SelectTrigger>
          <SelectContent>
            {eligible.map((user) => (
              <SelectItem key={user.id} value={String(user.id)}>{user.name} — {user.email}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          size="sm"
          variant="outline"
          disabled={!selectedProfessionalId || assign.isPending || !serviceActive}
          onClick={() => selectedProfessionalId && assign.mutate({
            path: { centerServiceId },
            body: { professionalId: Number(selectedProfessionalId) },
          })}
        >
          <IconPlus />{assign.isPending ? "Asignando…" : "Asignar"}
        </Button>
      </div>

      {assignments.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando profesionales…</p>
      ) : assignments.isError ? (
        <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
          <p>No se pudieron cargar los profesionales.</p>
          <Button size="sm" variant="outline" onClick={() => assignments.refetch()}>Reintentar</Button>
        </div>
      ) : (assignments.data ?? []).length === 0 ? (
        <p className="text-sm text-muted-foreground">Sin profesionales asignados a este servicio.</p>
      ) : (
        <ul className="divide-y rounded-lg border">
          {(assignments.data ?? []).map((assignment) => (
            <li key={assignment.id} className="flex items-center justify-between gap-2 px-3 py-2 text-sm">
              <span>
                <span className="font-medium">{assignment.professionalName}</span>
                <span className="text-muted-foreground"> — {assignment.professionalEmail}</span>
                {!assignment.active && <span className="text-muted-foreground"> (inactivo)</span>}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
