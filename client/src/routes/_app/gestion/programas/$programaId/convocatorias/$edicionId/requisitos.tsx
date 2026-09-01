import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { FormField, LoadingOrError, RoutePanel, inputClass, requirementLabels, selectClass } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create4Mutation, delete4Mutation, findAll2Options, findAll2QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateProgramRequirementRequest } from "@/generated/types.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/requisitos",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { edicionId } = Route.useParams()
  const client = useQueryClient()
  const query = useQuery(findAll2Options({ path: { editionId: edicionId } }))
  const [type, setType] = useState<CreateProgramRequirementRequest["type"]>("MIN_AGE")
  const [value, setValue] = useState("")
  const [description, setDescription] = useState("")
  const refresh = () => client.invalidateQueries({ queryKey: findAll2QueryKey({ path: { editionId: edicionId } }) })
  const create = useMutation({ ...create4Mutation(), onSuccess: () => { setValue(""); setDescription(""); refresh() } })
  const remove = useMutation({ ...delete4Mutation(), onSuccess: refresh })
  return <RoutePanel><form className="mb-6 grid gap-3 rounded-lg border p-4" onSubmit={(event) => { event.preventDefault(); create.mutate({ path: { editionId: edicionId }, body: { type, value, description: description || undefined } }) }}><h3 className="font-semibold">Agregar requisito</h3><div className="grid gap-3 sm:grid-cols-2"><FormField label="Tipo"><select className={selectClass} value={type} onChange={(e) => setType(e.target.value as typeof type)}>{Object.entries(requirementLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></FormField><FormField label="Valor"><input className={inputClass} value={value} onChange={(e) => setValue(e.target.value)} required /></FormField></div><FormField label="Descripción"><input className={inputClass} value={description} onChange={(e) => setDescription(e.target.value)} /></FormField><div className="flex justify-end"><Button type="submit" disabled={create.isPending}>Agregar</Button></div></form>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />{query.data?.length === 0 ? <p className="text-sm text-muted-foreground">No hay requisitos configurados.</p> : <Table><TableHeader><TableRow><TableHead>Tipo</TableHead><TableHead>Valor</TableHead><TableHead>Descripción</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data?.map((item) => <TableRow key={item.id}><TableCell>{item.type ? requirementLabels[item.type] : "—"}</TableCell><TableCell>{item.value}</TableCell><TableCell>{item.description || "—"}</TableCell><TableCell>{item.id && <DeleteConfirmationButton description="Se eliminará este requisito." disabled={remove.isPending} size="sm" onConfirm={() => remove.mutate({ path: { editionId: edicionId, requirementId: item.id! } })} />}</TableCell></TableRow>)}</TableBody></Table>}
  </RoutePanel>
}
