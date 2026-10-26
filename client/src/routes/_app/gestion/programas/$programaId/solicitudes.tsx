import { IconFileDescription, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { AdminApplicationsTable } from "@/components/applications/AdminApplicationsTable"
import { LoadingOrError } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import {
  Empty,
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

export const Route = createFileRoute("/_app/gestion/programas/$programaId/solicitudes")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    list4Options({ query: { programId: programaId, page: page - 1, size: PAGE_SIZE } }),
  )

  const applications = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)

  return (
    <div className="flex min-h-full flex-col">
      <div className="mx-2 flex-1 py-2 sm:mx-4! lg:py-4">
        {dataUpdatedAt > 0 && (
          <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
            <span>Última actualización: {new Date(dataUpdatedAt).toLocaleTimeString("es-AR")}</span>
            <Button size="xs" variant="ghost" onClick={() => refetch()} disabled={isFetching}>
              <IconRefresh className={isFetching ? "animate-spin" : undefined} />
              Actualizar
            </Button>
          </div>
        )}

        <LoadingOrError pending={isPending} error={isError} retry={() => refetch()} />
        {!isPending && !isError && (applications.length === 0 ? (
          <Empty className="min-h-72 border">
            <EmptyHeader>
              <EmptyMedia variant="icon"><IconFileDescription /></EmptyMedia>
              <EmptyTitle>Este programa no tiene solicitudes</EmptyTitle>
              <EmptyDescription>
                Las solicitudes presentadas a cualquiera de sus ediciones van a aparecer acá.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <AdminApplicationsTable applications={applications} hideProgram />
        ))}
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
    </div>
  )
}
