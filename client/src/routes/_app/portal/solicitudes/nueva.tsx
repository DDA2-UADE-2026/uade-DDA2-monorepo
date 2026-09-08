import { IconArrowLeft, IconCheck, IconChecklist, IconFileDescription } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useRef, useState } from "react"
import { z } from "zod"

import { ApplicationError, ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { requirementLabels } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Item, ItemContent, ItemDescription, ItemGroup, ItemMedia, ItemTitle } from "@/components/ui/item"
import { get1QueryKey, getAvailableProgramOptions, listQueryKey, submitMutation } from "@/generated/@tanstack/react-query.gen"
import type { AvailableEnrollmentPeriodResponse, AvailableProgramDetailResponse, AvailableProgramEditionResponse } from "@/generated/types.gen"
import { canApplyToPeriod, clearSubmissionKey, formatApplicationDate, getSubmissionKey } from "@/lib/application-flow"

const searchSchema = z.object({
  programaId: z.uuid().optional().catch(undefined),
  periodoId: z.uuid().optional().catch(undefined),
})

export const Route = createFileRoute("/_app/portal/solicitudes/nueva")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId, periodoId } = Route.useSearch()
  const { user } = Route.useRouteContext()
  const program = useQuery({ ...getAvailableProgramOptions({ path: { id: programaId ?? "" } }), enabled: Boolean(programaId) })
  const edition = program.data?.editions?.find((item) => item.enrollmentPeriods?.some((period) => period.id === periodoId))
  const period = edition?.enrollmentPeriods?.find((item) => item.id === periodoId)

  return (
    <ApplicationPage breadcrumbs={[
      { label: "Programas", to: "/portal/programas" },
      ...(programaId ? [{ label: program.data?.name ?? "Programa", to: `/portal/programas/${programaId}` }] : []),
      { label: "Presentar solicitud" },
    ]}>
      <ApplicationHeading title="Presentar solicitud" description="Revisá la información antes de confirmar tu presentación." />
      {!programaId || !periodoId ? (
        <Card><CardHeader><CardTitle>Elegí un programa y un período de inscripción</CardTitle><CardDescription>Las solicitudes se inician desde la página de cada programa.</CardDescription></CardHeader><CardContent><Button render={<Link to="/portal/programas" search={{ page: 1 }} />}>Ver programas</Button></CardContent></Card>
      ) : program.isPending ? <ApplicationLoading /> : program.isError ? (
        <ApplicationError error={program.error} title="No pudimos consultar el programa" retry={() => program.refetch()} />
      ) : !edition || !period || !canApplyToPeriod(edition, period) ? (
        <Card><CardHeader><CardTitle>Este período no está disponible para nuevas solicitudes</CardTitle><CardDescription>Volvé al programa para consultar las inscripciones habilitadas.</CardDescription></CardHeader><CardContent><Button variant="outline" render={<Link to="/portal/programas/$programaId" params={{ programaId }} />}>Volver al programa</Button></CardContent></Card>
      ) : program.data && user.id != null ? (
        <ApplicationConfirmation key={`${user.id}:${periodoId}`} program={program.data} edition={edition} period={period} userId={user.id} />
      ) : null}
    </ApplicationPage>
  )
}

