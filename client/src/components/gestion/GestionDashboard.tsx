import type { ComponentType, ReactNode } from "react"
import { IconArrowUpRight, IconBuildingCommunity, IconFileDescription, IconFilePlus, IconMapPin, IconRefresh, IconSpeakerphone, IconTarget, IconUsers } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"

import { ActivityStatusBadge, formatActivityDateRange } from "@/components/activities/activity-ui"
import { UserAvatar } from "@/components/UserAvatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { findAllOptions, list1Options, list4Options, list5Options, listMunicipalCentersOptions } from "@/generated/@tanstack/react-query.gen"
import { useMe } from "@/hooks/use-auth"
import { applicationStatusLabels, formatApplicationDate, isApplicationResolved } from "@/lib/application-flow"
import { programImageSource } from "@/lib/program-images"

const previewQuery = { query: { page: 0, size: 5 } }
const queryDefaults = { staleTime: 60_000, retry: false } as const
const numberFormat = new Intl.NumberFormat("es-AR")

type QueryState = {
  isPending: boolean
  isError: boolean
  isFetching: boolean
  refetch: () => unknown
}

function Metric({ label, value, description, icon: Icon, query, link }: {
  label: string
  value: number | undefined
  description: string
  icon: ComponentType<{ className?: string }>
  query: QueryState
  link: ReactNode
}) {
  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm text-muted-foreground"><Icon className="size-4" />{label}</CardTitle>
        <CardAction>{link}</CardAction>
      </CardHeader>
      <CardContent className="space-y-1">
        {query.isPending ? <Skeleton className="h-9 w-20" aria-label={`Cargando ${label.toLowerCase()}`} /> : (
          <p className="font-heading text-3xl font-semibold tabular-nums">{query.isError || value === undefined ? "—" : numberFormat.format(value)}</p>
        )}
        <p className="text-xs text-muted-foreground">{query.isError ? "No disponible por el momento" : description}</p>
      </CardContent>
    </Card>
  )
}

function DashboardPanel({ title, description, query, empty, children, link, className }: {
  title: string
  description: string
  query: QueryState
  empty: boolean
  children: ReactNode
  link: ReactNode
  className?: string
}) {
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle><h2>{title}</h2></CardTitle>
        <CardDescription>{description}</CardDescription>
        <CardAction>{link}</CardAction>
      </CardHeader>
      <CardContent>
        {query.isPending ? (
          <div className="space-y-4" role="status" aria-label={`Cargando ${title.toLowerCase()}`}>
            {[0, 1, 2].map((key) => <Skeleton key={key} className="h-12 w-full" />)}
          </div>
        ) : query.isError ? (
          <div role="alert" className="flex min-h-28 flex-col items-start justify-center gap-3">
            <p className="text-sm text-muted-foreground">No se pudo cargar esta sección.</p>
            <Button size="sm" variant="outline" onClick={() => query.refetch()} disabled={query.isFetching}><IconRefresh />Reintentar</Button>
          </div>
        ) : empty ? (
          <p className="flex min-h-28 items-center text-sm text-muted-foreground">Todavía no hay registros para mostrar.</p>
        ) : children}
      </CardContent>
    </Card>
  )
}

function ActiveBadge({ active }: { active?: boolean }) {
  return <Badge variant={active ? "secondary" : "outline"}>{active === undefined ? "Sin estado" : active ? "Activo" : "Inactivo"}</Badge>
}

