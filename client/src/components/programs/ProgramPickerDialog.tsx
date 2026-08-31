import { useQuery } from "@tanstack/react-query"
import { useState } from "react"

import { DataPagination } from "@/components/DataPagination"
import { LoadingOrError } from "@/components/programs/ProgramRouteUi"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listOptions } from "@/generated/@tanstack/react-query.gen"
import type { ProgramListItemResponse } from "@/generated/types.gen"

const PAGE_SIZE = 10

type ProgramPickerDialogProps = {
  onOpenChange: (open: boolean) => void
  selectedIds: Set<string>
  excludeIds?: Set<string>
  onToggle: (program: ProgramListItemResponse, checked: boolean) => void
  pendingId?: string
}

export function ProgramPickerDialog({ onOpenChange, selectedIds, excludeIds, onToggle, pendingId }: ProgramPickerDialogProps) {
  const [page, setPage] = useState(1)

  const query = useQuery(listOptions({ query: { page: page - 1, size: PAGE_SIZE } }))
  const programs = (query.data?.content ?? []).filter((program) => program.id && !excludeIds?.has(program.id))
  const totalItems = Number(query.data?.totalElements ?? 0)
  const totalPages = Math.max(1, query.data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Programas incompatibles</DialogTitle>
        </DialogHeader>
        <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
        {!query.isPending && !query.isError && (
          programs.length === 0 ? (
            <p className="text-sm text-muted-foreground">No hay programas disponibles.</p>
          ) : (
            <Table>
              <TableHeader><TableRow><TableHead className="w-10" /><TableHead>Nombre</TableHead><TableHead>Objetivo</TableHead></TableRow></TableHeader>
              <TableBody>
                {programs.map((program) => (
                  <TableRow key={program.id}>
                    <TableCell>
                      <Checkbox
                        checked={!!program.id && selectedIds.has(program.id)}
                        disabled={pendingId === program.id}
                        onCheckedChange={(checked) => onToggle(program, checked === true)}
                        aria-label={`Marcar ${program.name} como incompatible`}
                      />
                    </TableCell>
                    <TableCell className="font-medium">{program.name}</TableCell>
                    <TableCell className="max-w-xs truncate text-muted-foreground">{program.objective || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )
        )}
        {!query.isPending && !query.isError && totalItems > 0 && (
          <DataPagination page={currentPage} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} className="border-t-0 px-0 py-0" />
        )}
      </DialogContent>
    </Dialog>
  )
}
