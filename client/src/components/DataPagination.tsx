import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination"
import { getPaginationRange } from "@/lib/pagination"
import { cn } from "@/lib/utils"

type DataPaginationProps = {
  page: number
  totalPages: number
  totalItems: number
  pageSize: number
  onPageChange: (page: number) => void
  className?: string
}

export function DataPagination({ page, totalPages, totalItems, pageSize, onPageChange, className }: DataPaginationProps) {
  return (
    <div className={cn("flex shrink-0 flex-col-reverse items-center gap-3 border-t px-4 py-3 sm:flex-row sm:justify-between lg:px-6", className)}>
      <p className="text-sm text-muted-foreground">Mostrando {(page - 1) * pageSize + 1}–{Math.min(page * pageSize, totalItems)} de {totalItems}</p>
      <Pagination className="mx-0 w-auto">
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious
              href="#"
              text="Anterior"
              aria-disabled={page === 1}
              className={page === 1 ? "pointer-events-none opacity-50" : undefined}
              onClick={(event) => { event.preventDefault(); onPageChange(Math.max(1, page - 1)) }}
            />
          </PaginationItem>
          {getPaginationRange(page, totalPages).map((item, index) => item === "ellipsis" ? (
            <PaginationItem key={`ellipsis-${index}`}><PaginationEllipsis /></PaginationItem>
          ) : (
            <PaginationItem key={item}>
              <PaginationLink href="#" isActive={item === page} onClick={(event) => { event.preventDefault(); onPageChange(item) }}>{item}</PaginationLink>
            </PaginationItem>
          ))}
          <PaginationItem>
            <PaginationNext
              href="#"
              text="Siguiente"
              aria-disabled={page === totalPages}
              className={page === totalPages ? "pointer-events-none opacity-50" : undefined}
              onClick={(event) => { event.preventDefault(); onPageChange(Math.min(totalPages, page + 1)) }}
            />
          </PaginationItem>
        </PaginationContent>
      </Pagination>
    </div>
  )
}