export function GestionDashboard() {
  const { data } = useMe()
  const user = data?.user
  const displayName = user?.name || user?.username || "Usuario"
  const permissions = user?.permissions ?? []
  const isAdmin = user?.activeRole?.toUpperCase() === "ADMIN"
  const canViewUsers = permissions.includes("users:view")
  const canViewPrograms = permissions.includes("programs:management:view")
  const canViewApplications = permissions.includes("applications:management:view")
  const canViewActivities = isAdmin && permissions.includes("activities:management:view")
  const canViewCenters = isAdmin && permissions.includes("centers:management:view")
  const canCreateApplication = permissions.includes("applications:management:create")

  const users = useQuery({ ...findAllOptions(), ...queryDefaults, enabled: canViewUsers })
  const programs = useQuery({ ...list1Options(previewQuery), ...queryDefaults, enabled: canViewPrograms })
  const applications = useQuery({ ...list4Options(previewQuery), ...queryDefaults, enabled: canViewApplications })
  const activities = useQuery({ ...list5Options(previewQuery), ...queryDefaults, enabled: canViewActivities })
  const centers = useQuery({ ...listMunicipalCentersOptions(previewQuery), ...queryDefaults, enabled: canViewCenters })

  const visibleQueries = [
    ...(canViewUsers ? [users] : []),
    ...(canViewPrograms ? [programs] : []),
    ...(canViewApplications ? [applications] : []),
    ...(canViewActivities ? [activities] : []),
    ...(canViewCenters ? [centers] : []),
  ]
  const isFetching = visibleQueries.some((query) => query.isFetching)
  const recentUsers = [...(users.data ?? [])].sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? "")).slice(0, 5)
  const programItems = programs.data?.content ?? []
  const applicationItems = applications.data?.content ?? []
  const activityItems = activities.data?.content ?? []
  const centerItems = centers.data?.content ?? []

  const usersLink = <Button size="icon-sm" variant="ghost" aria-label="Ver usuarios" render={<Link to="/gestion/usuarios" />}><IconArrowUpRight /></Button>
  const programsLink = <Button size="icon-sm" variant="ghost" aria-label="Ver programas" render={<Link to="/gestion/programas" search={{ page: 1 }} />}><IconArrowUpRight /></Button>
  const applicationsLink = <Button size="icon-sm" variant="ghost" aria-label="Ver solicitudes" render={<Link to="/gestion/solicitudes" search={{ page: 1 }} />}><IconArrowUpRight /></Button>
  const activitiesLink = <Button size="icon-sm" variant="ghost" aria-label="Ver actividades" render={<Link to="/gestion/actividades" search={{ page: 1 }} />}><IconArrowUpRight /></Button>
  const centersLink = <Button size="icon-sm" variant="ghost" aria-label="Ver centros" render={<Link to="/gestion/centros" search={{ page: 1, search: "", estado: "todos" }} />}><IconArrowUpRight /></Button>

  return (
    <main className="flex w-full flex-col gap-6 p-4 lg:p-6">
      <header className="flex flex-col justify-between gap-4 py-2 lg:flex-row lg:items-center">
        <div className="flex min-w-0 items-center gap-4">
          <UserAvatar user={user ?? { name: displayName }} className="size-12 shrink-0 sm:size-14" fallbackClassName="text-base" />
          <div className="min-w-0 space-y-1.5">
            <h1 className="truncate font-heading text-xl font-medium sm:text-2xl">Hola, {displayName}</h1>
            <p className="text-sm text-muted-foreground">Un vistazo a la gestión social del municipio.</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {visibleQueries.length > 0 && (
            <Button variant="outline" size="sm" disabled={isFetching} onClick={() => { void Promise.allSettled(visibleQueries.map((query) => query.refetch())) }}>
              <IconRefresh className={isFetching ? "animate-spin" : undefined} />{isFetching ? "Actualizando…" : "Actualizar resumen"}
            </Button>
          )}
          {canCreateApplication && <Button size="sm" render={<Link to="/gestion/solicitudes/asistida" />}><IconFilePlus />Solicitud asistida</Button>}
        </div>
      </header>

      {visibleQueries.length === 0 ? (
        <Card><CardHeader>
          <CardTitle>Tu espacio de gestión</CardTitle>
          <CardDescription>Usá el menú lateral para acceder a las herramientas de tu rol. No hay resúmenes habilitados para tu rol actual.</CardDescription>
        </CardHeader></Card>
      ) : (
        <>
          <section aria-label="Resumen de la gestión" className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,10rem),1fr))] gap-4">
            {canViewUsers && <Metric label="Usuarios" value={users.data?.length} description="Cuentas registradas" icon={IconUsers} query={users} link={usersLink} />}
            {canViewPrograms && <Metric label="Programas" value={programs.data?.totalElements} description="Programas en el catálogo" icon={IconTarget} query={programs} link={programsLink} />}
            {canViewApplications && <Metric label="Solicitudes" value={applications.data?.totalElements} description="Solicitudes registradas" icon={IconFileDescription} query={applications} link={applicationsLink} />}
            {canViewActivities && <Metric label="Actividades" value={activities.data?.totalElements} description="Actividades comunitarias" icon={IconSpeakerphone} query={activities} link={activitiesLink} />}
            {canViewCenters && <Metric label="Centros" value={centers.data?.totalElements} description="Centros municipales" icon={IconBuildingCommunity} query={centers} link={centersLink} />}
          </section>

          <div className="grid min-w-0 grid-cols-1 items-start gap-6 xl:grid-cols-2">
            {canViewApplications && (
              <DashboardPanel title="Últimas solicitudes" description="Las últimas cinco solicitudes registradas y su estado." query={applications} empty={applicationItems.length === 0} link={applicationsLink} className="min-w-0">
                <Table>
                  <TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Estado</TableHead><TableHead>Fecha</TableHead></TableRow></TableHeader>
                  <TableBody>
                    {applicationItems.map((application) => (
                      <TableRow key={application.id ?? application.applicationNumber}>
                        <TableCell className="max-w-64 whitespace-normal">
                          <Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: application.id ?? "" }} className="font-medium hover:underline">#{application.applicationNumber ?? "—"} · {application.userName || "Sin titular"}</Link>
                          <p className="mt-1 text-xs text-muted-foreground">{application.programName || "Sin programa"}</p>
                        </TableCell>
                        <TableCell><Badge variant={isApplicationResolved(application.status) ? "secondary" : "outline"}>{application.status ? applicationStatusLabels[application.status] : "Sin estado"}</Badge></TableCell>
                        <TableCell className="text-xs text-muted-foreground">{formatApplicationDate(application.createdAt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </DashboardPanel>
            )}

            {canViewPrograms && (
              <DashboardPanel title="Programas sociales" description="Una selección del catálogo y sus objetivos." query={programs} empty={programItems.length === 0} link={programsLink}>
                <ul className="divide-y">
                  {programItems.map((program) => (
                    <li key={program.id} className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
                      <div className="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-muted text-muted-foreground">
                        {program.imageUrl ? <img src={programImageSource(program.imageUrl)} alt="" loading="lazy" className="size-full object-cover" /> : <IconTarget className="size-5" />}
                      </div>
                      <div className="min-w-0 flex-1 space-y-1">
                        <Link to="/gestion/programas/$programaId" params={{ programaId: program.id ?? "" }} className="font-medium hover:underline">{program.name || "Sin nombre"}</Link>
                        <p className="line-clamp-2 text-xs text-muted-foreground">{program.objective || "Sin objetivo registrado."}</p>
                      </div>
                      <ActiveBadge active={program.active} />
                    </li>
                  ))}
                </ul>
              </DashboardPanel>
            )}

            {canViewUsers && (
              <DashboardPanel title="Usuarios recientes" description="Las últimas cuentas incorporadas al municipio." query={users} empty={recentUsers.length === 0} link={usersLink}>
                <ul className="divide-y">
                  {recentUsers.map((account) => (
                    <li key={account.id ?? account.username} className="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
                      <UserAvatar user={account} size="sm" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-medium">{account.name || account.username || "Sin nombre"}</p>
                        <p className="truncate text-xs text-muted-foreground">{account.roles?.join(" · ") || "Sin roles asignados"}</p>
                      </div>
                      <ActiveBadge active={account.active} />
                    </li>
                  ))}
                </ul>
                <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1 border-t pt-3 text-xs text-muted-foreground">
                  <span>Activas: {numberFormat.format(users.data?.filter((account) => account.active === true).length ?? 0)}</span>
                  <span>Inactivas: {numberFormat.format(users.data?.filter((account) => account.active === false).length ?? 0)}</span>
                </div>
              </DashboardPanel>
            )}

            {canViewActivities && (
              <DashboardPanel title="Actividades comunitarias" description="Fechas, lugares y estado de las actividades registradas." query={activities} empty={activityItems.length === 0} link={activitiesLink}>
                <ul className="divide-y">
                  {activityItems.map((activity) => (
                    <li key={activity.id} className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
                      <div className="min-w-0 space-y-1">
                        <Link to="/gestion/actividades/$actividadId" params={{ actividadId: activity.id ?? "" }} className="font-medium hover:underline">{activity.name || "Sin nombre"}</Link>
                        <p className="text-xs text-muted-foreground">{formatActivityDateRange(activity.startDate, activity.endDate)}</p>
                        <p className="flex items-center gap-1 text-xs text-muted-foreground"><IconMapPin className="size-3.5 shrink-0" />{activity.location || "Sin ubicación"}</p>
                      </div>
                      <ActivityStatusBadge status={activity.status} />
                    </li>
                  ))}
                </ul>
              </DashboardPanel>
            )}

            {canViewCenters && (
              <DashboardPanel title="Centros municipales" description="Espacios de atención y su estado." query={centers} empty={centerItems.length === 0} link={centersLink}>
                <ul className="divide-y">
                  {centerItems.map((center) => (
                    <li key={center.id} className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground"><IconBuildingCommunity className="size-5" /></div>
                      <div className="min-w-0 flex-1 space-y-1">
                        <Link to="/gestion/centros/$centroId" params={{ centroId: center.id ?? "" }} className="font-medium hover:underline">{center.name || "Sin nombre"}</Link>
                        <p className="text-xs text-muted-foreground">{center.address || "Sin dirección registrada"}</p>
                      </div>
                      <ActiveBadge active={center.active} />
                    </li>
                  ))}
                </ul>
              </DashboardPanel>
            )}
          </div>
        </>
      )}
    </main>
  )
}
