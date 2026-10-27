import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import type { ReactNode } from "react"

import { ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { benefitLabels, formatRequirementValue, parseLocalDate, requirementLabels, statusLabels } from "@/components/programs/ProgramRouteUi"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  findAll2Options,
  findAll3Options,
  findById2Options,
  findById3Options,
} from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationResponse } from "@/generated/types.gen"
import { formatApplicationDateTime } from "@/lib/application-flow"
import { UserChip } from "@/components/UserChip"

const currencyFormatter = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

function formatEditionDate(value?: string) {
  const date = value ? parseLocalDate(value) : undefined
  return date ? date.toLocaleDateString("es-AR", { day: "2-digit", month: "short", year: "numeric" }) : "—"
}

// Read-only summary of who applied and to what, so the evaluation is written with the program in view.
export function ApplicationProgramContext({ application }: { application: AdminApplicationResponse }) {
  const programId = application.programId ?? ""
  const editionId = application.programEditionId ?? ""
  const program = useQuery({ ...findById2Options({ path: { id: programId } }), enabled: !!programId })
  const edition = useQuery({ ...findById3Options({ path: { id: editionId } }), enabled: !!editionId })
  const requirements = useQuery({ ...findAll2Options({ path: { editionId } }), enabled: !!editionId })
  const benefits = useQuery({ ...findAll3Options({ path: { editionId } }), enabled: !!editionId })

  return (
    <Card>
      <CardHeader>
        <CardTitle>Contexto de la solicitud</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6 text-sm">
        <div className="grid gap-6 sm:grid-cols-3">
          <Section title="Solicitud">
            <div><ApplicationStatusBadge status={application.status} /></div>
            <Row label="Titular">
              <UserChip user={{ id: application.userId, name: application.userName, email: application.userEmail }} secondary={application.userEmail} />
            </Row>
            {application.registeredByUserId != null && application.registeredByUserId !== application.userId && (
              <Row label="Registrada por">
                <UserChip user={{ id: application.registeredByUserId, name: application.registeredByUserName }} />
              </Row>
            )}
            <Row label="Presentada">{formatApplicationDateTime(application.submittedAt)}</Row>
            <Row label="Trabajador asignado"><UserChip user={{ id: application.assignedWorkerUserId, name: application.assignedWorkerName }} fallback="Sin asignar" /></Row>
          </Section>

          <Section title="Programa">
            {programId ? (
              <Link to="/gestion/programas/$programaId" params={{ programaId: programId }} className="font-medium hover:underline">
                {application.programName ?? program.data?.name ?? "Programa"}
              </Link>
            ) : <p className="font-medium">{application.programName ?? "—"}</p>}
            {program.isPending && programId ? (
              <Skeleton className="h-12 w-full" />
            ) : program.data?.objective ? (
              <p className="line-clamp-4 whitespace-pre-wrap text-muted-foreground" title={program.data.objective}>{program.data.objective}</p>
            ) : (
              <p className="text-muted-foreground">Sin objetivo cargado.</p>
            )}
          </Section>

          <Section title="Edición">
            <div className="flex flex-wrap items-center gap-2">
              {programId && editionId ? (
                <Link
                  to="/gestion/programas/$programaId/convocatorias/$edicionId"
                  params={{ programaId: programId, edicionId: editionId }}
                  className="font-medium hover:underline"
                >
                  {application.programEditionName ?? edition.data?.name ?? "Edición"}
                </Link>
              ) : <p className="font-medium">{application.programEditionName ?? "—"}</p>}
              {edition.data?.status && (
                <Badge variant={edition.data.status === "ACTIVE" ? "secondary" : "outline"}>{statusLabels[edition.data.status]}</Badge>
              )}
            </div>
            {edition.isPending && editionId ? (
              <Skeleton className="h-10 w-full" />
            ) : edition.data && (
              <>
                <Row label="Vigencia">{formatEditionDate(edition.data.startDate)} – {formatEditionDate(edition.data.endDate)}</Row>
                <Row label="Cupos">{edition.data.currentEnrollment ?? 0} / {edition.data.maxCapacity ?? "sin límite"}</Row>
              </>
            )}
          </Section>
        </div>

        {editionId && (
          <div className="grid gap-6 border-t pt-5 sm:grid-cols-2">
            <Section title="Requisitos de la edición">
              {requirements.isPending ? <Skeleton className="h-10 w-full" /> : requirements.isError ? (
                <p className="text-muted-foreground">No se pudieron cargar los requisitos.</p>
              ) : !requirements.data?.length ? (
                <p className="text-muted-foreground">Sin requisitos.</p>
              ) : (
                <ul className="space-y-1.5">
                  {requirements.data.map((requirement) => (
                    <li key={requirement.id}>
                      <span className="font-medium">{requirement.type ? requirementLabels[requirement.type] : "Requisito"}</span>
                      {requirement.value != null && requirement.value !== "" && <>: {formatRequirementValue(requirement.type, String(requirement.value))}</>}
                      {requirement.description && <span className="block text-xs text-muted-foreground">{requirement.description}</span>}
                    </li>
                  ))}
                </ul>
              )}
            </Section>
            <Section title="Beneficios">
              {benefits.isPending ? <Skeleton className="h-10 w-full" /> : benefits.isError ? (
                <p className="text-muted-foreground">No se pudieron cargar los beneficios.</p>
              ) : !benefits.data?.length ? (
                <p className="text-muted-foreground">Sin beneficios.</p>
              ) : (
                <ul className="space-y-1.5">
                  {benefits.data.map((benefit) => (
                    <li key={benefit.id}>
                      <span className="font-medium">{benefit.benefitType ? benefitLabels[benefit.benefitType] : "Beneficio"}</span>
                      {benefit.amount != null && <>: {currencyFormatter.format(benefit.amount)}</>}
                      {benefit.description && <span className="block text-xs text-muted-foreground">{benefit.description}</span>}
                    </li>
                  ))}
                </ul>
              )}
            </Section>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="min-w-0 space-y-2">
      <h2 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{title}</h2>
      {children}
    </section>
  )
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <div>{children}</div>
    </div>
  )
}
