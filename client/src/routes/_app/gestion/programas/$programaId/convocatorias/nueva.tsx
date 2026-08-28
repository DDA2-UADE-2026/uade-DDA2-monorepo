import { useMutation, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { FormField, RoutePanel, inputClass, parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { create6Mutation, list1QueryKey } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/nueva",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [name, setName] = useState("")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [capacity, setCapacity] = useState("")
  const create = useMutation({ ...create6Mutation(), onSuccess: (data) => { client.invalidateQueries({ queryKey: list1QueryKey({ path: { programId: programaId } }) }); navigate({ to: "/gestion/programas/$programaId/convocatorias/$edicionId", params: { programaId, edicionId: data.id ?? "" } }) } })
  return <RoutePanel><h2 className="mb-5 text-lg font-semibold">Nueva convocatoria</h2><form className="grid gap-5" onSubmit={(event) => { event.preventDefault(); create.mutate({ path: { programId: programaId }, body: { name, startDate: startDate || undefined, endDate: endDate || undefined, maxCapacity: capacity ? Number(capacity) : undefined } }) }}>
    <FormField label="Nombre"><input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} required /></FormField>
    <div className="grid gap-4 sm:grid-cols-2"><FormField label="Fecha de inicio"><ProgramDatePicker value={startDate} onChange={setStartDate} /></FormField><FormField label="Fecha de finalización"><ProgramDatePicker value={endDate} onChange={setEndDate} disabled={startDate ? { before: parseLocalDate(startDate)! } : undefined} /></FormField></div>
    <FormField label="Capacidad máxima"><input className={inputClass} type="number" min={1} value={capacity} onChange={(e) => setCapacity(e.target.value)} /></FormField>
    {create.isError && <p className="text-sm text-destructive">No se pudo crear la convocatoria.</p>}<div className="flex justify-end"><Button type="submit" disabled={create.isPending}>{create.isPending ? "Creando…" : "Crear convocatoria"}</Button></div>
  </form></RoutePanel>
}
