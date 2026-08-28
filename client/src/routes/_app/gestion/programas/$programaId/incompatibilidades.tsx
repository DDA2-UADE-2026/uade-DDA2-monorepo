import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { LoadingOrError, RoutePanel } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create3Mutation, delete6Mutation, findAll5Options, findAll5QueryKey, listProgramOptionsOptions } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/incompatibilidades",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const client = useQueryClient()
  const query = useQuery(findAll5Options({ path: { programId: programaId } }))
  const options = useQuery(listProgramOptionsOptions())
  const [selected, setSelected] = useState("")
  const refresh = () => client.invalidateQueries({ queryKey: findAll5QueryKey({ path: { programId: programaId } }) })
  const create = useMutation({ ...create3Mutation(), onSuccess: () => { setSelected(""); refresh() } })
  const remove = useMutation({ ...delete6Mutation(), onSuccess: refresh })
  const unavailable = new Set([programaId, ...(query.data ?? []).map((item) => item.incompatibleWithProgramId)])
  return <RoutePanel>
    <div className="mb-6 flex gap-2"><Select value={selected || null} onValueChange={(value) => setSelected(value ?? "")}><SelectTrigger className="w-full"><SelectValue placeholder="Seleccionar programa…" /></SelectTrigger><SelectContent>{(options.data ?? []).filter((item) => item.id && !unavailable.has(item.id)).map((item) => <SelectItem key={item.id} value={item.id!}>{item.name}</SelectItem>)}</SelectContent></Select><Button disabled={!selected || create.isPending} onClick={() => create.mutate({ path: { programId: programaId, incompatibleProgramId: selected } })}>Agregar</Button></div>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
    {query.data?.length === 0 ? <p className="text-sm text-muted-foreground">No hay incompatibilidades configuradas.</p> : <Table><TableHeader><TableRow><TableHead>Programa incompatible</TableHead><TableHead className="w-24" /></TableRow></TableHeader><TableBody>{query.data?.map((item) => <TableRow key={item.incompatibleWithProgramId}><TableCell>{item.incompatibleWithProgramName}</TableCell><TableCell><Button size="sm" variant="ghost" disabled={remove.isPending} onClick={() => item.incompatibleWithProgramId && remove.mutate({ path: { programId: programaId, incompatibleProgramId: item.incompatibleWithProgramId } })}>Quitar</Button></TableCell></TableRow>)}</TableBody></Table>}
  </RoutePanel>
}
