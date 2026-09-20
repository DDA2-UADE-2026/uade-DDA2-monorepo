import { IconAlertTriangle, IconMail, IconMapPin, IconPhone } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"

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
import {
  createMunicipalCenterMutation,
  listMunicipalCentersQueryKey,
  updateMunicipalCenterMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { MunicipalCenterResponse } from "@/generated/types.gen"
import { zCreateMunicipalCenterRequest, zUpdateMunicipalCenterRequest } from "@/generated/zod.gen"

// `phone` y `email` son opcionales en el DTO generado, pero el form siempre
// los completa como string (vacío = ausente al enviar).
const optionalEmail = zUpdateMunicipalCenterRequest.shape.email.unwrap()
const phoneField = zUpdateMunicipalCenterRequest.shape.phone.unwrap()
const emailField = z
  .string()
  .max(180)
  .refine((value) => value === "" || optionalEmail.safeParse(value).success, "Ingresá un correo válido.")

const createCenterSchema = zCreateMunicipalCenterRequest.extend({
  name: zCreateMunicipalCenterRequest.shape.name.trim().min(1, "Ingresá el nombre del centro."),
  address: zCreateMunicipalCenterRequest.shape.address.trim().min(1, "Ingresá la dirección del centro."),
  phone: phoneField,
  email: emailField,
})

const updateCenterSchema = zUpdateMunicipalCenterRequest.extend({
  name: zUpdateMunicipalCenterRequest.shape.name.trim().min(1, "Ingresá el nombre del centro."),
  address: zUpdateMunicipalCenterRequest.shape.address.trim().min(1, "Ingresá la dirección del centro."),
  phone: phoneField,
  email: emailField,
})

export function CenterDialog({ center, onOpenChange, onSaved }: {
  center: MunicipalCenterResponse | null
  onOpenChange: (open: boolean) => void
  onSaved?: () => void
}) {
  const queryClient = useQueryClient()
  const isEditing = center !== null
  const create = useMutation({
    ...createMunicipalCenterMutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listMunicipalCentersQueryKey() })
      onSaved?.()
      onOpenChange(false)
    },
  })
  const update = useMutation({
    ...updateMunicipalCenterMutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listMunicipalCentersQueryKey() })
      onSaved?.()
      onOpenChange(false)
    },
  })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error
  const form = useForm({
    defaultValues: {
      name: center?.name ?? "",
      address: center?.address ?? "",
      phone: center?.phone ?? "",
      email: center?.email ?? "",
    },
    validators: { onChange: isEditing ? updateCenterSchema : createCenterSchema },
    onSubmit: ({ value }) => {
      if (isPending) return
      create.reset()
      update.reset()
      const body = {
        name: value.name.trim(),
        address: value.address.trim(),
        phone: value.phone.trim() || undefined,
        email: value.email.trim() || undefined,
      }
      if (center?.id) {
        update.mutate({ path: { id: center.id }, body })
      } else {
        create.mutate({ body })
      }
    },
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!isPending) onOpenChange(open) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar centro" : "Nuevo centro"}</DialogTitle>
          <DialogDescription>
            {isEditing ? "Actualizá los datos del centro municipal." : "Registrá un nuevo centro municipal con su ubicación."}
          </DialogDescription>
        </DialogHeader>

        {mutationError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>{mutationError.message ?? "No se pudo guardar el centro."}</AlertTitle>
            {mutationError.fields && mutationError.fields.length > 0 && (
              <AlertDescription>
                <ul className="list-disc pl-4">{mutationError.fields.map((field, index) => <li key={`${field.field}-${index}`}>{field.message}</li>)}</ul>
              </AlertDescription>
            )}
          </Alert>
        )}

        <form
          id="center-form"
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
                      placeholder="Centro Norte"
                      maxLength={150}
                      autoFocus
                    />
                  </InputGroup>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Field name="address" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Dirección</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon><IconMapPin /></InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                      placeholder="Av. Siempre Viva 123"
                      maxLength={255}
                    />
                  </InputGroup>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Field name="phone" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Teléfono (opcional)</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon><IconPhone /></InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                      placeholder="011 4567-8900"
                      maxLength={30}
                    />
                  </InputGroup>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Field name="email" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Correo (opcional)</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon><IconMail /></InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      type="email"
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                      placeholder="centro@example.com"
                      maxLength={180}
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
            <Button type="submit" form="center-form" disabled={!canSubmit || isSubmitting || isPending}>
              {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear centro"}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
