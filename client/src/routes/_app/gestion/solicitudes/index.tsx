import {
  IconFileCheck,
  IconFileDescription,
  IconFilePlus,
  IconRefresh,
  IconSearch,
  IconX,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
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
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { listManagedApplicationsOptions } from "@/generated/@tanstack/react-query.gen"
import type { AdminApplicationResponse, ApplicationResponse } from "@/generated/types.gen"
import {
  applicationStatusLabels,
  formatApplicationDateTime,
  isApplicationResolved,
} from "@/lib/application-flow"

const PAGE_SIZE = 10
const ALL_STATUSES = "ALL"
const applicationStatuses = Object.keys(applicationStatusLabels) as NonNullable<ApplicationResponse["status"]>[]

const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
  estado: z.enum(applicationStatuses).optional().catch(undefined),
  q: z.string().catch("").default(""),
})

export const Route = createFileRoute("/_app/gestion/solicitudes/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page, estado, q } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [searchDraft, setSearchDraft] = useState<string | null>(null)
  const searchInput = searchDraft ?? q

  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    listManagedApplicationsOptions({
      query: {
        page: page - 1,
        size: PAGE_SIZE,
        status: estado,
        search: q.trim() || undefined,
      },
    }),
  )

  const applications = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)
  const filtered = Boolean(estado) || q.trim().length > 0

  const applyFilters = (next: { estado?: NonNullable<ApplicationResponse["status"]>; q?: string }) => {
    setSearchDraft(null)
    navigate({
      search: {
        page: 1,
        estado: "estado" in next ? next.estado : estado,
        q: next.q ?? q,
      },
    })
  }

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
            <form
              className="mb-3 flex flex-col gap-2 sm:flex-row"
              noValidate
              onSubmit={(event) => {
                event.preventDefault()
                applyFilters({ q: searchInput.trim() })
              }}
            >
              <InputGroup className="sm:max-w-md">
                <InputGroupAddon>
                  <IconSearch />
                </InputGroupAddon>
                <InputGroupInput
                  value={searchInput}
                  onChange={(event) => setSearchDraft(event.target.value)}
                  placeholder="Buscar por número, nombre, usuario o correo"
                  aria-label="Buscar solicitudes"
                />
              </InputGroup>
              <Select
                value={estado ?? ALL_STATUSES}
                onValueChange={(value: string | null) =>
                  applyFilters({
                    estado: !value || value === ALL_STATUSES
                      ? undefined
                      : value as NonNullable<ApplicationResponse["status"]>,
                  })
                }
              >
                <SelectTrigger className="sm:w-56" aria-label="Filtrar por estado">
                  <SelectValue placeholder="Todos los estados" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_STATUSES}>Todos los estados</SelectItem>
                  {applicationStatuses.map((status) => (
                    <SelectItem key={status} value={status}>
                      {applicationStatusLabels[status]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button type="submit" variant="outline" className="sm:min-w-28">
                <IconSearch />
                Buscar
              </Button>
              {filtered && (
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    setSearchDraft(null)
                    navigate({ search: { page: 1, estado: undefined, q: "" } })
                  }}
                >
                  <IconX />
                  Limpiar
                </Button>
              )}
            </form>

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
                  <EmptyTitle>{filtered ? "Sin resultados" : "Todavía no hay solicitudes"}</EmptyTitle>
                  <EmptyDescription>
                    {filtered
                      ? "Ninguna solicitud coincide con los filtros aplicados."
                      : "Cuando se presente una solicitud, propia o asistida, va a aparecer en esta bandeja."}
                  </EmptyDescription>
                </EmptyHeader>
                <EmptyContent>
                  {filtered ? (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setSearchDraft(null)
                        navigate({ search: { page: 1, estado: undefined, q: "" } })
                      }}
                    >
                      Limpiar filtros
                    </Button>
                  ) : (
                    <Button size="sm" render={<Link to="/gestion/solicitudes/asistida" />}>
                      <IconFilePlus />
                      Registrar solicitud asistida
                    </Button>
                  )}
                </EmptyContent>
              </Empty>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-24">N.º</TableHead>
                    <TableHead>Titular</TableHead>
                    <TableHead>Programa</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead>Documentación</TableHead>
                    <TableHead>Presentada</TableHead>
                    <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {applications.map((item) => (
                    <ApplicationRow key={item.application?.id} item={item} />
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
            onPageChange={(nextPage) => navigate({ search: { page: nextPage, estado, q } })}
          />
        )}
      </SidebarShellContent>
    </SidebarShell>
  )
}

function ApplicationRow({ item }: { item: AdminApplicationResponse }) {
  const application = item.application
  const applicationId = application?.id
  const pending = application?.pendingDocuments ?? []

  return (
    <TableRow>
      <TableCell className="font-medium">
        {applicationId ? (
          <Link
            to="/gestion/solicitudes/$solicitudId"
            params={{ solicitudId: applicationId }}
            className="hover:underline"
          >
            N.º {application?.applicationNumber ?? "—"}
          </Link>
        ) : (
          <span>N.º {application?.applicationNumber ?? "—"}</span>
        )}
      </TableCell>
      <TableCell className="min-w-48">
        <p className="truncate font-medium">{item.applicant?.name || "Titular sin nombre"}</p>
        <p className="truncate text-xs text-muted-foreground">
          {item.applicant?.email || (item.applicant?.username ? `@${item.applicant.username}` : "—")}
        </p>
      </TableCell>
      <TableCell className="min-w-48 max-w-xs">
        <p className="truncate">{application?.programName || "—"}</p>
        <p className="truncate text-xs text-muted-foreground">{application?.programEditionName || "—"}</p>
      </TableCell>
      <TableCell>
        <ApplicationStatusBadge status={application?.status} />
      </TableCell>
      <TableCell>
        {pending.length === 0 ? (
          <span className="text-sm text-muted-foreground">Al día</span>
        ) : (
          <Badge variant="outline" className="border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/50 dark:text-amber-300">
            {pending.length} pendiente{pending.length === 1 ? "" : "s"}
          </Badge>
        )}
      </TableCell>
      <TableCell className="text-muted-foreground">
        {formatApplicationDateTime(application?.submittedAt)}
      </TableCell>
      <TableCell>
        <div className="flex justify-end gap-1">
          {applicationId && (
            <>
              <Button
                size="icon-sm"
                variant="ghost"
                aria-label={`Revisar documentos de la solicitud N.º ${application?.applicationNumber ?? ""}`}
                render={<Link to="/gestion/documentos" search={{ solicitudId: applicationId }} />}
              >
                <IconFileCheck />
              </Button>
              <Button
                size="sm"
                variant="outline"
                render={(
                  <Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: applicationId }} />
                )}
              >
                Ver detalle
              </Button>
            </>
          )}
        </div>
      </TableCell>
    </TableRow>
  )
}

function ApplicationStatusBadge({ status }: { status: ApplicationResponse["status"] }) {
  if (!status) return <Badge variant="outline">Sin estado</Badge>

  return (
    <Badge variant={status === "APPROVED" ? "default" : status === "REJECTED" ? "destructive" : isApplicationResolved(status) ? "outline" : "secondary"}>
      {applicationStatusLabels[status]}
    </Badge>
  )
}
