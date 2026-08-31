import { IconPlus, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { OutletNavContent, OutletNavRightButton, OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listOptions } from "@/generated/@tanstack/react-query.gen"

const PAGE_SIZE = 10
const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
})

export const Route = createFileRoute("/_app/gestion/programas/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    listOptions({ query: { page: page - 1, size: PAGE_SIZE } }),
  )
  const programs = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)
  const setPage = (nextPage: number) => navigate({ search: { page: nextPage } })

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavContent>Programas</OutletNavContent>
        <OutletNavRightButton className="gap-1.5">
          <Button size="sm" render={<Link to="/gestion/programas/nuevo" />}>
            <IconPlus />
            Nuevo programa
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
              <p className="text-sm text-muted-foreground">Cargando programas…</p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los programas.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>Reintentar</Button>
              </div>
            ) : programs.length === 0 ? (
              <p className="text-sm text-muted-foreground">No hay programas registrados.</p>
            ) : (
              <Table>
                <TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Objetivo</TableHead><TableHead>Creado</TableHead><TableHead>Actualizado</TableHead></TableRow></TableHeader>
                <TableBody>
                  {programs.map((program) => (
                    <TableRow key={program.id}>
                      <TableCell className="font-medium">
                        <Link to="/gestion/programas/$programaId" params={{ programaId: program.id ?? "" }} className="hover:underline">{program.name}</Link>
                      </TableCell>
                      <TableCell className="max-w-xl whitespace-normal text-muted-foreground">{program.objective || "—"}</TableCell>
                      <TableCell className="text-muted-foreground">{program.createdAt ? new Date(program.createdAt).toLocaleDateString("es-AR") : "—"}</TableCell>
                      <TableCell className="text-muted-foreground">{program.updatedAt ? new Date(program.updatedAt).toLocaleDateString("es-AR") : "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>
        {!isPending && !isError && totalItems > 0 && (
          <DataPagination page={currentPage} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} />
        )}
      </SidebarShellContent>
    </SidebarShell>
  )
}
