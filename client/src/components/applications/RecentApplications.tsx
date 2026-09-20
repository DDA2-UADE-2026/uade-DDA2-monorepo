import { IconArrowRight, IconClipboardText } from "@tabler/icons-react"
import { useQueries, useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"

import { ApplicationError, ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { SectionHeading } from "@/components/layout/SectionHeading"
import { ProgramThumbnail } from "@/components/programs/ProgramThumbnail"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { getAvailableProgramOptions, listOptions } from "@/generated/@tanstack/react-query.gen"
import type { ApplicationResponse } from "@/generated/types.gen"
import { formatApplicationDate } from "@/lib/application-flow"
import { cn } from "@/lib/utils"

const RECENT_COUNT = 3
// El endpoint no acepta orden, así que traemos una página chica y la ordenamos acá.
const FETCH_SIZE = 10

function sortByMostRecent(applications: Array<ApplicationResponse>): Array<ApplicationResponse> {
  return [...applications]
    .sort((a, b) => (b.submittedAt ?? b.createdAt ?? "").localeCompare(a.submittedAt ?? a.createdAt ?? ""))
    .slice(0, RECENT_COUNT)
}

export function RecentApplications({ className }: { className?: string }) {
  const query = useQuery(listOptions({ query: { page: 0, size: FETCH_SIZE } }))
  const applications = sortByMostRecent(query.data?.content ?? [])

  // La solicitud no trae la portada, así que la resolvemos por programa. Si el
  // programa dejó de estar disponible la consulta falla y queda la imagen por defecto.
  const programIds = [
    ...new Set(
      applications
        .map((application) => application.programId)
        .filter((id): id is string => id !== undefined),
    ),
  ]
  const programs = useQueries({
    queries: programIds.map((id) => ({
      ...getAvailableProgramOptions({ path: { id } }),
      retry: false,
      staleTime: 5 * 60 * 1000,
    })),
  })
  const imageByProgramId = new Map(
    programs.map((program, index) => [programIds[index], program.data?.imageUrl]),
  )
  const programImageOf = (application: ApplicationResponse) =>
    application.programId ? imageByProgramId.get(application.programId) : undefined

  return (
    <section aria-labelledby="recent-applications-title" className={cn("space-y-3", className)}>
      <SectionHeading
        id="recent-applications-title"
        title="Mis solicitudes recientes"
        description="Seguí el estado de tus últimas presentaciones."
        action={
          <Button
            size="sm"
            variant="ghost"
            className="shrink-0"
            render={<Link to="/portal/solicitudes" search={{ page: 1 }} />}
          >
            Ver todas
            <IconArrowRight />
          </Button>
        }
      />

      {query.isPending ? (
        <RecentApplicationsSkeleton />
      ) : query.isError ? (
        <ApplicationError
          error={query.error}
          title="No pudimos cargar tus solicitudes"
          retry={() => query.refetch()}
        />
      ) : applications.length === 0 ? (
        <Empty className="min-h-48 rounded-xl border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <IconClipboardText />
            </EmptyMedia>
            <EmptyTitle>Todavía no presentaste solicitudes</EmptyTitle>
            <EmptyDescription>
              Cuando solicites un programa, vas a poder seguirlo desde acá.
            </EmptyDescription>
          </EmptyHeader>
          <Button variant="outline" render={<Link to="/portal/programas" search={{ page: 1 }} />}>
            Ver programas
          </Button>
        </Empty>
      ) : (
        <div className="overflow-hidden rounded-xl border">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/40 hover:bg-muted/40">
                <TableHead>Solicitud</TableHead>
                <TableHead className="hidden sm:table-cell">Presentada</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="w-12">
                  <span className="sr-only">Acciones</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {applications.map((application) => (
                <TableRow key={application.id ?? application.applicationNumber}>
                  <TableCell className="max-w-64">
                    <div className="flex items-center gap-3">
                      <ProgramThumbnail
                        imageUrl={programImageOf(application)}
                        className="size-10"
                        glowClassName="blur-xl opacity-70"
                      />
                      <div className="min-w-0">
                        <p className="truncate font-medium">
                          {application.programName ?? "Solicitud de programa"}
                        </p>
                        <p className="truncate text-xs text-muted-foreground">
                          N.º {application.applicationNumber ?? "—"}
                          {application.programEditionName ? ` · ${application.programEditionName}` : ""}
                        </p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="hidden text-muted-foreground sm:table-cell">
                    {formatApplicationDate(application.submittedAt)}
                  </TableCell>
                  <TableCell>
                    <ApplicationStatusBadge status={application.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    {application.id && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        render={
                          <Link
                            to="/portal/solicitudes/$solicitudId"
                            params={{ solicitudId: application.id }}
                          />
                        }
                      >
                        <IconArrowRight />
                        <span className="sr-only">
                          Ver solicitud N.º {application.applicationNumber ?? ""}
                        </span>
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  )
}

function RecentApplicationsSkeleton() {
  return (
    <div className="divide-y rounded-xl border" role="status" aria-label="Cargando solicitudes">
      {Array.from({ length: RECENT_COUNT }).map((_, index) => (
        <div key={index} className="flex items-center gap-4 p-3">
          <Skeleton className="size-10 shrink-0 rounded-xl" />
          <div className="min-w-0 flex-1 space-y-2">
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-3 w-1/3" />
          </div>
          <Skeleton className="hidden h-4 w-24 sm:block" />
          <Skeleton className="h-6 w-24 rounded-full" />
          <Skeleton className="size-8 rounded-md" />
        </div>
      ))}
      <span className="sr-only">Cargando solicitudes…</span>
    </div>
  )
}