export function ApplicationConfirmation({ program, edition, period, userId }: {
  program: AvailableProgramDetailResponse
  edition: AvailableProgramEditionResponse
  period: AvailableEnrollmentPeriodResponse
  userId: number
}) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const attemptKey = useRef<string | null>(null)
  const submitting = useRef(false)
  const [unavailable, setUnavailable] = useState(false)
  const submit = useMutation({ ...submitMutation(), retry: false, onError: showApiErrorToast })

  const present = async () => {
    if (submitting.current || submit.isSuccess || !period.id) return
    if (!canApplyToPeriod(edition, period)) { setUnavailable(true); return }
    submitting.current = true
    attemptKey.current ??= getSubmissionKey(userId, period.id)
    try {
      const application = await submit.mutateAsync({ body: { enrollmentPeriodId: period.id }, headers: { "Idempotency-Key": attemptKey.current } })
      if (application.id) {
        queryClient.setQueryData(get1QueryKey({ path: { id: application.id } }), application)
        clearSubmissionKey(userId, period.id)
        queryClient.invalidateQueries({ queryKey: listQueryKey() })
        await navigate({ to: "/portal/solicitudes/$solicitudId", params: { solicitudId: application.id }, replace: true })
      }
    } catch {
      // Keep the same submission key and show the API error for a safe retry.
    } finally {
      submitting.current = false
    }
  }

  return <>
    <Card>
      <CardHeader>
        <Badge variant="secondary" className="w-fit">{edition.name ?? "Edición"}</Badge>
        <CardTitle className="font-heading text-xl">{program.name}</CardTitle>
        <CardDescription>Inscripción del {formatApplicationDate(period.openDate)} al {formatApplicationDate(period.closeDate)}.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {program.objective && <p className="text-sm leading-6">{program.objective}</p>}
        <div>
          <h2 className="mb-3 font-medium">Requisitos de la edición</h2>
          {edition.requirements?.length ? <ItemGroup>{edition.requirements.map((requirement) => <Item key={requirement.id} variant="muted">
            <ItemMedia variant="icon"><IconChecklist /></ItemMedia>
            <ItemContent><ItemTitle>{requirement.type ? requirementLabels[requirement.type] : "Requisito"}{requirement.value ? `: ${requirement.value}` : ""}</ItemTitle>{requirement.description && <ItemDescription className="line-clamp-none">{requirement.description}</ItemDescription>}</ItemContent>
          </Item>)}</ItemGroup> : <p className="text-sm text-muted-foreground">No hay condiciones adicionales informadas.</p>}
        </div>
        <div>
          <h2 className="mb-1 font-medium">Documentación solicitada</h2>
          <p className="mb-3 text-sm text-muted-foreground">Podrás adjuntar estos documentos después de presentar la solicitud.</p>
          {edition.documentRequirements?.length ? <ItemGroup>{edition.documentRequirements.map((requirement) => <Item key={requirement.id} variant="outline">
            <ItemMedia variant="icon"><IconFileDescription /></ItemMedia>
            <ItemContent><ItemTitle>{requirement.name}</ItemTitle>{requirement.description && <ItemDescription className="line-clamp-none">{requirement.description}</ItemDescription>}</ItemContent>
            <Badge variant={requirement.required ? "default" : "secondary"}>{requirement.required ? "Obligatorio" : "Opcional"}</Badge>
          </Item>)}</ItemGroup> : <p className="text-sm text-muted-foreground">Esta edición no solicita documentación.</p>}
        </div>
      </CardContent>
    </Card>
    <Alert><IconCheck /><AlertTitle>Vas a presentar una solicitud a tu nombre</AlertTitle><AlertDescription>Recibirás un número para consultar su estado desde Mis solicitudes. La presentación no implica la aprobación del beneficio.</AlertDescription></Alert>
    {unavailable && <Alert variant="destructive"><AlertTitle>El período de inscripción ya no está vigente</AlertTitle><AlertDescription>Volvé al programa para consultar los períodos disponibles.</AlertDescription></Alert>}
    {submit.error?.status === 409 && <Button variant="outline" render={<Link to="/portal/solicitudes" search={{ page: 1 }} />}>Consultar mis solicitudes</Button>}
    {submit.isSuccess && !submit.data.id && <Alert variant="destructive"><AlertTitle>La respuesta no incluyó el número de seguimiento</AlertTitle><AlertDescription>Consultá Mis solicitudes antes de volver a presentar.</AlertDescription></Alert>}
    {submit.isSuccess && submit.data.id && <Button render={<Link to="/portal/solicitudes/$solicitudId" params={{ solicitudId: submit.data.id }} />}>Continuar a mi solicitud</Button>}
    <div className="flex flex-col-reverse justify-between gap-3 sm:flex-row">
      <Button variant="outline" disabled={submit.isPending} render={<Link to="/portal/programas/$programaId" params={{ programaId: program.id ?? "" }} />}><IconArrowLeft />Volver al programa</Button>
      <Button onClick={present} disabled={submit.isPending || submit.isSuccess || unavailable}>{submit.isPending ? "Presentando…" : "Presentar solicitud"}</Button>
    </div>
  </>
}
