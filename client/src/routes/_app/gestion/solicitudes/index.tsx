import {
  IconFileDescription,
  IconFilePlus,
  IconRefresh,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { AdminApplicationsTable } from "@/components/applications/AdminApplicationsTable"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { list4Options } from "@/generated/@tanstack/react-query.gen"

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
              <AdminApplicationsTable applications={applications} />
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
