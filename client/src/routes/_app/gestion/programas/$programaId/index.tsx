import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect } from "react"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { FormField, LoadingOrError, RoutePanel, inputClass, textareaClass } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { delete2Mutation, findById2Options, findById2QueryKey, listQueryKey, update2Mutation } from "@/generated/@tanstack/react-query.gen"
import type { UpdateProgramRequest } from "@/generated/types.gen"
import { zUpdateProgramRequest } from "@/generated/zod.gen"

const updateProgramSchema = zUpdateProgramRequest.extend({
  name: zUpdateProgramRequest.shape.name.trim().min(1, "Ingresá el nombre del programa."),
})

export const Route = createFileRoute("/_app/gestion/programas/$programaId/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const query = useQuery(findById2Options({ path: { id: programaId } }))
  const update = useMutation({ ...update2Mutation(), onSuccess: () => { client.invalidateQueries({ queryKey: findById2QueryKey({ path: { id: programaId } }) }); client.invalidateQueries({ queryKey: listQueryKey() }) }, onError: showApiErrorToast })
  const remove = useMutation({ ...delete2Mutation(), onSuccess: () => { client.invalidateQueries({ queryKey: listQueryKey() }); navigate({ to: "/gestion/programas", search: { page: 1 } }) }, onError: showApiErrorToast })
  const form = useForm({
    defaultValues: { name: "" } as UpdateProgramRequest,
    validators: { onChange: updateProgramSchema },
    onSubmit: ({ value }) => update.mutate({
      path: { id: programaId },
      body: value,
    }),
  })
  useEffect(() => {
    if (query.data) form.reset({ name: query.data.name ?? "", objective: query.data.objective })
  }, [form, query.data])

  return <RoutePanel>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
    {query.data && <form className="grid gap-5" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}>
      <form.Field name="name" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Nombre" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} /></FormField>
      }} />
      <form.Field name="objective" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Objetivo" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><textarea id={field.name} name={field.name} className={textareaClass} value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value || undefined)} aria-invalid={invalid} /></FormField>
      }} />
      <div className="flex justify-between gap-2"><DeleteConfirmationButton description="Se eliminará este programa." disabled={remove.isPending} onConfirm={() => remove.mutateAsync({ path: { id: programaId } })} /><Button type="submit" disabled={update.isPending}>{update.isPending ? "Guardando…" : "Guardar cambios"}</Button></div>
    </form>}
  </RoutePanel>
}
