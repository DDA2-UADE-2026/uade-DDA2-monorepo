import { useForm } from "@tanstack/react-form"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { FormField, RoutePanel, inputClass, parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { create6Mutation, list1QueryKey } from "@/generated/@tanstack/react-query.gen"
import { zCreateProgramEditionRequest } from "@/generated/zod.gen"

const createEditionSchema = zCreateProgramEditionRequest.required().extend({
  name: zCreateProgramEditionRequest.shape.name.trim().min(1, "Ingresá el nombre de la convocatoria."),
})

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/nueva",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const create = useMutation({ ...create6Mutation(), onSuccess: (data) => { client.invalidateQueries({ queryKey: list1QueryKey({ path: { programId: programaId } }) }); navigate({ to: "/gestion/programas/$programaId/convocatorias/$edicionId", params: { programaId, edicionId: data.id ?? "" } }) } })
  const form = useForm({
    defaultValues: { name: "", startDate: "", endDate: "", maxCapacity: undefined as number | undefined },
    validators: { onChange: createEditionSchema },
    onSubmit: ({ value }) => create.mutate({
      path: { programId: programaId },
      body: { ...value, startDate: value.startDate || undefined, endDate: value.endDate || undefined },
    }),
  })

  return <RoutePanel><h2 className="mb-5 text-lg font-semibold">Nueva convocatoria</h2><form className="grid gap-5" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}>
    <form.Field name="name" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Nombre" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} /></FormField>
    }} />
    <div className="grid gap-4 sm:grid-cols-2">
      <form.Field name="startDate" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Fecha de inicio" invalid={invalid} errors={field.state.meta.errors}><ProgramDatePicker value={field.state.value} onChange={field.handleChange} /></FormField>
      }} />
      <form.Subscribe selector={(state) => state.values.startDate} children={(startDate) => <form.Field name="endDate" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Fecha de finalización" invalid={invalid} errors={field.state.meta.errors}><ProgramDatePicker value={field.state.value} onChange={field.handleChange} disabled={startDate ? { before: parseLocalDate(startDate)! } : undefined} /></FormField>
      }} />} />
    </div>
    <form.Field name="maxCapacity" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Capacidad máxima" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} type="number" value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value ? Number(event.target.value) : undefined)} aria-invalid={invalid} /></FormField>
    }} />
    {create.isError && <p className="text-sm text-destructive">No se pudo crear la convocatoria.</p>}<div className="flex justify-end"><Button type="submit" disabled={create.isPending}>{create.isPending ? "Creando…" : "Crear convocatoria"}</Button></div>
  </form></RoutePanel>
}
