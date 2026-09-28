import { IconAlertTriangle, IconMapPin, IconUsers } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"

import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import {
  create8Mutation,
  get1Options,
  get1QueryKey,
  list5QueryKey,
  update7Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ActivityResponse } from "@/generated/types.gen"
import { zCreateActivityRequest } from "@/generated/zod.gen"

// El contrato generado deja fechas y cupo como opcionales porque el OpenAPI no
// los marca `required`, pero el backend los exige y valida el rango.
const activityFormSchema = zCreateActivityRequest
  .extend({
    name: zCreateActivityRequest.shape.name.trim().min(1, "Ingresá el nombre de la actividad."),
    description: zCreateActivityRequest.shape.description.trim().min(1, "Ingresá la descripción de la actividad."),
    location: zCreateActivityRequest.shape.location.trim().min(1, "Ingresá el lugar de la actividad."),
    startDate: z.iso.date({ error: "Elegí la fecha de inicio." }),
    endDate: z.iso.date({ error: "Elegí la fecha de finalización." }),
    capacity: z.int({ error: "Ingresá el cupo." }).gte(1, "El cupo debe ser mayor a cero."),
  })
  .refine((value) => value.endDate >= value.startDate, {
    error: "La fecha de finalización no puede ser anterior a la de inicio.",
    path: ["endDate"],
  })

/**
 * Alta y edición de actividades. Al editar carga el detalle completo, porque el
 * item del listado no trae la descripción y enviarla vacía la borraría.
 */
export function ActivityDialog({ activityId, onOpenChange, onSaved }: {
  activityId?: string
  onOpenChange: (open: boolean) => void
  onSaved?: (activity: ActivityResponse) => void
}) {
  const isEditing = activityId !== undefined
  const detail = useQuery({
    ...get1Options({ path: { id: activityId ?? "" } }),
    enabled: isEditing,
  })
  const loading = isEditing && detail.isPending

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar actividad" : "Nueva actividad"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Solo se pueden modificar actividades en borrador."
              : "La actividad se crea en borrador; la publicación es un segundo paso."}
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="space-y-4" role="status" aria-label="Cargando actividad">
            <Skeleton className="h-9 w-full" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-9 w-full" />
            <div className="grid gap-4 sm:grid-cols-2">
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </div>
          </div>
        ) : isEditing && detail.isError ? (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No pudimos cargar la actividad</AlertTitle>
            <AlertDescription className="flex flex-col items-start gap-3">
              <span>{detail.error.message ?? "Intentá nuevamente en unos instantes."}</span>
              <Button size="sm" variant="outline" onClick={() => detail.refetch()}>Reintentar</Button>
            </AlertDescription>
          </Alert>
        ) : (
          <ActivityForm
            activity={detail.data ?? null}
            onOpenChange={onOpenChange}
            onSaved={onSaved}
          />
        )}

        {loading && (
          <DialogFooter>
            <DialogClose render={<Button type="button" variant="outline" />}>Cancelar</DialogClose>
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  )
}

