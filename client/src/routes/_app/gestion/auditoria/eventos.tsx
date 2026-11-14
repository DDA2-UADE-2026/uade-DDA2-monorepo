import { IconAlertTriangle, IconApi, IconArrowDownLeft, IconArrowUpRight, IconHeartbeat, IconRefresh, IconServer } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import type { ReactNode } from "react"
import { z } from "zod"

import { DeadLettersChart, HourlyVolumeChart, ReceivedByStatusChart, TopPublishedTypesChart } from "@/components/core-events/CoreEventCharts"
import { CoreEventsTable } from "@/components/core-events/CoreEventsTable"
import { SectionTitle, StatTile } from "@/components/indicators/IndicatorUi"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import {
  getCoreConnectionStatusOptions,
  getCoreDashboardOptions,
  getCoreLocalMetricsOptions,
  listCorePublicationsOptions,
  listCoreSubscriptionsOptions,
} from "@/generated/@tanstack/react-query.gen"
import type { CoreConnectionStatusResponse, CoreLocalMetrics, CoreModuleDashboard } from "@/generated/types.gen"
import { formatDateTime } from "@/lib/core-events"
import { formatCount } from "@/lib/indicators"
import { cn } from "@/lib/utils"

const CORE_VIEW_PERMISSION = "integrations:core:view"
const STATUS_REFRESH_MS = 30_000
const DASHBOARD_REFRESH_MS = 60_000

const WINDOWS = { "1": "1 h", "24": "24 h", "168": "7 días", "720": "30 días" } as const
type WindowKey = keyof typeof WINDOWS

const searchSchema = z.object({
  ventana: z.enum(Object.keys(WINDOWS) as [WindowKey, ...WindowKey[]]).optional().catch(undefined),
})

export const Route = createFileRoute("/_app/gestion/auditoria/eventos")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

type ConnectionState = NonNullable<NonNullable<CoreConnectionStatusResponse["http"]>["state"]>

const stateLabels: Record<ConnectionState, string> = {
  UP: "Conectado",
  DEGRADED: "Degradado",
  DOWN: "Sin conexión",
  DISABLED: "Desactivado",
  NOT_CONFIGURED: "Sin configurar",
}

