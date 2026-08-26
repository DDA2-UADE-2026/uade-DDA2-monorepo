import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { benefitLabels, FormField, LoadingOrError, RoutePanel, inputClass, selectClass } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create5Mutation, delete5Mutation, findAll3Options, findAll3QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateProgramBenefitRequest } from "@/generated/types.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/beneficios",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { edicionId } = Route.useParams()
  const client = useQueryClient()
  const query = useQuery(findAll3Options({ path: { editionId: edicionId } }))
  const [type, setType] = useState<CreateProgramBenefitRequest["benefitType"]>("FOOD_ASSISTANCE")
  const [description, setDescription] = useState("")
  const [amount, setAmount] = useState("")
  const refresh = () => client.invalidateQueries({ queryKey: findAll3QueryKey({ path: { editionId: edicionId } }) })
  const create = useMutation({ ...create5Mutation(), onSuccess: () => { setDescription(""); setAmount(""); refresh() } })
  const remove = useMutation({ ...delete5Mutation(), onSuccess: refresh })
  return <RoutePanel><form className="mb-6 grid gap-3 rounded-lg border p-4" onSubmit={(event) => { event.preventDefault(); create.mutate({ path: { editionId: edicionId }, body: { benefitType: type, description: description || undefined, amount: amount ? Number(amount) : undefined } }) }}><h3 className="font-semibold">Agregar beneficio</h3><div className="grid gap-3 sm:grid-cols-2"><FormField label="Tipo"><select className={selectClass} value={type} onChange={(e) => setType(e.target.value as typeof type)}>{Object.entries(benefitLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></FormField><FormField label="Monto"><input className={inputClass} type="number" min={0} step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} /></FormField></div><FormField label="Descripción"><input className={inputClass} value={description} onChange={(e) => setDescription(e.target.value)} /></FormField><div className="flex justify-end"><Button type="submit" disabled={create.isPending}>Agregar</Button></div></form>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />{query.data?.length === 0 ? <p className="text-sm text-muted-foreground">No hay beneficios configurados.</p> : <Table><TableHeader><TableRow><TableHead>Tipo</TableHead><TableHead>Descripción</TableHead><TableHead>Monto</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data?.map((item) => <TableRow key={item.id}><TableCell>{item.benefitType ? benefitLabels[item.benefitType] : "—"}</TableCell><TableCell>{item.description || "—"}</TableCell><TableCell>{item.amount == null ? "—" : new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(item.amount)}</TableCell><TableCell><Button size="sm" variant="ghost" onClick={() => item.id && remove.mutate({ path: { editionId: edicionId, benefitId: item.id } })}>Eliminar</Button></TableCell></TableRow>)}</TableBody></Table>}
  </RoutePanel>
}
