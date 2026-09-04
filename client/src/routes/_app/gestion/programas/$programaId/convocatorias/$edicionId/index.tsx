import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect, useState } from "react"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { FormField, LoadingOrError, RoutePanel, inputClass, parseLocalDate, statusLabels } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { activateMutation, closeMutation, delete3Mutation, findById3Options, findById3QueryKey, list1QueryKey, suspendMutation, update3Mutation } from "@/generated/@tanstack/react-query.gen"
import { zUpdateProgramEditionRequest } from "@/generated/zod.gen"

const updateEditionSchema = zUpdateProgramEditionRequest.required().extend({
  name: zUpdateProgramEditionRequest.shape.name.trim().min(1, "Ingresá el nombre de la convocatoria."),
})

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId, edicionId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [closeDialogOpen, setCloseDialogOpen] = useState(false)
  const query = useQuery(findById3Options({ path: { id: edicionId } }))
  const refresh = () => { client.invalidateQueries({ queryKey: findById3QueryKey({ path: { id: edicionId } }) }); client.invalidateQueries({ queryKey: list1QueryKey({ path: { programId: programaId } }) }) }
  const update = useMutation({ ...update3Mutation(), onSuccess: refresh, onError: showApiErrorToast })
  const activate = useMutation({ ...activateMutation(), onSuccess: refresh, onError: showApiErrorToast })
  const suspend = useMutation({ ...suspendMutation(), onSuccess: refresh, onError: showApiErrorToast })
  const close = useMutation({ ...closeMutation(), onSuccess: () => { refresh(); setCloseDialogOpen(false) }, onError: (error) => { refresh(); showApiErrorToast(error) } })
  const remove = useMutation({ ...delete3Mutation(), onSuccess: () => { refresh(); navigate({ to: "/gestion/programas/$programaId/convocatorias", params: { programaId }, search: { page: 1 } }) }, onError: showApiErrorToast })
  const busy = update.isPending || activate.isPending || suspend.isPending || close.isPending || remove.isPending
  const resetOperationErrors = () => { update.reset(); activate.reset(); suspend.reset(); close.reset(); remove.reset() }
  const form = useForm({
    defaultValues: { name: "", startDate: "", endDate: "", maxCapacity: undefined as number | undefined },
    validators: { onChange: updateEditionSchema },
    onSubmit: ({ value }) => {
      resetOperationErrors()
      update.mutate({
        path: { id: edicionId },
        body: { ...value, startDate: value.startDate || undefined, endDate: value.endDate || undefined },
      })
    },
  })
  useEffect(() => {
    if (query.data) form.reset({ name: query.data.name ?? "", startDate: query.data.startDate ?? "", endDate: query.data.endDate ?? "", maxCapacity: query.data.maxCapacity })
  }, [form, query.data])
  const isClosed = query.data?.status === "CLOSED"
  return <RoutePanel><LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />{query.data && <form className="grid gap-5" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}>
    <div className="flex items-center justify-between">
      <span className={isClosed ? "rounded-full border border-destructive/40 bg-destructive/10 px-3 py-1 text-xs font-medium text-destructive" : "rounded-full border px-3 py-1 text-xs"}>
        {query.data.status ? statusLabels[query.data.status] : "—"}
      </span>
      <div className="flex flex-wrap gap-2">
        {query.data.status !== "ACTIVE" && query.data.status !== "CLOSED" && (
          <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => { resetOperationErrors(); activate.mutate({ path: { id: edicionId } }) }}>
            Activar
          </Button>
        )}
        {query.data.status === "ACTIVE" && (
          <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => { resetOperationErrors(); suspend.mutate({ path: { id: edicionId } }) }}>
            Suspender
          </Button>
        )}
        {query.data.status !== "CLOSED" && (
          <AlertDialog
            open={closeDialogOpen}
            onOpenChange={(open) => {
              if (!close.isPending) setCloseDialogOpen(open)
            }}
          >
            <AlertDialogTrigger
              disabled={busy}
              render={<Button type="button" size="sm" variant="outline" />}
            >
              Cerrar
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>¿Cerrar esta convocatoria?</AlertDialogTitle>
                <AlertDialogDescription>
                  La convocatoria quedará disponible sólo para consulta. Esta acción es definitiva y no se puede deshacer.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel disabled={close.isPending}>Cancelar</AlertDialogCancel>
                <AlertDialogAction
                  type="button"
                  variant="destructive"
                  disabled={close.isPending}
                  onClick={() => {
                    resetOperationErrors()
                    close.mutate({ path: { id: edicionId } })
                  }}
                >
                  {close.isPending ? "Cerrando…" : "Cerrar convocatoria"}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        )}
      </div>
    </div>
    {isClosed && <Alert variant="destructive"><AlertTitle>Convocatoria cerrada</AlertTitle><AlertDescription>Ya no se pueden guardar cambios ni eliminar esta convocatoria.</AlertDescription></Alert>}
    <form.Field name="name" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Nombre" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} /></FormField>
    }} />
    <div className="grid gap-4 sm:grid-cols-2">
      <form.Field name="startDate" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Inicio" invalid={invalid} errors={field.state.meta.errors}><ProgramDatePicker value={field.state.value} onChange={field.handleChange} /></FormField>
      }} />
      <form.Subscribe selector={(state) => state.values.startDate} children={(startDate) => <form.Field name="endDate" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Finalización" invalid={invalid} errors={field.state.meta.errors}><ProgramDatePicker value={field.state.value} onChange={field.handleChange} disabled={startDate ? { before: parseLocalDate(startDate)! } : undefined} /></FormField>
      }} />} />
    </div>
    <form.Field name="maxCapacity" children={(field) => {
      const invalid = field.state.meta.isTouched && !field.state.meta.isValid
      return <FormField label="Capacidad máxima" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} type="number" value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value ? Number(event.target.value) : undefined)} aria-invalid={invalid} /></FormField>
    }} />
    <p className="text-sm text-muted-foreground">Inscriptos actuales: {query.data.currentEnrollment ?? 0}</p>
    <div className="flex justify-between"><DeleteConfirmationButton description="Se eliminará esta convocatoria." disabled={busy || isClosed} onConfirm={() => { resetOperationErrors(); return remove.mutateAsync({ path: { id: edicionId } }) }} /><Button type="submit" disabled={busy || isClosed}>Guardar cambios</Button></div>
  </form>}</RoutePanel>
}
