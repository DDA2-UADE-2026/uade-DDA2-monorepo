import { IconPlus } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { ProgramPickerDialog } from "@/components/programs/ProgramPickerDialog"
import { LoadingOrError, RoutePanel } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create3Mutation, delete6Mutation, findAll5Options, findAll5QueryKey } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/incompatibilidades",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const client = useQueryClient()
  const query = useQuery(findAll5Options({ path: { programId: programaId } }))
  const [pickerOpen, setPickerOpen] = useState(false)
  const refresh = () => client.invalidateQueries({ queryKey: findAll5QueryKey({ path: { programId: programaId } }) })
  const create = useMutation({ ...create3Mutation(), onSuccess: refresh })
  const remove = useMutation({ ...delete6Mutation(), onSuccess: refresh })
  const selectedIds = new Set((query.data ?? []).map((item) => item.incompatibleWithProgramId).filter((id): id is string => !!id))
  const excludeIds = new Set([programaId])
  const pendingId = create.isPending ? create.variables?.path.incompatibleProgramId : remove.isPending ? remove.variables?.path.incompatibleProgramId : undefined
  return <RoutePanel>
    <div className="mb-6 flex justify-end"><Button onClick={() => setPickerOpen(true)}><IconPlus />Gestionar incompatibilidades</Button></div>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
    {query.data?.length === 0 ? <p className="text-sm text-muted-foreground">No hay incompatibilidades configuradas.</p> : <Table><TableHeader><TableRow><TableHead>Programa incompatible</TableHead><TableHead className="w-24" /></TableRow></TableHeader><TableBody>{query.data?.map((item) => <TableRow key={item.incompatibleWithProgramId}><TableCell>{item.incompatibleWithProgramName}</TableCell><TableCell><Button size="sm" variant="ghost" disabled={remove.isPending} onClick={() => item.incompatibleWithProgramId && remove.mutate({ path: { programId: programaId, incompatibleProgramId: item.incompatibleWithProgramId } })}>Quitar</Button></TableCell></TableRow>)}</TableBody></Table>}
    {pickerOpen && (
      <ProgramPickerDialog
        onOpenChange={(open) => { if (!open) setPickerOpen(false) }}
        selectedIds={selectedIds}
        excludeIds={excludeIds}
        pendingId={pendingId}
        onToggle={(program, checked) => {
          if (!program.id) return
          if (checked) create.mutate({ path: { programId: programaId, incompatibleProgramId: program.id } })
          else remove.mutate({ path: { programId: programaId, incompatibleProgramId: program.id } })
        }}
      />
    )}
  </RoutePanel>
}
