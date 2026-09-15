import {
  IconFileDescription,
  IconFilePlus,
  IconRefresh,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { UserAvatar } from "@/components/UserAvatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { list4Options } from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationListItemResponse } from "@/generated/types.gen"
import {
  applicationStatusLabels,
  formatApplicationDateTime,
  isApplicationResolved,
} from "@/lib/application-flow"

const PAGE_SIZE = 10
const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
})

export const Route = createFileRoute("/_app/gestion/solicitudes/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    list4Options({ query: { page: page - 1, size: PAGE_SIZE } }),
  )

  const applications = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Solicitudes" }]} />
        <OutletNavRightButton className="gap-1.5">
          <Button size="sm" render={<Link to="/gestion/solicitudes/asistida" />}>
            <IconFilePlus />
            Solicitud asistida
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-2 sm:mx-4! lg:py-4">
            {dataUpdatedAt > 0 && (
              <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                <span>Última actualización: {new Date(dataUpdatedAt).toLocaleTimeString("es-AR")}</span>
                <Button size="xs" variant="ghost" onClick={() => refetch()} disabled={isFetching}>
                  <IconRefresh className={isFetching ? "animate-spin" : undefined} />
                  Actualizar
                </Button>
              </div>
            )}

            {isPending ? (
              <p className="text-sm text-muted-foreground">Cargando solicitudes…</p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar las solicitudes.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>Reintentar</Button>
              </div>
            ) : applications.length === 0 ? (
              <Empty className="min-h-72 border">
                <EmptyHeader>
                  <EmptyMedia variant="icon"><IconFileDescription /></EmptyMedia>
                  <EmptyTitle>Todavía no hay solicitudes</EmptyTitle>
                  <EmptyDescription>
                    Cuando se presente una solicitud, propia o asistida, va a aparecer en esta bandeja.
                  </EmptyDescription>
                </EmptyHeader>
                <EmptyContent>
                  <Button size="sm" render={<Link to="/gestion/solicitudes/asistida" />}>
                    <IconFilePlus />
                    Registrar solicitud asistida
                  </Button>
                </EmptyContent>
              </Empty>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-20">Número</TableHead>
                    <TableHead>Titular</TableHead>
                    <TableHead>Programa</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead>Presentada</TableHead>
                    <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {applications.map((application) => (
                    <ApplicationRow key={application.id} application={application} />
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>

        {!isPending && !isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={(nextPage) => navigate({ search: { page: nextPage } })}
          />
        )}
      </SidebarShellContent>
    </SidebarShell>
  )
}

function ApplicationRow({ application }: { application: AdminApplicationListItemResponse }) {
  const applicationId = application.id
  const titular = application.userName || "Titular sin nombre"

  return (
    <TableRow>
      <TableCell className="font-medium">
        {applicationId ? (
          <Link
            to="/gestion/solicitudes/$solicitudId"
            params={{ solicitudId: applicationId }}
            className="hover:underline"
          >
            {application.applicationNumber ?? "—"}
          </Link>
        ) : application.applicationNumber ?? "—"}
      </TableCell>
      <TableCell className="min-w-56">
        <div className="flex min-w-0 items-center gap-2.5">
          <UserAvatar
            user={{
              id: application.userId,
              name: application.userName,
              email: application.userEmail,
            }}
            size="sm"
          />
          <div className="min-w-0">
            <p className="truncate font-medium">
              {applicationId ? (
                <Link
                  to="/gestion/solicitudes/$solicitudId"
                  params={{ solicitudId: applicationId }}
                  className="hover:underline"
                >
                  {titular}
                </Link>
              ) : titular}
            </p>
            <p className="truncate text-xs text-muted-foreground">{application.userEmail || "—"}</p>
          </div>
        </div>
      </TableCell>
      <TableCell className="min-w-48 max-w-xs">
        {application.programId ? (
          <Link
            to="/gestion/programas/$programaId"
            params={{ programaId: application.programId }}
            className="block truncate hover:underline"
          >
            {application.programName || "Programa"}
          </Link>
        ) : (
          <p className="truncate">{application.programName || "—"}</p>
        )}
        {application.programId && application.programEditionId ? (
          <Link
            to="/gestion/programas/$programaId/convocatorias/$edicionId"
            params={{ programaId: application.programId, edicionId: application.programEditionId }}
            className="block truncate text-xs text-muted-foreground hover:underline"
          >
            {application.programEditionName || "Edición"}
          </Link>
        ) : (
          <p className="truncate text-xs text-muted-foreground">{application.programEditionName || "—"}</p>
        )}
      </TableCell>
      <TableCell>
        <ApplicationStatusBadge status={application.status} />
      </TableCell>
      <TableCell className="text-muted-foreground">
        {formatApplicationDateTime(application.submittedAt)}
      </TableCell>
      <TableCell>
        <div className="flex justify-end gap-1">
          {applicationId && (
            <Button
              size="sm"
              variant="outline"
              aria-label={`Ver la solicitud de ${titular}`}
              render={(
                <Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: applicationId }} />
              )}
            >
              Ver detalle
            </Button>
          )}
        </div>
      </TableCell>
    </TableRow>
  )
}

function ApplicationStatusBadge({ status }: { status: AdminApplicationListItemResponse["status"] }) {
  if (!status) return <Badge variant="outline">Sin estado</Badge>

  return (
    <Badge variant={status === "APPROVED" ? "default" : status === "REJECTED" ? "destructive" : isApplicationResolved(status) ? "outline" : "secondary"}>
      {applicationStatusLabels[status]}
    </Badge>
  )
}
