import { IconArrowDownLeft, IconArrowUpRight } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import type { ReactNode } from "react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { listCorePublicationsOptions, listCoreSubscriptionsOptions } from "@/generated/@tanstack/react-query.gen"

const ACTIVE_BADGE = "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
const INACTIVE_BADGE = "border-zinc-500/30 bg-zinc-500/10 text-zinc-700 dark:text-zinc-300"

type ContractItem = { key: string; title: string; detail?: string; active?: boolean }

/** Eventos async que declaramos publicar y a los que estamos suscriptos en Core. */
export function CoreContract() {
  const publications = useQuery(listCorePublicationsOptions())
  const subscriptions = useQuery(listCoreSubscriptionsOptions())

  return (
    <div className="flex flex-col gap-4">
      <ContractCard
        title="Publicaciones"
        description="Eventos que nuestro módulo publica en Core."
        icon={<IconArrowUpRight className="size-4 text-sky-600 dark:text-sky-400" />}
        pending={publications.isPending}
        error={publications.isError ? publications.error.message ?? "Core no respondió." : undefined}
        onRetry={() => void publications.refetch()}
        empty="No hay publicaciones declaradas."
        items={(publications.data ?? []).map((publication) => ({
          key: publication.id ?? publication.eventType ?? "",
          title: publication.eventType ?? "—",
          active: publication.active,
        }))}
      />
      <ContractCard
        title="Suscripciones"
        description="Eventos de otros módulos que Core nos entrega."
        icon={<IconArrowDownLeft className="size-4 text-violet-600 dark:text-violet-400" />}
        pending={subscriptions.isPending}
        error={subscriptions.isError ? subscriptions.error.message ?? "Core no respondió." : undefined}
        onRetry={() => void subscriptions.refetch()}
        empty="No hay suscripciones registradas."
        items={(subscriptions.data ?? []).map((subscription) => ({
          key: subscription.id ?? subscription.eventType ?? "",
          title: subscription.eventType ?? "—",
          detail: `${subscription.queueName ?? "—"} · hasta ${subscription.maxAttempts ?? "?"} intentos`,
          active: subscription.active,
        }))}
      />
    </div>
  )
}

function ContractCard({ title, description, icon, pending, error, onRetry, empty, items }: {
  title: string
  description: string
  icon: ReactNode
  pending: boolean
  error?: string
  onRetry: () => void
  empty: string
  items: ContractItem[]
}) {
  const sorted = [...items].sort((a, b) => a.title.localeCompare(b.title))
  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">{icon}{title}</CardTitle>
        <CardDescription>
          {description} {!pending && !error && `${items.filter((item) => item.active).length} activas de ${items.length}.`}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {pending ? <Skeleton className="h-32 rounded-lg" /> : error ? (
          <div className="flex flex-col items-start gap-2 text-sm">
            <p className="text-destructive">{error}</p>
            <Button size="sm" variant="outline" onClick={onRetry}>Reintentar</Button>
          </div>
        ) : sorted.length === 0 ? <p className="text-sm text-muted-foreground">{empty}</p> : (
          <ul className="divide-y rounded-lg border">
            {sorted.map((item) => (
              <li key={item.key} className="flex items-center justify-between gap-3 px-3 py-2">
                <div className="min-w-0">
                  <p className="font-mono text-sm break-all">{item.title}</p>
                  {item.detail && <p className="truncate text-xs text-muted-foreground">{item.detail}</p>}
                </div>
                <Badge variant="outline" className={item.active ? ACTIVE_BADGE : INACTIVE_BADGE}>
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
