import { IconBuilding, IconCalendarEvent, IconCheck, IconSearch } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { useState } from "react"

import { DataPagination } from "@/components/DataPagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listCenterServicesOptions, listMunicipalCentersOptions } from "@/generated/@tanstack/react-query.gen"
import type { MunicipalCenterResponse } from "@/generated/types.gen"
import { isUuid } from "@/lib/appointment-flow"

const PAGE_SIZE = 8

type PickerProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelect: (id: string) => void
}

export function AppointmentCenterSelectionDialog({ open, onOpenChange, onSelect, selectedCenterId, description = "Buscá un centro activo para consultar su calendario de turnos." }: Omit<PickerProps, "onSelect"> & { selectedCenterId: string; onSelect: (id: string, center: MunicipalCenterResponse) => void; description?: string }) {
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(1)
  const query = useQuery({
    ...listMunicipalCentersOptions({ query: { active: true, page: page - 1, size: PAGE_SIZE, ...(search.trim() ? { search: search.trim() } : {}) } }),
    enabled: open,
  })
  const centers = query.data?.content ?? []
  const totalItems = query.data?.totalElements ?? 0
  const totalPages = Math.max(1, query.data?.totalPages ?? 1)

  const select = (center: MunicipalCenterResponse) => {
    if (!center.id) return
    onSelect(center.id, center)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100vh-2rem)] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>Seleccionar centro</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <InputGroup>
          <InputGroupAddon><IconSearch /></InputGroupAddon>
          <InputGroupInput
            value={search}
            onChange={(event) => { setSearch(event.target.value); setPage(1) }}
            placeholder="Buscar centro por nombre"
            aria-label="Buscar centro por nombre"
            autoFocus
          />
        </InputGroup>
        {query.isPending ? (
          <p className="text-sm text-muted-foreground">Cargando centros…</p>
        ) : query.isError ? (
          <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
            <p>No se pudieron cargar los centros.</p>
            <Button size="sm" variant="outline" onClick={() => query.refetch()}>Reintentar</Button>
          </div>
        ) : centers.length === 0 ? (
          <Empty className="min-h-48 border">
            <EmptyHeader>
              <EmptyMedia variant="icon"><IconBuilding /></EmptyMedia>
              <EmptyTitle>Sin resultados</EmptyTitle>
              <EmptyDescription>No hay centros activos que coincidan con la búsqueda.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="max-h-[48vh] overflow-auto">
            <Table className="min-w-[32rem]">
              <TableHeader><TableRow><TableHead>Centro</TableHead><TableHead>Dirección</TableHead><TableHead className="w-28"><span className="sr-only">Seleccionar</span></TableHead></TableRow></TableHeader>
              <TableBody>
                {centers.map((center) => {
                  const selected = center.id === selectedCenterId
                  return <TableRow key={center.id}>
                    <TableCell className="font-medium">{center.name || "Centro sin nombre"}</TableCell>
                    <TableCell className="text-muted-foreground">{center.address || "—"}</TableCell>
                    <TableCell className="text-right">
                      <Button type="button" size="sm" variant={selected ? "secondary" : "outline"} disabled={!center.id || !center.name} onClick={() => select(center)}>
                        {selected && <IconCheck />}{selected ? "Seleccionado" : "Seleccionar"}
                      </Button>
                    </TableCell>
                  </TableRow>
                })}
              </TableBody>
            </Table>
          </div>
        )}
        {!query.isPending && !query.isError && totalItems > 0 && (
          <DataPagination page={page} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} className="border-t-0 px-0 py-0" />
        )}
      </DialogContent>
    </Dialog>
  )
}

export function AppointmentServiceSelectionDialog({ open, onOpenChange, onSelect, centerId, selectedServiceId }: PickerProps & { centerId: string; selectedServiceId: string }) {
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(1)
  const query = useQuery({
    ...listCenterServicesOptions({ path: { centerId } }),
    enabled: open && isUuid(centerId),
  })
  const normalizedSearch = search.trim().toLocaleLowerCase("es-AR")
  const services = (query.data ?? []).filter((item) =>
    !normalizedSearch || [item.service?.name, item.service?.description].some((value) => value?.toLocaleLowerCase("es-AR").includes(normalizedSearch)),
  )
  const totalItems = services.length
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = services.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const select = (id: string) => {
    onSelect(id)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100vh-2rem)] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>Seleccionar servicio</DialogTitle>
          <DialogDescription>Filtrá la agenda por un servicio del centro o consultá todos.</DialogDescription>
        </DialogHeader>
        <InputGroup>
          <InputGroupAddon><IconSearch /></InputGroupAddon>
          <InputGroupInput
            value={search}
            onChange={(event) => { setSearch(event.target.value); setPage(1) }}
            placeholder="Buscar servicio"
            aria-label="Buscar servicio"
            autoFocus
          />
        </InputGroup>
        <Button type="button" variant={selectedServiceId ? "outline" : "secondary"} className="w-fit" onClick={() => select("")}>
          <IconCalendarEvent />Todos los servicios
        </Button>
        {query.isPending ? (
          <p className="text-sm text-muted-foreground">Cargando servicios…</p>
        ) : query.isError ? (
          <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
            <p>No se pudieron cargar los servicios.</p>
            <Button size="sm" variant="outline" onClick={() => query.refetch()}>Reintentar</Button>
          </div>
        ) : totalItems === 0 ? (
          <Empty className="min-h-48 border">
            <EmptyHeader>
              <EmptyMedia variant="icon"><IconCalendarEvent /></EmptyMedia>
              <EmptyTitle>Sin resultados</EmptyTitle>
              <EmptyDescription>No hay servicios que coincidan con la búsqueda.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="max-h-[44vh] overflow-auto">
            <Table className="min-w-[32rem]">
              <TableHeader><TableRow><TableHead>Servicio</TableHead><TableHead>Estado</TableHead><TableHead className="w-28"><span className="sr-only">Seleccionar</span></TableHead></TableRow></TableHeader>
              <TableBody>
                {pageItems.map((item) => {
                  const id = item.service?.id
                  const selected = id === selectedServiceId
                  return <TableRow key={item.id ?? id}>
                    <TableCell>
                      <p className="font-medium">{item.service?.name || "Servicio sin nombre"}</p>
                      {item.service?.description && <p className="text-xs text-muted-foreground">{item.service.description}</p>}
                    </TableCell>
                    <TableCell><Badge variant={item.active ? "outline" : "secondary"}>{item.active ? "Activo" : "Inactivo"}</Badge></TableCell>
                    <TableCell className="text-right">
                      <Button type="button" size="sm" variant={selected ? "secondary" : "outline"} disabled={!id} onClick={() => id && select(id)}>
                        {selected && <IconCheck />}{selected ? "Seleccionado" : "Seleccionar"}
                      </Button>
                    </TableCell>
                  </TableRow>
                })}
              </TableBody>
            </Table>
          </div>
        )}
        {!query.isPending && !query.isError && totalItems > 0 && (
          <DataPagination page={currentPage} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} className="border-t-0 px-0 py-0" />
        )}
      </DialogContent>
    </Dialog>
  )
}
