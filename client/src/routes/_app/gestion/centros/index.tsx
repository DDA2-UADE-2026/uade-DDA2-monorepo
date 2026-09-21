import { IconAlertTriangle, IconMail, IconMapPin, IconPencil, IconPhone, IconPlus, IconPower, IconRefresh, IconSearch } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { useState } from "react"

import { CenterDialog } from "@/components/centros/CenterDialog"
import { ESTADO_LABELS, centerSearchSchema, toCenterListQuery } from "@/components/centros/centerFilters"
import { DataPagination } from "@/components/DataPagination"
import { OutletNavRightButton, OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  activateMunicipalCenterMutation,
  deactivateMunicipalCenterMutation,
  listMunicipalCentersOptions,
  listMunicipalCentersQueryKey,
} from "@/generated/@tanstack/react-query.gen"
import type { MunicipalCenterResponse } from "@/generated/types.gen"

export const Route = createFileRoute("/_app/gestion/centros/")({
  validateSearch: centerSearchSchema,
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const queryClient = useQueryClient()
  const [dialog, setDialog] = useState<{ center: MunicipalCenterResponse | null } | null>(null)
  const [statusChange, setStatusChange] = useState<MunicipalCenterResponse | null>(null)
  const [searchInput, setSearchInput] = useState(search.search)

  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    listMunicipalCentersOptions({ query: toCenterListQuery(search) }),
  )
  const centers = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(search.page, totalPages)
  const setPage = (page: number) => navigate({ search: { ...search, page } })
  const applySearch = () => navigate({ search: { ...search, search: searchInput.trim(), page: 1 } })
  const setEstado = (estado: "todos" | "activos" | "inactivos") =>
    navigate({ search: { ...search, estado, page: 1 } })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: listMunicipalCentersQueryKey() })
  const activate = useMutation({ ...activateMunicipalCenterMutation(), onSuccess: () => { invalidate(); setStatusChange(null) } })
  const deactivate = useMutation({ ...deactivateMunicipalCenterMutation(), onSuccess: () => { invalidate(); setStatusChange(null) } })
  const statusPending = activate.isPending || deactivate.isPending
  const statusError = activate.error ?? deactivate.error

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Centros" }]} />
        <OutletNavRightButton className="gap-1.5">
          <Link to="/gestion/centros/servicios">
            <Button size="sm" variant="outline">
              Catálogo de servicios
            </Button>
          </Link>
          <Button size="sm" onClick={() => setDialog({ center: null })}>
            <IconPlus />
            Nuevo centro
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
            <div className="mb-3 flex flex-col gap-2 sm:flex-row">
              <InputGroup className="sm:max-w-sm">
                <InputGroupAddon><IconSearch /></InputGroupAddon>
                <InputGroupInput
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  onKeyDown={(event) => { if (event.key === "Enter") applySearch() }}
                  placeholder="Buscar por nombre o dirección…"
                  aria-label="Buscar centros"
                />
              </InputGroup>
              <div className="flex gap-2">
                <Select value={search.estado} onValueChange={(value) => setEstado(value as "todos" | "activos" | "inactivos")}>
                  <SelectTrigger className="w-36" aria-label="Filtrar por estado">
                    <SelectValue>
                      {(value: string | null) => (value ? ESTADO_LABELS[value as keyof typeof ESTADO_LABELS] : "Todos")}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="todos">Todos</SelectItem>
                    <SelectItem value="activos">Activos</SelectItem>
                    <SelectItem value="inactivos">Inactivos</SelectItem>
                  </SelectContent>
                </Select>
                <Button size="sm" variant="outline" onClick={applySearch}>Buscar</Button>
              </div>
            </div>
            {isPending ? (
              <p className="text-sm text-muted-foreground">Cargando centros…</p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los centros.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>Reintentar</Button>
              </div>
            ) : centers.length === 0 ? (
              <p className="text-sm text-muted-foreground">No hay centros registrados con ese criterio.</p>
            ) : (
              <Table>
                <TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Dirección</TableHead><TableHead>Contacto</TableHead><TableHead>Estado</TableHead><TableHead>Actualizado</TableHead><TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead></TableRow></TableHeader>
                <TableBody>
                  {centers.map((center) => (
                    <TableRow key={center.id}>
                      <TableCell className="font-medium">
                        <Link to="/gestion/centros/$centroId" params={{ centroId: center.id ?? "" }} className="hover:underline">
                          {center.name}
                        </Link>
                      </TableCell>
                      <TableCell className="max-w-xs whitespace-normal text-muted-foreground">
                        <span className="flex items-start gap-1.5"><IconMapPin className="mt-0.5 size-3.5 shrink-0" />{center.address}</span>
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {center.phone && <span className="flex items-center gap-1.5"><IconPhone className="size-3.5 shrink-0" />{center.phone}</span>}
                        {center.email && <span className="flex items-center gap-1.5"><IconMail className="size-3.5 shrink-0" />{center.email}</span>}
                        {!center.phone && !center.email && "—"}
                      </TableCell>
                      <TableCell>
                        <span className="flex items-center gap-1.5 text-xs">
                          <span className={`size-2 shrink-0 rounded-full ${center.active ? "bg-emerald-500" : "bg-muted-foreground"}`} />
                          {center.active ? "Activo" : "Inactivo"}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{center.updatedAt ? new Date(center.updatedAt).toLocaleDateString("es-AR") : "—"}</TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-1">
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="ghost"
                            aria-label={`Editar centro ${center.name ?? ""}`}
                            onClick={() => setDialog({ center })}
                          >
                            <IconPencil />
                          </Button>
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="ghost"
                            aria-label={`${center.active ? "Desactivar" : "Activar"} centro ${center.name ?? ""}`}
                            onClick={() => { activate.reset(); deactivate.reset(); setStatusChange(center) }}
                          >
                            <IconPower />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>
        {!isPending && !isError && totalItems > 0 && (
          <DataPagination page={currentPage} totalPages={totalPages} totalItems={totalItems} pageSize={10} onPageChange={setPage} />
        )}
      </SidebarShellContent>
      {dialog && (
        <CenterDialog
          center={dialog.center}
          onOpenChange={(open) => { if (!open) setDialog(null) }}
        />
      )}
      <AlertDialog open={statusChange !== null} onOpenChange={(open) => { if (!open && !statusPending) setStatusChange(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {statusChange?.active ? "Desactivar centro" : "Activar centro"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {statusChange?.active
                ? `«${statusChange?.name}» dejará de formar parte de la oferta activa, pero se conservan sus servicios, horarios y asignaciones.`
                : `Se volverá a validar que las disponibilidades que «${statusChange?.name}» volvería efectivas tengan cobertura y no se superpongan. Si alguna falla, toda la activación se rechaza.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          {statusError && (
            <Alert variant="destructive">
              <IconAlertTriangle />
              <AlertTitle>No se pudo cambiar el estado</AlertTitle>
              <AlertDescription>{statusError.message ?? "Revisá el estado de los servicios y agendas e intentá nuevamente."}</AlertDescription>
            </Alert>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={statusPending}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              disabled={statusPending || !statusChange?.id}
              onClick={(event) => {
                event.preventDefault()
                if (!statusChange?.id) return
                if (statusChange.active) {
                  deactivate.mutate({ path: { id: statusChange.id } })
                } else {
                  activate.mutate({ path: { id: statusChange.id } })
                }
              }}
            >
              {statusPending ? "Guardando…" : statusChange?.active ? "Desactivar" : "Activar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </SidebarShell>
  )
}
