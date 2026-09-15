import {
  IconAlertTriangle,
  IconFileCheck,
  IconFileDescription,
  IconRefresh,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import type { ReactNode } from "react"

import { AdminDocumentsTable } from "@/components/applications/AdminApplicationDocuments"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { UserAvatar } from "@/components/UserAvatar"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { get2Options, list6Options } from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationResponse, ErrorResponse } from "@/generated/types.gen"
import {
  applicationStatusLabels,
  formatApplicationDateTime,
  isApplicationResolved,
} from "@/lib/application-flow"

export const Route = createFileRoute("/_app/gestion/solicitudes/$solicitudId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useParams()
  const query = useQuery(get2Options({ path: { id: solicitudId } }))
  const application = query.data
  const heading = application?.applicationNumber != null
    ? `Solicitud N.º ${application.applicationNumber}`
    : "Solicitud"

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[
          { label: "Solicitudes", to: "/gestion/solicitudes" },
          { label: query.isPending ? "Solicitud" : heading },
        ]} />
        <OutletNavRightButton>
          <Button
            size="sm"
            variant="outline"
            render={<Link to="/gestion/solicitudes" search={{ page: 1 }} />}
          >
            <IconFileCheck />
            Ver todas
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-5xl space-y-6 p-4 lg:p-6">
            {query.isPending ? (
              <div role="status" aria-label="Cargando solicitud" className="space-y-5">
                <Skeleton className="h-8 w-64" />
                <Skeleton className="h-44 w-full" />
                <Skeleton className="h-60 w-full" />
                <span className="sr-only">Cargando solicitud…</span>
              </div>
            ) : query.isError ? (
              <DetailError error={query.error} retry={() => query.refetch()} />
            ) : !application ? (
              <Empty className="min-h-72 border">
                <EmptyHeader>
                  <EmptyMedia variant="icon"><IconFileDescription /></EmptyMedia>
                  <EmptyTitle>Sin datos de la solicitud</EmptyTitle>
                  <EmptyDescription>La consulta no devolvió información para este identificador.</EmptyDescription>
                </EmptyHeader>
              </Empty>
            ) : (
              <>
                <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                  <div className="min-w-0">
                    <h1 className="font-heading text-2xl font-medium">{heading}</h1>
                    <p className="mt-1 truncate font-mono text-xs text-muted-foreground" title={solicitudId}>
                      {solicitudId}
                    </p>
                  </div>
                  <ApplicationStatusBadge status={application.status} />
                </header>

                {(application.pendingDocuments?.length ?? 0) > 0 && (
                  <Alert>
                    <IconAlertTriangle />
                    <AlertTitle>Documentación pendiente</AlertTitle>
                    <AlertDescription>
                      <ul className="list-disc pl-4">
                        {application.pendingDocuments?.map((pending) => (
                          <li key={pending.requirementId ?? pending.code}>
                            {pending.name ?? pending.code ?? "Documento"}
                            {" — "}
                            {pending.reason === "OBSERVED"
                              ? pending.observation ?? "Observado, requiere una nueva entrega."
                              : "Falta adjuntar."}
                          </li>
                        ))}
                      </ul>
                    </AlertDescription>
                  </Alert>
                )}

                <div className="grid gap-4 lg:grid-cols-2">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-base">Datos de la solicitud</CardTitle>
                    </CardHeader>
                    <CardContent className="grid gap-3 text-sm">
                      <DetailRow
                        label="Programa"
                        value={application.programId ? (
                          <Link
                            to="/gestion/programas/$programaId"
                            params={{ programaId: application.programId }}
                            className="hover:underline"
                          >
                            {application.programName || "Programa"}
                          </Link>
                        ) : application.programName}
                      />
                      <DetailRow
                        label="Edición"
                        value={application.programId && application.programEditionId ? (
                          <Link
                            to="/gestion/programas/$programaId/convocatorias/$edicionId"
                            params={{
                              programaId: application.programId,
                              edicionId: application.programEditionId,
                            }}
                            className="hover:underline"
                          >
                            {application.programEditionName || "Edición"}
                          </Link>
                        ) : application.programEditionName}
                      />
                      <DetailRow label="Presentada" value={formatApplicationDateTime(application.submittedAt)} />
                      <DetailRow label="Última actualización" value={formatApplicationDateTime(application.updatedAt)} />
                      {application.resolvedAt && (
                        <DetailRow label="Resuelta" value={formatApplicationDateTime(application.resolvedAt)} />
                      )}
                      {application.resolutionReason && (
                        <DetailRow label="Motivo de la resolución" value={application.resolutionReason} />
                      )}
                      {application.originTicketId && (
                        <DetailRow label="Trámite de origen" value={application.originTicketId} mono />
                      )}
                      <DetailRow label="Convocatoria" value={application.enrollmentPeriodId} mono />
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader>
                      <CardTitle className="text-base">Personas vinculadas</CardTitle>
                    </CardHeader>
                    <CardContent className="grid gap-4 text-sm">
                      <PersonRow
                        label="Titular"
                        id={application.userId}
                        name={application.userName}
                        email={application.userEmail}
                      />
                      <PersonRow
                        label="Registrada por"
                        id={application.registeredByUserId}
                        name={application.registeredByUserName
                          ?? (application.registeredByUserId === application.userId ? application.userName : undefined)}
                        email={application.registeredByUserId === application.userId ? application.userEmail : undefined}
                      />
                      <PersonRow
                        label="Trabajador asignado"
                        id={application.assignedWorkerUserId}
                        name={application.assignedWorkerName}
                        empty="Sin asignar"
                      />
                      {application.idempotencyKey && (
                        <DetailRow label="Clave de idempotencia" value={application.idempotencyKey} mono />
                      )}
                    </CardContent>
                  </Card>
                </div>

                <ApplicationDocumentsSection applicationId={solicitudId} />
              </>
            )}
          </div>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function ApplicationDocumentsSection({ applicationId }: { applicationId: string }) {
  const documents = useQuery(list6Options({ path: { applicationId } }))

  return (
    <section aria-labelledby="admin-application-documents" className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 id="admin-application-documents" className="font-heading text-xl font-medium">Documentación entregada</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Validá u observá cada entrega sin salir de la solicitud.
          </p>
        </div>
        <Button
          type="button"
          size="sm"
          variant="outline"
          disabled={documents.isFetching}
          onClick={() => documents.refetch()}
        >
          <IconRefresh className={documents.isFetching ? "animate-spin" : undefined} />
          Actualizar
        </Button>
      </div>

      {documents.isPending ? (
        <Skeleton className="h-40 w-full" />
      ) : documents.isError ? (
        <DetailError
          error={documents.error}
          title="No se pudieron cargar los documentos"
          retry={() => documents.refetch()}
        />
      ) : (documents.data?.length ?? 0) === 0 ? (
        <Empty className="min-h-56 border">
          <EmptyHeader>
            <EmptyMedia variant="icon"><IconFileDescription /></EmptyMedia>
            <EmptyTitle>Sin documentos entregados</EmptyTitle>
            <EmptyDescription>Esta solicitud todavía no tiene archivos para revisar.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <AdminDocumentsTable applicationId={applicationId} documents={documents.data ?? []} />
      )}
    </section>
  )
}

