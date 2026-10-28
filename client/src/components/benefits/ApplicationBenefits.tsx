import { IconPlus } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { useState } from "react"
import { toast } from "sonner"
import { ApplicationError } from "@/components/applications/ApplicationUi"
import { benefitAmount, benefitName, benefitPeriod, BenefitStatusBadge } from "@/components/benefits/BenefitUi"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { create10Mutation, findAll3Options, list6Options } from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationResponse } from "@/generated/types.gen"

export function ApplicationBenefits({ application, canAssign }: { application: AdminApplicationResponse & { id: string }; canAssign: boolean }) {
  const [open, setOpen] = useState(false)
  const query = useQuery(list6Options({ path: { id: application.id } }))
  return <section className="space-y-3"><div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="font-heading text-xl font-medium">Beneficios otorgados</h2><p className="text-sm text-muted-foreground">Consultá y gestioná los beneficios de esta solicitud.</p></div>{canAssign && application.status === "APPROVED" && <Button size="sm" onClick={() => setOpen(true)}><IconPlus />Otorgar beneficio</Button>}</div>
    {query.isPending ? <p className="text-sm text-muted-foreground">Cargando beneficios…</p> : query.isError ? <ApplicationError error={query.error} title="No pudimos cargar los beneficios" retry={() => query.refetch()} /> : !query.data.length ? <p className="rounded-xl border p-5 text-sm text-muted-foreground">Esta solicitud todavía no tiene beneficios otorgados.</p> : <div className="overflow-hidden rounded-xl border"><Table><TableHeader><TableRow><TableHead>Beneficio</TableHead><TableHead>Estado</TableHead><TableHead>Vigencia</TableHead><TableHead>Monto</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data.map((grant) => <TableRow key={grant.id}><TableCell>{benefitName(grant.benefitType)}</TableCell><TableCell><BenefitStatusBadge status={grant.status} /></TableCell><TableCell>{benefitPeriod(grant.startDate, grant.endDate)}</TableCell><TableCell>{benefitAmount(grant.amount)}</TableCell><TableCell>{grant.id && <Button size="sm" variant="outline" render={<Link to="/gestion/beneficios/$beneficioId" params={{ beneficioId: grant.id }} search={{ solicitud: application.id }} />}>Ver detalle</Button>}</TableCell></TableRow>)}</TableBody></Table></div>}
    {open && <AssignBenefitDialog application={application} onClose={() => setOpen(false)} />}
  </section>
}

function AssignBenefitDialog({ application, onClose }: { application: AdminApplicationResponse & { id: string }; onClose: () => void }) {
  const [benefitId, setBenefitId] = useState("")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const queryClient = useQueryClient()
  const editionId = application.programEditionId ?? ""
  const benefits = useQuery({ ...findAll3Options({ path: { editionId } }), enabled: !!editionId })
  const mutation = useMutation(create10Mutation())
  const submit = async () => {
    try {
      await mutation.mutateAsync({ path: { id: application.id }, body: { programBenefitId: benefitId, startDate, endDate } })
      await queryClient.invalidateQueries()
      toast.success("Beneficio otorgado")
      onClose()
    } catch { /* El error de la API se muestra en el diálogo. */ }
  }
  return <Dialog open onOpenChange={(open) => { if (!open && !mutation.isPending) onClose() }}><DialogContent><DialogHeader><DialogTitle>Otorgar beneficio</DialogTitle><DialogDescription>Seleccioná un beneficio de la convocatoria. La vigencia debe incluir hoy y estar dentro de las fechas de la edición.</DialogDescription></DialogHeader>
    <label className="grid gap-2 text-sm">Beneficio<Select value={benefitId || null} onValueChange={(value) => setBenefitId(value ?? "")}><SelectTrigger className="w-full"><SelectValue placeholder={benefits.isPending ? "Cargando…" : "Seleccionar beneficio"} /></SelectTrigger><SelectContent>{benefits.data?.map((benefit) => benefit.id && <SelectItem key={benefit.id} value={benefit.id}>{benefitName(benefit.benefitType)} · {benefitAmount(benefit.amount)}</SelectItem>)}</SelectContent></Select></label>
    {benefits.isError && <p role="alert" className="text-destructive">No pudimos cargar los beneficios de la convocatoria.</p>}
    {benefits.isSuccess && benefits.data.length === 0 && <p className="text-muted-foreground">La convocatoria no tiene beneficios configurados.</p>}
    <label className="grid gap-2 text-sm">Inicio de vigencia<ProgramDatePicker value={startDate} onChange={setStartDate} /></label><label className="grid gap-2 text-sm">Fin de vigencia<ProgramDatePicker value={endDate} onChange={setEndDate} /></label>
    {mutation.error && <p role="alert" className="text-sm text-destructive">{mutation.error.message ?? "No se pudo otorgar el beneficio."}</p>}
    <DialogFooter><Button variant="outline" disabled={mutation.isPending} onClick={onClose}>Cancelar</Button><Button disabled={!benefitId || !startDate || !endDate || mutation.isPending} onClick={submit}>{mutation.isPending ? "Guardando…" : "Otorgar"}</Button></DialogFooter>
  </DialogContent></Dialog>
}
