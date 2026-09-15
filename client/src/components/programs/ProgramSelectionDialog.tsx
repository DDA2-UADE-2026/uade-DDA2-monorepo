import { IconCheck, IconHeartHandshake, IconSearch } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { useState } from "react"

import { DataPagination } from "@/components/DataPagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { listAvailableProgramsOptions } from "@/generated/@tanstack/react-query.gen"
import type { AvailableProgramListItemResponse } from "@/generated/types.gen"
import { formatApplicationDate } from "@/lib/application-flow"
import { programImageSource } from "@/lib/program-images"

const PAGE_SIZE = 8
const FETCH_SIZE = 100
const PROGRAM_IMAGE = `${import.meta.env.BASE_URL}brand/og.png`

type ProgramSelectionDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelect: (program: AvailableProgramListItemResponse) => void
  selectedProgramId?: string
  title?: string
  description?: string
}

export function ProgramSelectionDialog({
  open,
  onOpenChange,
  onSelect,
  selectedProgramId,
  title = "Seleccionar programa",
  description = "Buscá por nombre u objetivo del programa.",
}: ProgramSelectionDialogProps) {
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(1)
  const query = useQuery({
    ...listAvailableProgramsOptions({ query: { page: 0, size: FETCH_SIZE } }),
    enabled: open,
  })

  const normalizedSearch = search.trim().toLocaleLowerCase("es-AR")
  const programs = (query.data?.content ?? []).filter((program) => {
    if (!normalizedSearch) return true

    return [program.name, program.objective]
      .filter(Boolean)
      .some((value) =>
        value?.toLocaleLowerCase("es-AR").includes(normalizedSearch),
      )
  })
  const totalItems = programs.length
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = programs.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  )
  const truncated = (query.data?.totalElements ?? 0) > FETCH_SIZE

  const selectProgram = (program: AvailableProgramListItemResponse) => {
    if (program.id === undefined) return
    onSelect(program)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        <InputGroup>
          <InputGroupAddon>
            <IconSearch />
          </InputGroupAddon>
          <InputGroupInput
            value={search}
            onChange={(event) => {
              setSearch(event.target.value)
              setPage(1)
            }}
            placeholder="Buscar programa"
            aria-label="Buscar programa"
            autoFocus
          />
        </InputGroup>

        {query.isPending ? (
          <p className="text-sm text-muted-foreground">Cargando programas…</p>
        ) : query.isError ? (
          <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
            <p>No se pudieron cargar los programas.</p>
            <Button size="sm" variant="outline" onClick={() => query.refetch()}>
              Reintentar
            </Button>
          </div>
        ) : totalItems === 0 ? (
          <Empty className="min-h-56 border">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <IconHeartHandshake />
              </EmptyMedia>
              <EmptyTitle>Sin resultados</EmptyTitle>
              <EmptyDescription>
                {search
                  ? "No encontramos programas que coincidan con la búsqueda."
                  : "No hay programas disponibles para seleccionar."}
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="max-h-[52vh] overflow-auto">
            <Table className="min-w-2xl">
              <TableHeader>
                <TableRow>
                  <TableHead>Programa</TableHead>
                  <TableHead>Ediciones</TableHead>
                  <TableHead>Próxima edición</TableHead>
                  <TableHead className="w-28">
                    <span className="sr-only">Seleccionar</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pageItems.map((program) => {
                  const selected = program.id === selectedProgramId
                  const editions = program.availableEditions ?? 0

                  return (
                    <TableRow key={program.id ?? program.name}>
                      <TableCell>
                        <div className="flex min-w-48 items-center gap-2.5">
                          <img
                            src={programImageSource(program.imageUrl) ?? PROGRAM_IMAGE}
                            alt=""
                            className="size-8 shrink-0 rounded-md object-cover"
                          />
                          <div className="min-w-0">
                            <p className="truncate font-medium">
                              {program.name || "Programa sin nombre"}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">
                              {program.objective || "Sin objetivo declarado"}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={editions > 0 ? "outline" : "secondary"}>
                          {editions === 1 ? "1 edición" : `${editions} ediciones`}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {program.nextEditionStartDate
                          ? `${formatApplicationDate(program.nextEditionStartDate)} al ${formatApplicationDate(program.nextEditionEndDate)}`
                          : "—"}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          type="button"
                          size="sm"
                          variant={selected ? "secondary" : "outline"}
                          disabled={program.id === undefined}
                          onClick={() => selectProgram(program)}
                        >
                          {selected && <IconCheck />}
                          {selected ? "Seleccionado" : "Seleccionar"}
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </div>
        )}

        {truncated && (
          <p className="text-sm text-amber-700 dark:text-amber-300">
            Se muestran los primeros {FETCH_SIZE} programas disponibles.
          </p>
        )}

        {!query.isPending && !query.isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
            className="border-t-0 px-0 py-0"
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
