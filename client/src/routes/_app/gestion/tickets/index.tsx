import { IconAlertTriangle, IconInfoCircle, IconRefresh, IconTicket } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useMemo, useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { TicketPriorityBadge, TicketSheet, TicketStateBadge, TicketWorker } from "@/components/tickets/TicketSheet"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { findAllOptions, list9Options } from "@/generated/@tanstack/react-query.gen"
import type { SocialTicketResponse, UserManagementResponse } from "@/generated/types.gen"
import { formatDateTime } from "@/lib/core-events"
import { TICKET_MANAGE_PERMISSION, TICKET_STATES, TICKET_VIEW_PERMISSION, ticketCoreStatusLabel, ticketStateLabel, type TicketState } from "@/lib/tickets"
import { cn } from "@/lib/utils"

const PAGE_SIZE = 20
const ALL_STATES = "__all__"

const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
  estado: z.enum(TICKET_STATES as [TicketState, ...TicketState[]]).optional().catch(undefined),
  atencion: z.boolean().optional().catch(undefined),
})

export const Route = createFileRoute("/_app/gestion/tickets/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const { page, estado, atencion } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const permissions = user.permissions ?? []
  const canView = permissions.includes(TICKET_VIEW_PERMISSION)
  const canManage = permissions.includes(TICKET_MANAGE_PERMISSION)
  const canViewUsers = permissions.includes("users:view")
  const [selected, setSelected] = useState<SocialTicketResponse | null>(null)

  const tickets = useQuery({
    ...list9Options({ query: { page: page - 1, size: PAGE_SIZE, state: estado, attention: atencion || undefined } }),
    enabled: canView,
    placeholderData: keepPreviousData,
  })
  // El listado solo trae el id del asignado: el directorio de usuarios resuelve nombre y avatar.
  const usersQuery = useQuery({ ...findAllOptions(), enabled: canView && canViewUsers })
  const users = useMemo(() => {
    const map = new Map<number, UserManagementResponse>()
    for (const item of usersQuery.data ?? []) if (item.id != null) map.set(item.id, item)
    return map
  }, [usersQuery.data])

  const items = tickets.data?.content ?? []
  const totalItems = Number(tickets.data?.totalElements ?? 0)
  const totalPages = Math.max(1, tickets.data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)
  const hasFilters = !!estado || !!atencion

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Tickets" }]} />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 space-y-4 py-2 sm:mx-4! lg:py-4">
            {!canView ? (
              <Alert>
                <IconAlertTriangle />
                <AlertTitle>Sin acceso</AlertTitle>
                <AlertDescription>Tu rol activo no tiene el permiso {TICKET_VIEW_PERMISSION} para consultar los tickets derivados a Desarrollo Social.</AlertDescription>
              </Alert>
            ) : <>
              <div className="flex flex-col gap-3 rounded-xl border bg-muted/20 p-3 sm:flex-row sm:items-end">
                <div className="grid gap-1.5 sm:w-60">
                  <Label htmlFor="ticket-state">Estado</Label>
                  <Select
                    value={estado ?? ALL_STATES}
                    onValueChange={(value) => void navigate({ search: (prev) => ({ ...prev, page: 1, estado: !value || value === ALL_STATES ? undefined : value as TicketState }) })}
                  >
                    <SelectTrigger id="ticket-state" className="w-full">
                      <SelectValue>{(value: string) => value === ALL_STATES ? "Todos" : ticketStateLabel(value)}</SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={ALL_STATES}>Todos</SelectItem>
                      {TICKET_STATES.map((value) => <SelectItem key={value} value={value}>{ticketStateLabel(value)}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
                <Label className="flex h-9 items-center gap-2 font-normal">
                  <Switch
                    checked={!!atencion}
                    onCheckedChange={(checked) => void navigate({ search: (prev) => ({ ...prev, page: 1, atencion: checked || undefined }) })}
                  />
                  Solo los que requieren atención
                </Label>
                <div className="flex items-center gap-2 sm:ml-auto">
                  {tickets.dataUpdatedAt > 0 && <span className="text-xs text-muted-foreground">Actualizado {new Date(tickets.dataUpdatedAt).toLocaleTimeString("es-AR")}</span>}
                  <Button size="sm" variant="ghost" disabled={tickets.isFetching} onClick={() => void tickets.refetch()}>
                    <IconRefresh className={tickets.isFetching ? "animate-spin" : undefined} />
                    <span className="sr-only sm:not-sr-only">Refrescar</span>
                  </Button>
                </div>
              </div>

              {tickets.isPending ? (
                <div className="space-y-2">{Array.from({ length: 6 }, (_, index) => <Skeleton key={index} className="h-12 rounded-lg" />)}</div>
              ) : tickets.isError ? (
                <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                  <p>No se pudieron cargar los tickets: {tickets.error.message ?? "el servidor no respondió."}</p>
                  <Button size="sm" variant="outline" onClick={() => void tickets.refetch()}>Reintentar</Button>
                </div>
              ) : items.length === 0 ? (
                <Empty className="min-h-72 border">
                  <EmptyHeader>
                    <EmptyMedia variant="icon"><IconTicket /></EmptyMedia>
                    <EmptyTitle>{hasFilters ? "Sin resultados" : "Todavía no hay tickets"}</EmptyTitle>
                    <EmptyDescription>
                      {hasFilters ? "No hay tickets para estos filtros." : "Cuando M2 derive un ticket a Desarrollo Social, va a aparecer en esta bandeja."}
                    </EmptyDescription>
                  </EmptyHeader>
                </Empty>
              ) : (
                <div className={cn("overflow-x-auto rounded-xl border transition-opacity", tickets.isPlaceholderData && "opacity-60")}>
                  <Table className="min-w-4xl">
                    <TableHeader>
                      <TableRow>
                        <TableHead className="pl-4">Ticket</TableHead>
                        <TableHead>Estado</TableHead>
                        <TableHead>Prioridad</TableHead>
                        <TableHead>Ciudadano</TableHead>
                        <TableHead>Asignado</TableHead>
                        <TableHead className="pr-4">Actualizado</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {items.map((item) => (
                        <TableRow
                          key={item.id}
                          tabIndex={0}
                          className="cursor-pointer"
                          onClick={() => setSelected(item)}
                          onKeyDown={(event) => {
                            if (event.key !== "Enter" && event.key !== " ") return
                            event.preventDefault()
                            setSelected(item)
                          }}
                        >
                          <TableCell className="pl-4">
                            <div className="flex items-center gap-1.5">
                              <span className="font-mono text-sm font-medium">{item.publicId ?? "—"}</span>
                              {item.requiresAttention && <IconAlertTriangle aria-label="Requiere atención" className="size-4 text-red-600 dark:text-red-400" />}
                              {item.informationPending && <IconInfoCircle aria-label="Información pendiente" className="size-4 text-amber-600 dark:text-amber-400" />}
                            </div>
                            <p className="text-xs text-muted-foreground">Origen: {ticketCoreStatusLabel(item.currentStatus)}</p>
                          </TableCell>
                          <TableCell><TicketStateBadge state={item.state} /></TableCell>
                          <TableCell><TicketPriorityBadge priority={item.currentPriority} /></TableCell>
                          <TableCell className="max-w-48">
                            {item.anonymous ? <span className="text-muted-foreground">Anónimo</span> : <span className="block truncate font-mono text-xs text-muted-foreground">{item.citizenId ?? "—"}</span>}
                          </TableCell>
                          <TableCell className="max-w-56"><TicketWorker workerId={item.assignedWorkerId} users={users} /></TableCell>
                          <TableCell className="whitespace-nowrap pr-4 text-muted-foreground">{formatDateTime(item.sourceUpdatedAt)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </>}
          </div>
        </div>

        {canView && !tickets.isPending && !tickets.isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={(nextPage) => void navigate({ search: (prev) => ({ ...prev, page: nextPage }) })}
          />
        )}
      </SidebarShellContent>

      <TicketSheet ticket={selected} users={users} canManage={canManage} canPickUsers={canViewUsers} onClose={() => setSelected(null)} />
    </SidebarShell>
  )
}
