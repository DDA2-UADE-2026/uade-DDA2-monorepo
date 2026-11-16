/* eslint-disable react-refresh/only-export-components */
import { Badge } from "@/components/ui/badge"
import { benefitLabels } from "@/components/programs/ProgramRouteUi"
import { formatInterventionDate } from "@/components/interventions/InterventionUi"
import type { CitizenBenefitGrantResponse } from "@/generated/types.gen"

export type BenefitStatus = NonNullable<CitizenBenefitGrantResponse["status"]>
export const benefitStatusLabels: Record<BenefitStatus, string> = { ACTIVE: "Activo", SUSPENDED: "Suspendido", FINALIZED: "Finalizado", EXPIRED: "Vencido" }
export function BenefitStatusBadge({ status }: { status?: BenefitStatus }) {
  return <Badge variant={status === "ACTIVE" ? "default" : status === "SUSPENDED" ? "secondary" : "outline"}>{status ? benefitStatusLabels[status] : "Sin estado"}</Badge>
}
export function benefitName(type?: CitizenBenefitGrantResponse["benefitType"]) { return type ? benefitLabels[type] : "Beneficio" }
export function benefitAmount(amount?: number | null) { return amount == null ? "Sin monto" : new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(amount) }
export function benefitPeriod(start?: string, end?: string) { return `${formatInterventionDate(start)} – ${formatInterventionDate(end)}` }
