import { IconArrowDownLeft, IconArrowUpRight, IconRefresh, IconSearch, IconX } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { useState, type FormEvent } from "react"

import { DataPagination } from "@/components/DataPagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { getCoreEventOptions, listCoreEventsOptions } from "@/generated/@tanstack/react-query.gen"
import type { CoreEventSummary, ListCoreEventsData } from "@/generated/types.gen"
import { coreStatusLabel, formatDateTime } from "@/lib/core-events"
import { cn } from "@/lib/utils"

type EventStatus = NonNullable<NonNullable<ListCoreEventsData["query"]>["status"]>
type Filters = { eventType?: string; status?: EventStatus; correlationId?: string }

const PAGE_SIZE = 25
const ALL_STATUSES = "__all__"
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const EVENT_STATUSES: readonly EventStatus[] = ["ROUTED", "NO_SUBSCRIBERS", "REJECTED", "DUPLICATE"]

const statusStyles: Record<string, string> = {
  ROUTED: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  DELIVERED: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  NO_SUBSCRIBERS: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400",
  PENDING: "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-400",
  PENDING_RETRY: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400",
  DUPLICATE: "border-zinc-500/30 bg-zinc-500/10 text-zinc-700 dark:text-zinc-300",
  REJECTED: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400",
  DEAD: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400",
}

export function CoreStatusBadge({ status }: { status?: string }) {
  return <Badge variant="outline" className={cn(status && statusStyles[status])}>{coreStatusLabel(status)}</Badge>
}

