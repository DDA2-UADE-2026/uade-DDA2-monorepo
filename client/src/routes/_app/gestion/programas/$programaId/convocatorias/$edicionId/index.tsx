import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect, useState } from "react"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { FormField, LoadingOrError, RoutePanel, inputClass, parseLocalDate, statusLabels } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { activateMutation, closeMutation, delete3Mutation, findById3Options, findById3QueryKey, list1QueryKey, suspendMutation, update3Mutation } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId, edicionId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const query = useQuery(findById3Options({ path: { id: edicionId } }))
  const [form, setForm] = useState({ name: "", startDate: "", endDate: "", maxCapacity: "" })
  useEffect(() => {
    // The form mirrors the latest server snapshot after a fetch or mutation.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (query.data) setForm({ name: query.data.name ?? "", startDate: query.data.startDate ?? "", endDate: query.data.endDate ?? "", maxCapacity: query.data.maxCapacity?.toString() ?? "" })
  }, [query.data])
  const refresh = () => { client.invalidateQueries({ queryKey: findById3QueryKey({ path: { id: edicionId } }) }); client.invalidateQueries({ queryKey: list1QueryKey({ path: { programId: programaId } }) }) }
  const update = useMutation({ ...update3Mutation(), onSuccess: refresh })
  const activate = useMutation({ ...activateMutation(), onSuccess: refresh })
  const suspend = useMutation({ ...suspendMutation(), onSuccess: refresh })
  const close = useMutation({ ...closeMutation(), onSuccess: refresh, onError: refresh })
  const remove = useMutation({ ...delete3Mutation(), onSuccess: () => { refresh(); navigate({ to: "/gestion/programas/$programaId/convocatorias", params: { programaId }, search: { page: 1 } }) } })
  const busy = update.isPending || activate.isPending || suspend.isPending || close.isPending || remove.isPending
  const resetOperationErrors = () => { update.reset(); activate.reset(); suspend.reset(); close.reset(); remove.reset() }
  const hasOperationError = update.isError || activate.isError || suspend.isError || remove.isError || (close.isError && !query.isFetching && query.data?.status !== "CLOSED")
  return <RoutePanel><LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />{query.data && <form className="grid gap-5" onSubmit={(event) => { event.preventDefault(); resetOperationErrors(); update.mutate({ path: { id: edicionId }, body: { name: form.name, startDate: form.startDate || undefined, endDate: form.endDate || undefined, maxCapacity: form.maxCapacity ? Number(form.maxCapacity) : undefined } }) }}>
    <div className="flex items-center justify-between"><span className="rounded-full border px-3 py-1 text-xs">{query.data.status ? statusLabels[query.data.status] : "—"}</span><div className="flex flex-wrap gap-2">{query.data.status !== "ACTIVE" && query.data.status !== "CLOSED" && <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => { resetOperationErrors(); activate.mutate({ path: { id: edicionId } }) }}>Activar</Button>}{query.data.status === "ACTIVE" && <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => { resetOperationErrors(); suspend.mutate({ path: { id: edicionId } }) }}>Suspender</Button>}{query.data.status !== "CLOSED" && <Button type="button" size="sm" variant="outline" disabled={busy} onClick={() => { resetOperationErrors(); close.mutate({ path: { id: edicionId } }) }}>Cerrar</Button>}</div></div>
    <FormField label="Nombre"><input className={inputClass} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></FormField>
    <div className="grid gap-4 sm:grid-cols-2"><FormField label="Inicio"><ProgramDatePicker value={form.startDate} onChange={(startDate) => setForm({ ...form, startDate })} /></FormField><FormField label="Finalización"><ProgramDatePicker value={form.endDate} onChange={(endDate) => setForm({ ...form, endDate })} disabled={form.startDate ? { before: parseLocalDate(form.startDate)! } : undefined} /></FormField></div>
    <FormField label="Capacidad máxima"><input className={inputClass} type="number" min={1} value={form.maxCapacity} onChange={(e) => setForm({ ...form, maxCapacity: e.target.value })} /></FormField>
    <p className="text-sm text-muted-foreground">Inscriptos actuales: {query.data.currentEnrollment ?? 0}</p>
    {hasOperationError && <p className="text-sm text-destructive">No se pudo completar la operación.</p>}
    <div className="flex justify-between"><DeleteConfirmationButton description="Se eliminará esta convocatoria." disabled={busy} onConfirm={() => { resetOperationErrors(); remove.mutate({ path: { id: edicionId } }) }} /><Button type="submit" disabled={busy}>Guardar cambios</Button></div>
  </form>}</RoutePanel>
}