function DetailRow({
  label,
  value,
  description,
  mono,
}: {
  label: string
  value?: ReactNode
  description?: string
  mono?: boolean
}) {
  return (
    <div className="grid min-w-0 gap-0.5">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className={mono ? "break-all font-mono text-xs" : "truncate font-medium"}>{value || "—"}</span>
      {description && <span className="truncate text-xs text-muted-foreground">{description}</span>}
    </div>
  )
}

function PersonRow({
  label,
  id,
  name,
  email,
  empty = "—",
}: {
  label: string
  id?: number
  name?: string
  email?: string
  empty?: string
}) {
  if (!name && id == null) return <DetailRow label={label} value={empty} />

  return (
    <div className="grid min-w-0 gap-1.5">
      <span className="text-xs text-muted-foreground">{label}</span>
      <div className="flex min-w-0 items-center gap-2.5">
        <UserAvatar user={{ id, name, email }} size="sm" />
        <div className="min-w-0">
          <p className="truncate font-medium">{name || `Usuario #${id}`}</p>
          <p className="truncate text-xs text-muted-foreground">
            {email || (id != null ? `ID interno ${id}` : "—")}
          </p>
        </div>
      </div>
    </div>
  )
}

function ApplicationStatusBadge({ status }: { status: AdminApplicationResponse["status"] }) {
  if (!status) return <Badge variant="outline">Sin estado</Badge>

  return (
    <Badge
      className="w-fit"
      variant={status === "APPROVED" ? "default" : status === "REJECTED" ? "destructive" : isApplicationResolved(status) ? "outline" : "secondary"}
    >
      {applicationStatusLabels[status]}
    </Badge>
  )
}

function DetailError({
  error,
  title = "No se pudo cargar la solicitud",
  retry,
}: {
  error: ErrorResponse
  title?: string
  retry: () => void
}) {
  const notFound = error.status === 404
  return (
    <Alert variant="destructive">
      <IconAlertTriangle />
      <AlertTitle>{notFound ? "No encontramos la solicitud" : title}</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-3">
        <span>{error.message ?? (notFound ? "Verificá el identificador." : "Intentá nuevamente en unos instantes.")}</span>
        <div className="flex gap-2">
          <Button type="button" size="sm" variant="outline" onClick={retry}>Reintentar</Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            render={<Link to="/gestion/solicitudes" search={{ page: 1 }} />}
          >
            Volver a la bandeja
          </Button>
        </div>
      </AlertDescription>
    </Alert>
  )
}
