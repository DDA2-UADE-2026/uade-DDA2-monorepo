import { IconAlertTriangle } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { z } from "zod"

import { DAYS, dayLabel, type DayOfWeekValue, type TimeRangeValue } from "@/components/centros/timeRanges"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Checkbox } from "@/components/ui/checkbox"
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
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { ErrorResponse } from "@/generated/types.gen"

const dayEnum = z.enum(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"])

const timeRangeSchema = z
  .object({
    dayOfWeek: dayEnum,
    days: z.array(dayEnum).min(1, "Elegí al menos un día."),
    startTime: z.string().min(1, "Ingresá la hora de inicio."),
    endTime: z.string().min(1, "Ingresá la hora de fin."),
  })
  .refine((value) => value.startTime < value.endTime, {
    message: "El inicio debe ser anterior al fin.",
    path: ["endTime"],
  })

export interface TimeRangeSubmit extends TimeRangeValue {
  days: DayOfWeekValue[]
}

export function TimeRangeDialog({ title, description, initial, allowMultipleDays, pending, error, submitLabel, onSubmit, onOpenChange }: {
  title: string
  description: string
  initial: TimeRangeValue
  allowMultipleDays?: boolean
  pending: boolean
  error: ErrorResponse | null
  submitLabel: string
  onSubmit: (value: TimeRangeSubmit) => void
  onOpenChange: (open: boolean) => void
}) {
  const form = useForm({
    defaultValues: { ...initial, days: [initial.dayOfWeek] },
    validators: { onChange: timeRangeSchema },
    onSubmit: ({ value }) => {
      if (!pending) onSubmit(value)
    },
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!pending) onOpenChange(open) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        {error && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>{error.message ?? "No se pudo guardar la franja."}</AlertTitle>
            {error.fields && error.fields.length > 0 && (
              <AlertDescription>
                <ul className="list-disc pl-4">{error.fields.map((field, index) => <li key={`${field.field}-${index}`}>{field.message}</li>)}</ul>
              </AlertDescription>
            )}
          </Alert>
        )}

        <form
          id="time-range-form"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            event.stopPropagation()
            form.handleSubmit()
          }}
        >
          <FieldGroup>
            {allowMultipleDays ? (
              <form.Field name="days" children={(field) => (
                <Field>
                  <FieldLabel>Días</FieldLabel>
                  <div className="flex flex-wrap gap-2">
                    {DAYS.map((day) => {
                      const checked = field.state.value.includes(day.value)
                      return (
                        <label
                          key={day.value}
                          className="flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm transition-colors has-checked:border-primary has-checked:bg-primary/10"
                        >
                          <Checkbox
                            checked={checked}
                            onCheckedChange={(next) => {
                              const selected = field.state.value.filter((value) => value !== day.value)
                              field.handleChange(next === true ? [...selected, day.value] : selected)
                            }}
                            aria-label={day.label}
                          />
                          {day.label}
                        </label>
                      )
                    })}
                  </div>
                  {field.state.meta.isTouched && !field.state.meta.isValid && (
                    <FieldError errors={field.state.meta.errors} />
                  )}
                </Field>
              )} />
            ) : (
              <form.Field name="dayOfWeek" children={(field) => (
                <Field>
                  <FieldLabel htmlFor={field.name}>Día</FieldLabel>
                  <Select value={field.state.value} onValueChange={(value) => field.handleChange(value as DayOfWeekValue)}>
                    <SelectTrigger id={field.name} aria-label="Día de la semana">
                      <SelectValue>
                        {(value: string | null) => (value ? dayLabel(value) : "Seleccioná un día")}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {DAYS.map((day) => (
                        <SelectItem key={day.value} value={day.value}>{day.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              )} />
            )}

            <div className="grid grid-cols-2 gap-3">
              <form.Field name="startTime" children={(field) => {
                const invalid = field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={invalid}>
                    <FieldLabel htmlFor={field.name}>Inicio</FieldLabel>
                    <Input
                      id={field.name}
                      name={field.name}
                      type="time"
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                    />
                    {invalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                )
              }} />

              <form.Field name="endTime" children={(field) => {
                const invalid = field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={invalid}>
                    <FieldLabel htmlFor={field.name}>Fin</FieldLabel>
                    <Input
                      id={field.name}
                      name={field.name}
                      type="time"
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                    />
                    {invalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                )
              }} />
            </div>
          </FieldGroup>
        </form>

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={pending} />}>
            Cancelar
          </DialogClose>
          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
            <Button type="submit" form="time-range-form" disabled={!canSubmit || isSubmitting || pending}>
              {pending ? "Guardando…" : submitLabel}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
