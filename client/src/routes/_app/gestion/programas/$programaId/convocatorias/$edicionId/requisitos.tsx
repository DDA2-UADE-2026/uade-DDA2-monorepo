import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { FormField, LoadingOrError, RoutePanel, inputClass, requirementLabels } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create4Mutation, delete4Mutation, findAll2Options, findAll2QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateProgramRequirementRequest } from "@/generated/types.gen"
import { zCreateProgramRequirementRequest } from "@/generated/zod.gen"

const createRequirementSchema = zCreateProgramRequirementRequest.extend({
  type: zCreateProgramRequirementRequest.shape.type.unwrap(),
  value: zCreateProgramRequirementRequest.shape.value.trim().min(1, "Ingresá el valor del requisito."),
})

type RequirementFormValues = Omit<CreateProgramRequirementRequest, "type"> & {
  type: NonNullable<CreateProgramRequirementRequest["type"]>
}

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/requisitos",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { edicionId } = Route.useParams()
  const client = useQueryClient()
  const query = useQuery(findAll2Options({ path: { editionId: edicionId } }))
  const refresh = () => client.invalidateQueries({ queryKey: findAll2QueryKey({ path: { editionId: edicionId } }) })
  const create = useMutation({ ...create4Mutation(), onSuccess: () => { form.reset(); refresh() } })
  const remove = useMutation({ ...delete4Mutation(), onSuccess: refresh })
  const form = useForm({
    defaultValues: { type: "MIN_AGE", value: "" } as RequirementFormValues,
    validators: { onChange: createRequirementSchema },
    onSubmit: ({ value }) => create.mutate({
      path: { editionId: edicionId },
      body: value,
    }),
  })

  return <RoutePanel><form className="mb-6 grid gap-3 rounded-lg border p-4" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}><h3 className="font-semibold">Agregar requisito</h3><div className="grid gap-3 sm:grid-cols-2">
    <form.Field name="type" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Tipo" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><Select name={field.name} value={field.state.value} onValueChange={(nextType) => field.handleChange(nextType as typeof field.state.value)}><SelectTrigger id={field.name} className="w-full" onBlur={field.handleBlur} aria-invalid={invalid}><SelectValue>{requirementLabels[field.state.value]}</SelectValue></SelectTrigger><SelectContent>{Object.entries(requirementLabels).map(([key, label]) => <SelectItem key={key} value={key}>{label}</SelectItem>)}</SelectContent></Select></FormField>
    }} />
    <form.Field name="value" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Valor" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} /></FormField>
    }} />
  </div>
    <form.Field name="description" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Descripción" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value || undefined)} aria-invalid={invalid} /></FormField>
    }} />
    <div className="flex justify-end"><Button type="submit" disabled={create.isPending}>Agregar</Button></div></form>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />{query.data?.length === 0 ? <p className="text-sm text-muted-foreground">No hay requisitos configurados.</p> : <Table><TableHeader><TableRow><TableHead>Tipo</TableHead><TableHead>Valor</TableHead><TableHead>Descripción</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data?.map((item) => <TableRow key={item.id}><TableCell>{item.type ? requirementLabels[item.type] : "—"}</TableCell><TableCell>{item.value}</TableCell><TableCell>{item.description || "—"}</TableCell><TableCell>{item.id && <DeleteConfirmationButton description="Se eliminará este requisito." disabled={remove.isPending} size="sm" onConfirm={() => remove.mutate({ path: { editionId: edicionId, requirementId: item.id! } })} />}</TableCell></TableRow>)}</TableBody></Table>}
  </RoutePanel>
}
