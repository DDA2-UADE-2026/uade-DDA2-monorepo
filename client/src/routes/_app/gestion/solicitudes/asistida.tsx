import {
  IconAlertCircle,
  IconCalendarEvent,
  IconCheck,
  IconFileCheck,
  IconFileDescription,
  IconHeartHandshake,
  IconInfoCircle,
  IconUser,
} from "@tabler/icons-react"
import { useMutation, useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { useRef, useState } from "react"

import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Separator } from "@/components/ui/separator"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  findAllOptions,
  getAvailableProgramOptions,
  listAvailableProgramsOptions,
  submit1Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type {
  ApplicationResponse,
  AvailableEnrollmentPeriodResponse,
  AvailableProgramEditionResponse,
  UserManagementResponse,
} from "@/generated/types.gen"
import {
  applicationStatusLabels,
  canApplyToPeriod,
  clearSubmissionKey,
  enrollmentStatusLabels,
  formatApplicationDate,
  getSubmissionKey,
} from "@/lib/application-flow"

const PROGRAM_PAGE_SIZE = 100

export const Route = createFileRoute("/_app/gestion/solicitudes/asistida")({
  component: RouteComponent,
})

function RouteComponent() {
  const { user } = Route.useRouteContext()

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Solicitudes" }]} />
        <OutletNavRightButton>
          <Button
            size="sm"
            variant="outline"
            render={<Link to="/gestion/documentos" search={{ solicitudId: "" }} />}
          >
            <IconFileCheck />
            Revisar documentos
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full max-w-5xl space-y-6 p-4 lg:p-6">
            <header>
              <h1 className="font-heading text-2xl font-medium">Solicitudes</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Registrá una presentación asistida para una persona que ya existe en el sistema.
              </p>
            </header>

            <Alert>
              <IconInfoCircle />
              <AlertTitle>Alcance de la gestión actual</AlertTitle>
              <AlertDescription>
                Podés registrar solicitudes y revisar sus documentos. La bandeja general, la evaluación y la resolución todavía no están disponibles.
              </AlertDescription>
            </Alert>

            <AssistedApplicationForm currentUserId={user.id} />
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

type PeriodOption = {
  edition: AvailableProgramEditionResponse
  period: AvailableEnrollmentPeriodResponse & { id: string }
}

export function AssistedApplicationForm({ currentUserId }: { currentUserId?: number }) {
  const [selectedUserId, setSelectedUserId] = useState<number>()
  const [selectedProgramId, setSelectedProgramId] = useState("")
  const [selectedPeriodId, setSelectedPeriodId] = useState("")
  const [confirmation, setConfirmation] = useState<ApplicationResponse>()
  const attemptKey = useRef<string | null>(null)
  const submitting = useRef(false)

  const users = useQuery(findAllOptions())
  const programs = useQuery(listAvailableProgramsOptions({
    query: { page: 0, size: PROGRAM_PAGE_SIZE },
  }))
  const program = useQuery({
    ...getAvailableProgramOptions({ path: { id: selectedProgramId } }),
    enabled: Boolean(selectedProgramId),
  })
  const submit = useMutation({ ...submit1Mutation(), retry: false })

  const selectableUsers = (users.data ?? []).filter(
    (candidate): candidate is UserManagementResponse & { id: number } =>
      candidate.id != null && candidate.id !== currentUserId,
  )
  const selectedUser = selectableUsers.find((candidate) => candidate.id === selectedUserId)
  const selectedProgram = programs.data?.content?.find((candidate) => candidate.id === selectedProgramId)
  const periodOptions: PeriodOption[] = (program.data?.editions ?? []).flatMap((edition) =>
    (edition.enrollmentPeriods ?? []).flatMap((period) =>
      period.id && canApplyToPeriod(edition, period) ? [{ edition, period: { ...period, id: period.id } }] : [],
    ),
  )
  const selectedPeriod = periodOptions.find((option) => option.period.id === selectedPeriodId)
  const formReady = selectedUserId != null && Boolean(selectedPeriod)

  const resetMutation = () => {
    attemptKey.current = null
    submit.reset()
  }

  const changeUser = (value: string | null) => {
    setSelectedUserId(value ? Number(value) : undefined)
    resetMutation()
  }

  const changeProgram = (value: string | null) => {
    setSelectedProgramId(value ?? "")
    setSelectedPeriodId("")
    resetMutation()
  }

  const changePeriod = (value: string | null) => {
    setSelectedPeriodId(value ?? "")
    resetMutation()
  }

  const present = async () => {
    if (!selectedUserId || !selectedPeriod || submitting.current || submit.isSuccess) return

    submitting.current = true
    attemptKey.current ??= getSubmissionKey(selectedUserId, selectedPeriod.period.id)
    try {
      const application = await submit.mutateAsync({
        body: {
          userId: selectedUserId,
          enrollmentPeriodId: selectedPeriod.period.id,
        },
        headers: { "Idempotency-Key": attemptKey.current },
      })
      clearSubmissionKey(selectedUserId, selectedPeriod.period.id)
      attemptKey.current = null
      setConfirmation(application)
    } catch {
      // Keep the idempotency key so retrying this same presentation is safe.
    } finally {
      submitting.current = false
    }
  }

  const startAnother = () => {
    setSelectedUserId(undefined)
    setSelectedProgramId("")
    setSelectedPeriodId("")
    setConfirmation(undefined)
    resetMutation()
  }

  if (confirmation) {
    return (
      <ApplicationConfirmation
        application={confirmation}
        applicant={selectedUser}
        onReset={startAnother}
      />
    )
  }

  return (
    <Card>
      <CardHeader>
        <Badge variant="secondary" className="w-fit">
          <IconFileDescription />
          Presentación asistida
        </Badge>
        <CardTitle>Registrar una solicitud</CardTitle>
        <CardDescription>
          Seleccioná al titular y una convocatoria abierta. Vos quedarás registrado como quien realizó la presentación.
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-6">
        <FieldGroup className="grid gap-5 md:grid-cols-2">
          <Field>
            <FieldLabel htmlFor="assisted-applicant">Persona solicitante</FieldLabel>
            <Select
              value={selectedUserId == null ? null : String(selectedUserId)}
              onValueChange={changeUser}
              disabled={users.isPending || users.isError || submit.isPending}
            >
              <SelectTrigger id="assisted-applicant" className="w-full">
                <SelectValue placeholder={users.isPending ? "Cargando personas…" : "Seleccioná una persona"} />
              </SelectTrigger>
              <SelectContent>
                {selectableUsers.map((candidate) => (
                  <SelectItem key={candidate.id} value={String(candidate.id)}>
                    <span className="flex min-w-0 flex-col">
                      <span className="truncate">{candidate.name || candidate.username || `Usuario #${candidate.id}`}</span>
                      <span className="truncate text-xs text-muted-foreground">
                        {candidate.email || `ID interno ${candidate.id}`}{candidate.active === false ? " · Inactivo" : ""}
                      </span>
                    </span>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <FieldDescription>
              La solicitud quedará a nombre de esta persona. No podés usar la presentación asistida para vos mismo.
            </FieldDescription>
            {users.isError && <QueryError label="No se pudieron cargar las personas." retry={() => users.refetch()} />}
            {!users.isPending && !users.isError && selectableUsers.length === 0 && (
              <p className="text-sm text-muted-foreground">No hay otras personas disponibles para seleccionar.</p>
            )}
          </Field>

          <Field>
            <FieldLabel htmlFor="assisted-program">Programa</FieldLabel>
            <Select
              value={selectedProgramId || null}
              onValueChange={changeProgram}
              disabled={programs.isPending || programs.isError || submit.isPending}
            >
              <SelectTrigger id="assisted-program" className="w-full">
                <SelectValue placeholder={programs.isPending ? "Cargando programas…" : "Seleccioná un programa"} />
              </SelectTrigger>
              <SelectContent>
                {(programs.data?.content ?? []).flatMap((candidate) => candidate.id ? (
                  <SelectItem key={candidate.id} value={candidate.id}>
                    {candidate.name || "Programa sin nombre"}
                  </SelectItem>
                ) : [])}
              </SelectContent>
            </Select>
            <FieldDescription>
              Se muestran programas con ediciones activas vigentes o futuras.
            </FieldDescription>
            {programs.isError && <QueryError label="No se pudieron cargar los programas." retry={() => programs.refetch()} />}
            {!programs.isPending && !programs.isError && (programs.data?.content?.length ?? 0) === 0 && (
              <p className="text-sm text-muted-foreground">No hay programas disponibles.</p>
            )}
            {(programs.data?.totalElements ?? 0) > PROGRAM_PAGE_SIZE && (
              <p className="text-sm text-amber-700 dark:text-amber-300">
                Se muestran los primeros {PROGRAM_PAGE_SIZE} programas disponibles.
              </p>
            )}
          </Field>
        </FieldGroup>

        <Separator />

        <Field>
          <FieldLabel htmlFor="assisted-period">Convocatoria</FieldLabel>
          <Select
            value={selectedPeriodId || null}
            onValueChange={changePeriod}
            disabled={!selectedProgramId || program.isPending || program.isError || periodOptions.length === 0 || submit.isPending}
          >
            <SelectTrigger id="assisted-period" className="w-full">
              <SelectValue placeholder={
                !selectedProgramId ? "Primero seleccioná un programa"
                  : program.isPending ? "Cargando convocatorias…"
                    : "Seleccioná una convocatoria abierta"
              } />
            </SelectTrigger>
            <SelectContent>
              {periodOptions.map(({ edition, period }) => (
                <SelectItem key={period.id} value={period.id}>
                  <span className="flex min-w-0 flex-col">
                    <span className="truncate">{edition.name || "Edición"}</span>
                    <span className="text-xs text-muted-foreground">
                      {formatPeriod(period)}
                    </span>
                  </span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <FieldDescription>
            Solo se habilitan convocatorias abiertas y vigentes. El backend vuelve a validar su estado al confirmar.
          </FieldDescription>
          {program.isError && <QueryError label="No se pudieron cargar las convocatorias." retry={() => program.refetch()} />}
          {selectedProgramId && !program.isPending && !program.isError && periodOptions.length === 0 && (
            <Alert>
              <IconInfoCircle />
              <AlertTitle>Sin convocatorias habilitadas</AlertTitle>
              <AlertDescription>Este programa no tiene una inscripción abierta y vigente en este momento.</AlertDescription>
            </Alert>
          )}
        </Field>

        {selectedUser && selectedProgram && selectedPeriod && (
          <ApplicationSummary
            applicant={selectedUser}
            programName={program.data?.name ?? selectedProgram.name}
            option={selectedPeriod}
          />
        )}

        {submit.isError && (
          <Alert variant="destructive">
            <IconAlertCircle />
            <AlertTitle>No se pudo registrar la solicitud</AlertTitle>
            <AlertDescription>
              {submit.error.message ?? "Revisá que la persona y la convocatoria sigan habilitadas e intentá nuevamente."}
            </AlertDescription>
          </Alert>
        )}
      </CardContent>

      <CardFooter className="justify-end">
        <Button type="button" disabled={!formReady || submit.isPending || submit.isSuccess} onClick={present}>
          {submit.isPending ? "Registrando…" : "Registrar solicitud"}
        </Button>
      </CardFooter>
    </Card>
  )
}

function ApplicationSummary({
  applicant,
  programName,
  option,
}: {
  applicant: UserManagementResponse & { id: number }
  programName?: string
  option: PeriodOption
}) {
  const documents = option.edition.documentRequirements ?? []
  const requiredDocuments = documents.filter((document) => document.required).length

  return (
    <div className="rounded-xl border bg-muted/30 p-4">
      <h2 className="font-medium">Resumen de la presentación</h2>
      <div className="mt-4 grid gap-4 text-sm sm:grid-cols-2">
        <SummaryItem
          icon={<IconUser />}
          label="Titular"
          value={applicant.name || applicant.username || `Usuario #${applicant.id}`}
          description={applicant.email}
        />
        <SummaryItem
          icon={<IconHeartHandshake />}
          label="Programa y edición"
          value={programName || "Programa"}
          description={option.edition.name}
        />
        <SummaryItem
          icon={<IconCalendarEvent />}
          label="Inscripción"
          value={formatPeriod(option.period)}
          description={option.period.status ? enrollmentStatusLabels[option.period.status] : undefined}
        />
        <SummaryItem
          icon={<IconFileDescription />}
          label="Documentación posterior"
          value={documents.length === 0 ? "Sin documentos solicitados" : `${documents.length} documento${documents.length === 1 ? "" : "s"}`}
          description={documents.length === 0 ? undefined : `${requiredDocuments} obligatorio${requiredDocuments === 1 ? "" : "s"}`}
        />
      </div>
    </div>
  )
}

function SummaryItem({
  icon,
  label,
  value,
  description,
}: {
  icon: React.ReactNode
  label: string
  value: string
  description?: string
}) {
  return (
    <div className="flex min-w-0 gap-3">
      <span className="mt-0.5 text-primary [&_svg]:size-4">{icon}</span>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="truncate font-medium">{value}</p>
        {description && <p className="truncate text-xs text-muted-foreground">{description}</p>}
      </div>
    </div>
  )
}

function ApplicationConfirmation({
  application,
  applicant,
  onReset,
}: {
  application: ApplicationResponse
  applicant?: UserManagementResponse
  onReset: () => void
}) {
  return (
    <Card>
      <CardHeader>
        <span className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
          <IconCheck className="size-6" />
        </span>
        <CardTitle>Solicitud registrada</CardTitle>
        <CardDescription>
          La presentación quedó a nombre de {applicant?.name || applicant?.username || "la persona seleccionada"}.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="grid gap-4 rounded-xl border bg-muted/30 p-4 text-sm sm:grid-cols-2">
          <div>
            <p className="text-xs text-muted-foreground">Número de solicitud</p>
            <p className="mt-1 font-heading text-xl font-semibold">N.º {application.applicationNumber ?? "—"}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Estado</p>
            <Badge variant="secondary" className="mt-1">
              {application.status ? applicationStatusLabels[application.status] : "Presentada"}
            </Badge>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Programa</p>
            <p className="mt-1 font-medium">{application.programName || "—"}</p>
            {application.programEditionName && <p className="text-xs text-muted-foreground">{application.programEditionName}</p>}
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Fecha de presentación</p>
            <p className="mt-1 font-medium">{formatApplicationDate(application.submittedAt)}</p>
          </div>
        </div>

        {application.id ? (
          <div>
            <p className="text-xs text-muted-foreground">ID interno para revisión documental</p>
            <code className="mt-1 block break-all rounded-lg bg-muted px-3 py-2 text-xs">{application.id}</code>
          </div>
        ) : (
          <Alert variant="destructive">
            <IconAlertCircle />
            <AlertTitle>La respuesta no incluyó el identificador</AlertTitle>
            <AlertDescription>No vuelvas a registrar la misma presentación sin verificarla.</AlertDescription>
          </Alert>
        )}
      </CardContent>
      <CardFooter className="flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <Button type="button" variant="outline" onClick={onReset}>Registrar otra solicitud</Button>
        {application.id && (
          <Button render={<Link to="/gestion/documentos" search={{ solicitudId: application.id }} />}>
            <IconFileCheck />
            Revisar documentación
          </Button>
        )}
      </CardFooter>
    </Card>
  )
}

function QueryError({ label, retry }: { label: string; retry: () => void }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-destructive/30 px-3 py-2 text-sm text-destructive">
      <span>{label}</span>
      <Button type="button" size="xs" variant="ghost" onClick={retry}>Reintentar</Button>
    </div>
  )
}

function formatPeriod(period: AvailableEnrollmentPeriodResponse) {
  return `${formatApplicationDate(period.openDate)} al ${formatApplicationDate(period.closeDate)}`
}