export function CoreEventsTable({ module }: { module?: string }) {
  const [eventType, setEventType] = useState("")
  const [status, setStatus] = useState<string>(ALL_STATUSES)
  const [correlationId, setCorrelationId] = useState("")
  const [filters, setFilters] = useState<Filters>({})
  const [validationError, setValidationError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [selected, setSelected] = useState<CoreEventSummary | null>(null)

  const events = useQuery({
    ...listCoreEventsOptions({ query: { page, size: PAGE_SIZE, ...filters } }),
    placeholderData: keepPreviousData,
  })
  const items = events.data?.items ?? []
  const totalItems = events.data?.total ?? 0
  const totalPages = Math.max(1, events.data?.pages ?? 1)

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const type = eventType.trim()
    const correlation = correlationId.trim()
    if (type.length > 120) return setValidationError("El tipo de evento admite hasta 120 caracteres.")
    if (correlation && !UUID_PATTERN.test(correlation)) return setValidationError("La correlación debe ser un UUID.")
    setValidationError(null)
    setFilters({
      eventType: type || undefined,
      status: status === ALL_STATUSES ? undefined : status as EventStatus,
      correlationId: correlation || undefined,
    })
    setPage(1)
  }

  const clear = () => {
    setEventType("")
    setStatus(ALL_STATUSES)
    setCorrelationId("")
    setFilters({})
    setValidationError(null)
    setPage(1)
  }

  const hasFilters = Object.values(filters).some(Boolean)

  return (
    <Card className="min-w-0 gap-0 pb-0">
      <CardHeader className="pb-4">
        <CardTitle>Eventos</CardTitle>
        <CardDescription>Enviados y recibidos por nuestro módulo según Core. Elegí una fila para ver el sobre y sus entregas.</CardDescription>
        <CardAction>
          <Button size="xs" variant="ghost" disabled={events.isFetching} onClick={() => void events.refetch()}>
            <IconRefresh className={events.isFetching ? "animate-spin" : undefined} />
            <span className="sr-only sm:not-sr-only">Refrescar</span>
          </Button>
        </CardAction>
      </CardHeader>

      <CardContent className="min-w-0 space-y-4 px-0">
        <form className="mx-4 flex flex-col gap-3 rounded-xl border bg-muted/20 p-3 lg:flex-row lg:items-end" onSubmit={submit}>
          <div className="grid min-w-0 flex-1 gap-1.5">
            <Label htmlFor="core-event-type">Tipo de evento</Label>
            <Input id="core-event-type" value={eventType} onChange={(event) => setEventType(event.target.value)} placeholder="Ej.: desarrollo-social.solicitud.aprobada" />
          </div>
          <div className="grid gap-1.5 lg:w-48">
            <Label htmlFor="core-event-status">Estado</Label>
            <Select value={status} onValueChange={(value) => setStatus(value ?? ALL_STATUSES)}>
              <SelectTrigger id="core-event-status" className="w-full">
                <SelectValue>{(value: string) => value === ALL_STATUSES ? "Todos" : coreStatusLabel(value)}</SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL_STATUSES}>Todos</SelectItem>
                {EVENT_STATUSES.map((value) => <SelectItem key={value} value={value}>{coreStatusLabel(value)}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="grid min-w-0 flex-1 gap-1.5">
            <Label htmlFor="core-event-correlation">Correlación</Label>
            <Input id="core-event-correlation" value={correlationId} onChange={(event) => setCorrelationId(event.target.value)} placeholder="UUID del trámite" aria-invalid={validationError !== null} className="font-mono" />
          </div>
          <Button type="submit"><IconSearch />Buscar</Button>
          {hasFilters && <Button type="button" variant="ghost" onClick={clear}><IconX />Limpiar</Button>}
        </form>
        {validationError && <p className="-mt-2 px-4 text-sm text-destructive">{validationError}</p>}

        {events.isPending ? (
          <div className="space-y-2 px-4 pb-4">{Array.from({ length: 6 }, (_, index) => <Skeleton key={index} className="h-10 rounded-lg" />)}</div>
        ) : events.isError ? (
          <div className="flex flex-col items-start gap-2 px-4 pb-4 text-sm text-muted-foreground">
            <p>No se pudieron cargar los eventos: {events.error.message ?? "Core no respondió."}</p>
            <Button size="sm" variant="outline" onClick={() => void events.refetch()}>Reintentar</Button>
          </div>
        ) : items.length === 0 ? (
          <p className="px-4 pb-4 text-sm text-muted-foreground">{hasFilters ? "No hay eventos para estos filtros." : "Todavía no hay eventos visibles para el módulo."}</p>
        ) : <>
          <div className={cn("overflow-x-auto transition-opacity", events.isPlaceholderData && "opacity-60")}>
            <Table className="min-w-4xl">
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Dirección</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead>Origen</TableHead>
                  <TableHead>Ocurrió</TableHead>
                  <TableHead className="text-right">Entregas</TableHead>
                  <TableHead className="pr-4 text-right">Procesamiento</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item) => (
                  <TableRow
                    key={item.id ?? item.eventId}
                    tabIndex={0}
                    className="cursor-pointer"
                    onClick={() => setSelected(item)}
                    onKeyDown={(event) => (event.key === "Enter" || event.key === " ") && (event.preventDefault(), setSelected(item))}
                  >
                    <TableCell className="pl-4"><Direction outgoing={item.sourceModule === module} /></TableCell>
                    <TableCell>
                      <p className="font-medium">{item.eventType ?? "—"}</p>
                      <p className="font-mono text-xs text-muted-foreground">v{item.eventVersion ?? "?"} · {item.ingestionChannel ?? "—"}</p>
                    </TableCell>
                    <TableCell>
                      <CoreStatusBadge status={item.status} />
                      {item.rejectionCode && <p className="mt-1 font-mono text-xs text-destructive">{item.rejectionCode}</p>}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{item.sourceModule ?? "—"}</TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">{formatDateTime(item.occurredAt)}</TableCell>
                    <TableCell className="text-right tabular-nums">{item.deliveredCount ?? 0}/{item.deliveryCount ?? 0}</TableCell>
                    <TableCell className="pr-4 text-right tabular-nums text-muted-foreground">{item.processingMs != null ? `${item.processingMs} ms` : "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <DataPagination page={Math.min(page, totalPages)} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} />
        </>}
      </CardContent>

      <CoreEventSheet event={selected} module={module} onClose={() => setSelected(null)} />
    </Card>
  )
}

function Direction({ outgoing }: { outgoing: boolean }) {
  const Icon = outgoing ? IconArrowUpRight : IconArrowDownLeft
  return (
    <span className={cn("inline-flex items-center gap-1 text-xs font-medium", outgoing ? "text-sky-700 dark:text-sky-400" : "text-violet-700 dark:text-violet-400")}>
      <Icon aria-hidden="true" className="size-4" />
      {outgoing ? "Enviado" : "Recibido"}
    </span>
  )
}

function CoreEventSheet({ event, module, onClose }: { event: CoreEventSummary | null; module?: string; onClose: () => void }) {
  const eventId = event?.eventId
  const detail = useQuery({
    ...getCoreEventOptions({ path: { eventId: eventId ?? "" } }),
    enabled: !!eventId,
  })
  const deliveries = detail.data?.deliveries ?? []

  return (
    <Sheet open={event !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="w-full gap-0 data-[side=right]:sm:max-w-2xl">
        <SheetHeader className="border-b">
          <SheetTitle className="pr-8">{event?.eventType ?? "Evento"}</SheetTitle>
          <SheetDescription className="font-mono text-xs break-all">{eventId}</SheetDescription>
        </SheetHeader>
        {event && (
          <div className="min-h-0 flex-1 space-y-5 overflow-y-auto p-4">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
              <Field label="Dirección"><Direction outgoing={event.sourceModule === module} /></Field>
              <Field label="Estado"><CoreStatusBadge status={detail.data?.status ?? event.status} /></Field>
              <Field label="Origen">{event.sourceModule ?? "—"}</Field>
              <Field label="Canal">{event.ingestionChannel ?? "—"}</Field>
              <Field label="Ocurrió">{formatDateTime(event.occurredAt)}</Field>
              <Field label="Recibido por Core">{formatDateTime(event.receivedAt)}</Field>
              <Field label="Versión">{event.eventVersion ?? "—"}</Field>
              <Field label="Procesamiento">{event.processingMs != null ? `${event.processingMs} ms` : "—"}</Field>
              <Field label="Correlación" wide><span className="font-mono text-xs break-all">{event.correlationId ?? "—"}</span></Field>
              {event.rejectionCode && <Field label="Motivo de rechazo" wide><span className="font-mono text-xs text-destructive">{event.rejectionCode}</span></Field>}
            </dl>

            {detail.isPending ? <Skeleton className="h-40 rounded-xl" /> : detail.isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudo cargar el detalle: {detail.error.message ?? "Core no respondió."}</p>
                <Button size="sm" variant="outline" onClick={() => void detail.refetch()}>Reintentar</Button>
              </div>
            ) : <>
              <section className="space-y-2">
                <h3 className="font-heading font-medium">Entregas</h3>
                {deliveries.length === 0 ? <p className="text-sm text-muted-foreground">Sin entregas registradas.</p> : (
                  <div className="overflow-x-auto rounded-lg border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Destino</TableHead>
                          <TableHead>Cola</TableHead>
                          <TableHead>Estado</TableHead>
                          <TableHead className="text-right">Intentos</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {deliveries.map((delivery, index) => (
                          <TableRow key={`${delivery.targetModule}-${delivery.queueName}-${index}`}>
                            <TableCell className="font-medium">{delivery.targetModule ?? "—"}</TableCell>
                            <TableCell className="font-mono text-xs text-muted-foreground">{delivery.queueName ?? "—"}</TableCell>
                            <TableCell><CoreStatusBadge status={delivery.status} /></TableCell>
                            <TableCell className="text-right tabular-nums">{delivery.attempts ?? 0}/{delivery.maxAttempts ?? 0}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}
              </section>
              <section className="space-y-2">
                <h3 className="font-heading font-medium">Sobre</h3>
                <pre className="max-h-96 overflow-auto rounded-lg border bg-muted/40 p-3 font-mono text-xs leading-relaxed">
                  {JSON.stringify(detail.data?.envelope as unknown, null, 2)}
                </pre>
              </section>
            </>}
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}

function Field({ label, wide, children }: { label: string; wide?: boolean; children: React.ReactNode }) {
  return (
    <div className={cn("min-w-0", wide && "col-span-2")}>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-0.5">{children}</dd>
    </div>
  )
}
