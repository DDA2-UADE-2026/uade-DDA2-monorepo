import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { benefitLabels, FormField, LoadingOrError, RoutePanel, inputClass } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create5Mutation, delete5Mutation, findAll3Options, findAll3QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateProgramBenefitRequest } from "@/generated/types.gen"
import { zCreateProgramBenefitRequest } from "@/generated/zod.gen"

const createBenefitSchema = zCreateProgramBenefitRequest.extend({
  benefitType: zCreateProgramBenefitRequest.shape.benefitType.unwrap(),
})

type BenefitFormValues = Omit<CreateProgramBenefitRequest, "benefitType"> & {
  benefitType: NonNullable<CreateProgramBenefitRequest["benefitType"]>
}

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/beneficios",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { edicionId } = Route.useParams()
  const client = useQueryClient()
  const query = useQuery(findAll3Options({ path: { editionId: edicionId } }))
  const refresh = () => client.invalidateQueries({ queryKey: findAll3QueryKey({ path: { editionId: edicionId } }) })
  const create = useMutation({ ...create5Mutation(), onSuccess: () => { form.reset(); refresh() } })
  const remove = useMutation({ ...delete5Mutation(), onSuccess: refresh })
  const form = useForm({
    defaultValues: { benefitType: "FOOD_ASSISTANCE" } as BenefitFormValues,
    validators: { onChange: createBenefitSchema },
    onSubmit: ({ value }) => create.mutate({
      path: { editionId: edicionId },
      body: value,
    }),
  })

  return <RoutePanel><form className="mb-6 grid gap-3 rounded-lg border p-4" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}><h3 className="font-semibold">Agregar beneficio</h3><div className="grid gap-3 sm:grid-cols-2">
    <form.Field name="benefitType" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Tipo" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><Select name={field.name} value={field.state.value} onValueChange={(nextType) => field.handleChange(nextType as typeof field.state.value)}><SelectTrigger id={field.name} className="w-full" onBlur={field.handleBlur} aria-invalid={invalid}><SelectValue /></SelectTrigger><SelectContent>{Object.entries(benefitLabels).map(([key, label]) => <SelectItem key={key} value={key}>{label}</SelectItem>)}</SelectContent></Select></FormField>
    }} />
    <form.Field name="amount" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Monto" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} type="number" step="0.01" value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value ? Number(event.target.value) : undefined)} aria-invalid={invalid} /></FormField>
    }} />
  </div>
    <form.Field name="description" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Descripción" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value || undefined)} aria-invalid={invalid} /></FormField>
    }} />
    <div className="flex justify-end"><Button type="submit" disabled={create.isPending}>Agregar</Button></div></form>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />{query.data?.length === 0 ? <p className="text-sm text-muted-foreground">No hay beneficios configurados.</p> : <Table><TableHeader><TableRow><TableHead>Tipo</TableHead><TableHead>Descripción</TableHead><TableHead>Monto</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data?.map((item) => <TableRow key={item.id}><TableCell>{item.benefitType ? benefitLabels[item.benefitType] : "—"}</TableCell><TableCell>{item.description || "—"}</TableCell><TableCell>{item.amount == null ? "—" : new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(item.amount)}</TableCell><TableCell>{item.id && <DeleteConfirmationButton description="Se eliminará este beneficio." disabled={remove.isPending} size="sm" onConfirm={() => remove.mutate({ path: { editionId: edicionId, benefitId: item.id! } })} />}</TableCell></TableRow>)}</TableBody></Table>}
  </RoutePanel>
}
