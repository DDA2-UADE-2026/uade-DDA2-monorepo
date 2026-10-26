import { Link } from "@tanstack/react-router"

import { UserAvatar } from "@/components/UserAvatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { AdminApplicationListItemResponse } from "@/generated/types.gen"
import {
  applicationStatusLabels,
  formatApplicationDateTime,
  isApplicationResolved,
} from "@/lib/application-flow"

// Inside a program's own section the program is implied, so only the edition is shown.
export function AdminApplicationsTable({
  applications,
  hideProgram = false,
}: {
  applications: AdminApplicationListItemResponse[]
  hideProgram?: boolean
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-20">Número</TableHead>
          <TableHead>Titular</TableHead>
          <TableHead>{hideProgram ? "Edición" : "Programa"}</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead>Presentada</TableHead>
          <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {applications.map((application) => (
          <ApplicationRow key={application.id} application={application} hideProgram={hideProgram} />
        ))}
      </TableBody>
    </Table>
  )
}

function ApplicationRow({
  application,
  hideProgram,
}: {
  application: AdminApplicationListItemResponse
  hideProgram: boolean
}) {
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
        {!hideProgram && (application.programId ? (
          <Link
            to="/gestion/programas/$programaId"
            params={{ programaId: application.programId }}
            className="block truncate hover:underline"
          >
            {application.programName || "Programa"}
          </Link>
        ) : (
          <p className="truncate">{application.programName || "—"}</p>
        ))}
        {application.programId && application.programEditionId ? (
          <Link
            to="/gestion/programas/$programaId/convocatorias/$edicionId"
            params={{ programaId: application.programId, edicionId: application.programEditionId }}
            className={hideProgram ? "block truncate hover:underline" : "block truncate text-xs text-muted-foreground hover:underline"}
          >
            {application.programEditionName || "Edición"}
          </Link>
        ) : (
          <p className={hideProgram ? "truncate" : "truncate text-xs text-muted-foreground"}>{application.programEditionName || "—"}</p>
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
