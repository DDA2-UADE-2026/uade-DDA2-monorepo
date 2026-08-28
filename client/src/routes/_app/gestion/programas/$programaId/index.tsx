import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect, useState } from "react"
import { FormField, LoadingOrError, RoutePanel, inputClass, textareaClass } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { delete2Mutation, findById2Options, findById2QueryKey, listQueryKey, update2Mutation } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute("/_app/gestion/programas/$programaId/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const query = useQuery(findById2Options({ path: { id: programaId } }))
  const [name, setName] = useState("")
  const [objective, setObjective] = useState("")
  useEffect(() => {
    // The form mirrors the latest server snapshot after a fetch or save.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (query.data) { setName(query.data.name ?? ""); setObjective(query.data.objective ?? "") }
  }, [query.data])
  const update = useMutation({ ...update2Mutation(), onSuccess: () => { client.invalidateQueries({ queryKey: findById2QueryKey({ path: { id: programaId } }) }); client.invalidateQueries({ queryKey: listQueryKey() }) } })
  const remove = useMutation({ ...delete2Mutation(), onSuccess: () => { client.invalidateQueries({ queryKey: listQueryKey() }); navigate({ to: "/gestion/programas", search: { page: 1 } }) } })
  return <RoutePanel>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
    {query.data && <form className="grid gap-5" onSubmit={(event) => { event.preventDefault(); update.mutate({ path: { id: programaId }, body: { name, objective: objective || undefined } }) }}>
      {(update.isError || remove.isError) && <Alert variant="destructive"><AlertDescription>No se pudo completar la operación.</AlertDescription></Alert>}
      <FormField label="Nombre"><input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} maxLength={200} required /></FormField>
      <FormField label="Objetivo"><textarea className={textareaClass} value={objective} onChange={(e) => setObjective(e.target.value)} /></FormField>
      <div className="flex justify-between gap-2"><Button type="button" variant="destructive" disabled={remove.isPending} onClick={() => { if (confirm("¿Eliminar este programa?")) remove.mutate({ path: { id: programaId } }) }}>Eliminar</Button><Button type="submit" disabled={update.isPending}>{update.isPending ? "Guardando…" : "Guardar cambios"}</Button></div>
    </form>}
  </RoutePanel>
}