const stateStyles: Record<ConnectionState, { badge: string; dot: string }> = {
  UP: { badge: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400", dot: "bg-emerald-500" },
  DEGRADED: { badge: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400", dot: "bg-amber-500" },
  DOWN: { badge: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400", dot: "bg-red-500" },
  DISABLED: { badge: "border-zinc-500/30 bg-zinc-500/10 text-zinc-700 dark:text-zinc-300", dot: "bg-zinc-400" },
  NOT_CONFIGURED: { badge: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400", dot: "bg-amber-500" },
}

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const canView = (user.permissions ?? []).includes(CORE_VIEW_PERMISSION)
  const windowKey: WindowKey = search.ventana ?? "24"

  const status = useQuery({ ...getCoreConnectionStatusOptions(), enabled: canView, refetchInterval: STATUS_REFRESH_MS })
  const dashboard = useQuery({
    ...getCoreDashboardOptions({ query: { windowHours: Number(windowKey) } }),
    enabled: canView,
    placeholderData: keepPreviousData,
    refetchInterval: DASHBOARD_REFRESH_MS,
  })
  // /metrics solo consulta nuestra base: mantiene visibles outbox/inbox aunque Core no responda.
  const localFallback = useQuery({ ...getCoreLocalMetricsOptions(), enabled: canView && dashboard.isError })
  const publications = useQuery({ ...listCorePublicationsOptions(), enabled: canView })
  const subscriptions = useQuery({ ...listCoreSubscriptionsOptions(), enabled: canView })

  const refreshAll = () => {
    void status.refetch()
    void dashboard.refetch()
    void publications.refetch()
    void subscriptions.refetch()
  }
  const fetching = status.isFetching || dashboard.isFetching
  const local = dashboard.data?.local ?? localFallback.data
  const module = status.data?.module ?? dashboard.data?.core?.module

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Auditoría" }, { label: "Eventos" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-auto">
          <main className="mx-auto w-full max-w-7xl space-y-5 p-4 lg:p-6">
            <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
              <div>
                <h1 className="font-heading text-2xl font-medium">Eventos del núcleo</h1>
                <p className="text-sm text-muted-foreground">Conexión con Core y tráfico de eventos enviados y recibidos{module ? ` por ${module}` : ""}.</p>
              </div>
              {canView && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  {dashboard.dataUpdatedAt > 0 && (
                    <span>Actualizado {new Date(dashboard.dataUpdatedAt).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit", second: "2-digit" })}</span>
                  )}
                  <Button size="xs" variant="ghost" disabled={fetching} onClick={refreshAll}>
                    <IconRefresh className={fetching ? "animate-spin" : undefined} />
                    <span className="sr-only sm:not-sr-only">Refrescar</span>
                  </Button>
                </div>
              )}
            </header>

            {!canView ? (
              <Alert><IconAlertTriangle /><AlertTitle>Sin acceso</AlertTitle><AlertDescription>Tu rol activo no tiene el permiso {CORE_VIEW_PERMISSION} para consultar la integración con Core.</AlertDescription></Alert>
            ) : <>
              <SectionTitle title="Conexión" description={`Se comprueba cada ${STATUS_REFRESH_MS / 1000} segundos sin publicar ni consumir mensajes.`} />
              <ConnectionPanel status={status.data} pending={status.isPending} error={status.isError ? status.error.message : undefined} />

              <section aria-label="Ventana" className="flex flex-wrap items-end justify-between gap-3 rounded-xl border bg-card p-3">
                <div className="space-y-1.5">
                  <Label id="core-window-label">Ventana de métricas</Label>
                  <ToggleGroup
                    variant="outline"
                    size="sm"
                    aria-labelledby="core-window-label"
                    value={[windowKey]}
                    onValueChange={(value: string[]) => {
                      const next = value[0] as WindowKey | undefined
                      if (next) void navigate({ search: { ventana: next === "24" ? undefined : next }, replace: true })
                    }}
                  >
                    {(Object.keys(WINDOWS) as WindowKey[]).map((key) => <ToggleGroupItem key={key} value={key}>{WINDOWS[key]}</ToggleGroupItem>)}
                  </ToggleGroup>
                </div>
                <p className="max-w-md text-xs text-muted-foreground">
                  La ventana afecta el volumen por hora y los publicados en el período. El resto de los contadores de Core son acumulados.
                </p>
              </section>

              {dashboard.isPending ? <DashboardSkeleton /> : dashboard.isError ? <>
                <Alert variant="destructive">
                  <IconAlertTriangle />
                  <AlertTitle>No se pudieron cargar las métricas de Core</AlertTitle>
                  <AlertDescription className="flex flex-col items-start gap-2">
                    <span>{dashboard.error.message ?? "Intentá nuevamente."} Se muestran solo los contadores locales.</span>
                    <Button size="sm" variant="outline" onClick={() => void dashboard.refetch()}>Reintentar</Button>
                  </AlertDescription>
                </Alert>
                {local && <LocalMetrics local={local} checkedAt={localFallback.dataUpdatedAt} />}
              </> : dashboard.data?.core && (
                <div aria-busy={dashboard.isFetching} className={cn("space-y-5 transition-opacity", dashboard.isPlaceholderData && "opacity-60")}>
                  <CoreSummary core={dashboard.data.core} />
                  <div className="grid gap-4 lg:grid-cols-3">
                    <div className="lg:col-span-2"><HourlyVolumeChart data={dashboard.data.core} /></div>
                    <TopPublishedTypesChart data={dashboard.data.core} />
                  </div>
                  <div className="grid gap-4 lg:grid-cols-2">
                    <ReceivedByStatusChart data={dashboard.data.core} />
                    <DeadLettersChart data={dashboard.data.core} />
                  </div>
                  {local && <LocalMetrics local={local} checkedAt={dashboard.dataUpdatedAt} />}
                </div>
              )}

              <SectionTitle title="Contrato con Core" description="Lo que declaramos publicar y los eventos a los que estamos suscriptos." />
              <div className="grid gap-4 lg:grid-cols-2">
                <ContractCard
                  title="Publicaciones"
                  icon={<IconArrowUpRight className="size-4 text-sky-600 dark:text-sky-400" />}
                  pending={publications.isPending}
                  error={publications.isError ? publications.error.message : undefined}
                  empty="No hay publicaciones declaradas."
                  items={(publications.data ?? []).map((publication) => ({
                    key: publication.id ?? publication.eventType ?? "",
                    title: publication.eventType ?? "—",
                    active: publication.active,
                  }))}
                />
                <ContractCard
                  title="Suscripciones"
                  icon={<IconArrowDownLeft className="size-4 text-violet-600 dark:text-violet-400" />}
                  pending={subscriptions.isPending}
                  error={subscriptions.isError ? subscriptions.error.message : undefined}
                  empty="No hay suscripciones registradas."
                  items={(subscriptions.data ?? []).map((subscription) => ({
                    key: subscription.id ?? subscription.eventType ?? "",
                    title: subscription.eventType ?? "—",
                    detail: `${subscription.queueName ?? "—"} · hasta ${subscription.maxAttempts ?? "?"} intentos`,
                    active: subscription.active,
                  }))}
                />
              </div>

              <CoreEventsTable module={module} />
            </>}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function ConnectionPanel({ status, pending, error }: { status?: CoreConnectionStatusResponse; pending: boolean; error?: string }) {
  if (pending) return <div className="grid gap-4 md:grid-cols-3">{Array.from({ length: 3 }, (_, index) => <Skeleton key={index} className="h-40 rounded-xl" />)}</div>
  if (!status) {
    return <Alert variant="destructive"><IconAlertTriangle /><AlertTitle>No se pudo comprobar la conexión</AlertTitle><AlertDescription>{error ?? "Intentá nuevamente."}</AlertDescription></Alert>
  }
  const readiness = status.readiness ?? {}
  const http = status.http ?? {}
  const amqp = status.amqp ?? {}
  const checks = Object.entries(readiness.checks ?? {})
  return (
    <div className="space-y-3">
      <div className="grid gap-4 md:grid-cols-3">
        <ConnectionCard
          icon={<IconHeartbeat className="size-5" />}
          title="Salud de Core"
          description="GET /health/ready público; el estado sale del cuerpo, no del código HTTP."
          state={readiness.state}
          stateLabel={readiness.state === "UP" ? "Listo" : undefined}
          rows={[
            ...(checks.length > 0
              ? checks.map(([name, value]): [string, string] => [checkLabels[name] ?? name, value])
              : [["Estado remoto", readiness.status ?? "—"] as [string, string]]),
            ["Código", readiness.code ?? (readiness.upstreamStatus != null ? `HTTP ${readiness.upstreamStatus}` : "—")],
          ]}
        />
        <ConnectionCard
          icon={<IconApi className="size-5" />}
          title="API HTTP"
          description="GET /dashboard autenticado con la credencial de máquina."
          state={http.state}
          rows={[
            ["HTTP remoto", http.upstreamStatus != null ? String(http.upstreamStatus) : "—"],
            ["Código", http.code ?? "—"],
            ["Trace", http.traceId ?? "—"],
          ]}
        />
        <ConnectionCard
          icon={<IconServer className="size-5" />}
          title="Broker AMQPS"
          description={<>Consulta pasiva de la cola <span className="font-mono">{amqp.queue ?? "—"}</span>.</>}
          state={amqp.state}
          rows={[
            ["Mensajes en cola", amqp.pendingMessages != null ? formatCount(amqp.pendingMessages) : "—"],
            ["Consumidores activos", amqp.activeConsumers != null ? formatCount(amqp.activeConsumers) : "—"],
            ["Código", amqp.code ?? "—"],
          ]}
        />
      </div>
      <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
        <Flag on={status.enabled} label="Integración" />
        <Flag on={status.machineSecretConfigured} label="Secreto de máquina" />
        <Flag on={status.publisherEnabled} label="Publicador" />
        <Flag on={status.consumerEnabled} label="Consumidor" />
        <span className="ml-auto">Comprobado {formatDateTime(status.checkedAt)}</span>
      </div>
    </div>
  )
}

const checkLabels: Record<string, string> = { database: "Base de datos", broker: "Broker" }

function ConnectionCard({ icon, title, description, state, stateLabel, rows }: {
  icon: ReactNode
  title: string
  description: ReactNode
  state?: ConnectionState
  stateLabel?: string
  rows: Array<[string, string]>
}) {
  const style = state ? stateStyles[state] : undefined
  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">{icon}{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <Badge variant="outline" className={cn("gap-1.5", style?.badge)}>
          <span aria-hidden="true" className={cn("size-2 rounded-full", style?.dot, state === "UP" && "animate-pulse")} />
          {stateLabel ?? (state ? stateLabels[state] : "Desconocido")}
        </Badge>
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          {rows.map(([label, value]) => (
            <div key={label} className="contents">
              <dt className="text-muted-foreground">{label}</dt>
              <dd className="min-w-0 truncate text-right font-mono text-xs leading-5">{value}</dd>
            </div>
          ))}
        </dl>
      </CardContent>
    </Card>
  )
}

function Flag({ on, label }: { on?: boolean; label: string }) {
  return (
    <Badge variant="outline" className={on ? stateStyles.UP.badge : stateStyles.DISABLED.badge}>
      {label}: {on ? "sí" : "no"}
    </Badge>
  )
}

function CoreSummary({ core }: { core: CoreModuleDashboard }) {
  const received = core.received ?? {}
  return (
    <section aria-label="Resumen" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
      <StatTile label="Enviados en la ventana" value={formatCount(core.published?.inWindow)} hint={`${formatCount(core.published?.total)} en total.`} />
      <StatTile label="Recibidos" value={formatCount(received.total)} hint={`${formatCount(received.delivered)} entregados.`} />
      <StatTile label="Reintentos pendientes" value={formatCount(received.pendingRetry)} />
      <StatTile
        label="Fallidos (DLQ)"
        value={formatCount(core.deadLetters?.open)}
        hint={`${formatCount(received.dead)} entregas muertas.`}
        className={(core.deadLetters?.open ?? 0) > 0 ? "border-red-500/40" : undefined}
      />
      <StatTile label="Procesamiento promedio" value={formatMs(core.processing?.avgMs)} hint={`Máximo ${formatMs(core.processing?.maxMs)}.`} />
      <StatTile label="Suscripciones activas" value={`${formatCount(core.subscriptions?.active)} / ${formatCount(core.subscriptions?.total)}`} hint={`${core.publications?.declared?.length ?? 0} publicaciones declaradas.`} />
    </section>
  )
}

function LocalMetrics({ local, checkedAt }: { local: CoreLocalMetrics; checkedAt: number }) {
  const oldest = local.oldestUnconfirmedAt ? new Date(local.oldestUnconfirmedAt) : null
  // Más de 5 minutos sin confirmación del broker al momento de la consulta indica que el publicador está trabado.
  const stale = oldest !== null && checkedAt - oldest.getTime() > 5 * 60_000
  return <>
    <SectionTitle title="Outbox e inbox locales" description="Contadores acumulados de nuestra base. Confirmado por el broker no implica que el destino lo haya procesado." />
    <section aria-label="Contadores locales" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
      <StatTile label="Pendientes de envío" value={formatCount(local.pending)} />
      <StatTile label="En vuelo" value={formatCount(local.inFlight)} />
      <StatTile label="Confirmados por el broker" value={formatCount(local.brokerConfirmed)} />
      <StatTile label="Recibidos procesados" value={formatCount(local.inboxProcessed)} />
      <StatTile
        label="Más antiguo sin confirmar"
        value={oldest ? oldest.toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" }) : "—"}
        hint={oldest ? formatDateTime(local.oldestUnconfirmedAt) : "Nada pendiente."}
        className={stale ? "border-amber-500/50" : undefined}
      />
    </section>
  </>
}

function ContractCard({ title, icon, pending, error, empty, items }: {
  title: string
  icon: ReactNode
  pending: boolean
  error?: string
  empty: string
  items: Array<{ key: string; title: string; detail?: string; active?: boolean }>
}) {
  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">{icon}{title}</CardTitle>
        <CardDescription>{pending ? "Cargando…" : `${items.filter((item) => item.active).length} activas de ${items.length}.`}</CardDescription>
      </CardHeader>
      <CardContent>
        {pending ? <Skeleton className="h-32 rounded-lg" /> : error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : items.length === 0 ? <p className="text-sm text-muted-foreground">{empty}</p> : (
          <ul className="max-h-72 divide-y overflow-y-auto rounded-lg border">
            {items.map((item) => (
              <li key={item.key} className="flex items-center justify-between gap-3 px-3 py-2">
                <div className="min-w-0">
                  <p className="truncate font-mono text-sm">{item.title}</p>
                  {item.detail && <p className="truncate text-xs text-muted-foreground">{item.detail}</p>}
                </div>
                <Badge variant="outline" className={item.active ? stateStyles.UP.badge : stateStyles.DISABLED.badge}>
                  {item.active ? "Activa" : "Inactiva"}
                </Badge>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-4" role="status" aria-label="Cargando métricas">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">{Array.from({ length: 6 }, (_, index) => <Skeleton key={index} className="h-28 rounded-xl" />)}</div>
      <div className="grid gap-4 lg:grid-cols-3"><Skeleton className="h-80 rounded-xl lg:col-span-2" /><Skeleton className="h-80 rounded-xl" /></div>
      <span className="sr-only">Cargando métricas…</span>
    </div>
  )
}

function formatMs(value?: number) {
  if (value == null) return "—"
  return value >= 1000 ? `${(value / 1000).toLocaleString("es-AR", { maximumFractionDigits: 1 })} s` : `${Math.round(value)} ms`
}