function ActivityForm({ activity, onOpenChange, onSaved }: {
  activity: ActivityResponse | null
  onOpenChange: (open: boolean) => void
  onSaved?: (activity: ActivityResponse) => void
}) {
  const queryClient = useQueryClient()
  const isEditing = activity !== null

  const onSuccess = (saved: ActivityResponse) => {
    queryClient.invalidateQueries({ queryKey: list5QueryKey() })
    if (saved.id) queryClient.invalidateQueries({ queryKey: get1QueryKey({ path: { id: saved.id } }) })
    onSaved?.(saved)
    onOpenChange(false)
  }
  const create = useMutation({ ...create8Mutation(), onSuccess })
  const update = useMutation({ ...update7Mutation(), onSuccess })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error

  const form = useForm({
    defaultValues: {
      name: activity?.name ?? "",
      description: activity?.description ?? "",
      location: activity?.location ?? "",
      startDate: activity?.startDate ?? "",
      endDate: activity?.endDate ?? "",
      capacity: activity?.capacity as number | undefined,
    },
    validators: { onChange: activityFormSchema },
    onSubmit: ({ value }) => {
      if (isPending) return
      create.reset()
      update.reset()
      const body = {
        name: value.name.trim(),
        description: value.description.trim(),
        location: value.location.trim(),
        startDate: value.startDate,
        endDate: value.endDate,
        capacity: value.capacity,
      }
      if (activity?.id) {
        update.mutate({ path: { id: activity.id }, body })
      } else {
        create.mutate({ body })
      }
    },
  })

  return (
    <>
      {mutationError && (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>{mutationError.message ?? "No se pudo guardar la actividad."}</AlertTitle>
          {mutationError.fields && mutationError.fields.length > 0 && (
            <AlertDescription>
              <ul className="list-disc pl-4">
                {mutationError.fields.map((field, index) => (
                  <li key={`${field.field}-${index}`}>{field.message}</li>
                ))}
              </ul>
            </AlertDescription>
          )}
        </Alert>
      )}

      <form
        id="activity-form"
        noValidate
        onSubmit={(event) => {
          event.preventDefault()
          event.stopPropagation()
          form.handleSubmit()
        }}
      >
        <FieldGroup>
          <form.Field name="name" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <Field data-invalid={invalid}>
                <FieldLabel htmlFor={field.name}>Nombre</FieldLabel>
                <InputGroup>
                  <InputGroupInput
                    id={field.name}
                    name={field.name}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                    aria-invalid={invalid}
                    placeholder="Taller comunitario de RCP"
                    maxLength={200}
                    autoFocus
                  />
                </InputGroup>
                {invalid && <FieldError errors={field.state.meta.errors} />}
              </Field>
            )
          }} />

          <form.Field name="description" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <Field data-invalid={invalid}>
                <FieldLabel htmlFor={field.name}>Descripción</FieldLabel>
                <Textarea
                  id={field.name}
                  name={field.name}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(event.target.value)}
                  aria-invalid={invalid}
                  placeholder="Capacitación abierta sobre técnicas básicas de reanimación."
                  className="min-h-24"
                />
                {invalid && <FieldError errors={field.state.meta.errors} />}
              </Field>
            )
          }} />

          <form.Field name="location" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <Field data-invalid={invalid}>
                <FieldLabel htmlFor={field.name}>Lugar</FieldLabel>
                <InputGroup>
                  <InputGroupAddon><IconMapPin /></InputGroupAddon>
                  <InputGroupInput
                    id={field.name}
                    name={field.name}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                    aria-invalid={invalid}
                    placeholder="Centro Municipal Norte"
                    maxLength={300}
                  />
                </InputGroup>
                {invalid && <FieldError errors={field.state.meta.errors} />}
              </Field>
            )
          }} />

          <div className="grid gap-4 sm:grid-cols-2">
            <form.Field name="startDate" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel>Fecha de inicio</FieldLabel>
                  <ProgramDatePicker value={field.state.value} onChange={field.handleChange} />
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Subscribe selector={(state) => state.values.startDate} children={(startDate) => (
              <form.Field name="endDate" children={(field) => {
                const invalid = field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={invalid}>
                    <FieldLabel>Fecha de finalización</FieldLabel>
                    <ProgramDatePicker
                      value={field.state.value}
                      onChange={field.handleChange}
                      disabled={startDate ? { before: parseLocalDate(startDate)! } : undefined}
                    />
                    {invalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                )
              }} />
            )} />
          </div>

          <form.Field name="capacity" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <Field data-invalid={invalid}>
                <FieldLabel htmlFor={field.name}>Cupo</FieldLabel>
                <InputGroup>
                  <InputGroupAddon><IconUsers /></InputGroupAddon>
                  <InputGroupInput
                    id={field.name}
                    name={field.name}
                    type="number"
                    min={1}
                    value={field.state.value ?? ""}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value ? Number(event.target.value) : undefined)}
                    aria-invalid={invalid}
                    placeholder="30"
                  />
                </InputGroup>
                {invalid && <FieldError errors={field.state.meta.errors} />}
              </Field>
            )
          }} />
        </FieldGroup>
      </form>

      <DialogFooter>
        <DialogClose render={<Button type="button" variant="outline" disabled={isPending} />}>
          Cancelar
        </DialogClose>
        <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
          <Button type="submit" form="activity-form" disabled={!canSubmit || isSubmitting || isPending}>
            {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear actividad"}
          </Button>
        )} />
      </DialogFooter>
    </>
  )
}
