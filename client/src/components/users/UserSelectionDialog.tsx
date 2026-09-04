import { IconCheck, IconSearch, IconUsers } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { useState } from "react"

import { DataPagination } from "@/components/DataPagination"
import { UserAvatar } from "@/components/UserAvatar"
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
import { findAllOptions } from "@/generated/@tanstack/react-query.gen"
import type { UserManagementResponse } from "@/generated/types.gen"

const PAGE_SIZE = 8

type UserSelectionDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelect: (user: UserManagementResponse) => void
  selectedUserId?: number
  title?: string
  description?: string
}

export function UserSelectionDialog({
  open,
  onOpenChange,
  onSelect,
  selectedUserId,
  title = "Seleccionar usuario",
  description = "Buscá por nombre, usuario o correo electrónico.",
}: UserSelectionDialogProps) {
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(1)
  const query = useQuery({
    ...findAllOptions(),
    enabled: open,
  })

  const normalizedSearch = search.trim().toLocaleLowerCase("es-AR")
  const users = (query.data ?? []).filter((user) => {
    if (!normalizedSearch) return true

    return [user.name, user.username, user.email]
      .filter(Boolean)
      .some((value) =>
        value?.toLocaleLowerCase("es-AR").includes(normalizedSearch),
      )
  })
  const totalItems = users.length
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = users.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  )

  const selectUser = (user: UserManagementResponse) => {
    if (user.id === undefined) return
    onSelect(user)
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
            placeholder="Buscar usuario"
            aria-label="Buscar usuario"
            autoFocus
          />
        </InputGroup>

        {query.isPending ? (
          <p className="text-sm text-muted-foreground">Cargando usuarios…</p>
        ) : query.isError ? (
          <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
            <p>No se pudieron cargar los usuarios.</p>
            <Button size="sm" variant="outline" onClick={() => query.refetch()}>
              Reintentar
            </Button>
          </div>
        ) : totalItems === 0 ? (
          <Empty className="min-h-56 border">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <IconUsers />
              </EmptyMedia>
              <EmptyTitle>Sin resultados</EmptyTitle>
              <EmptyDescription>
                {search
                  ? "No encontramos usuarios que coincidan con la búsqueda."
                  : "No hay usuarios disponibles para seleccionar."}
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="max-h-[52vh] overflow-auto">
            <Table className="min-w-2xl">
              <TableHeader>
                <TableRow>
                  <TableHead>Usuario</TableHead>
                  <TableHead>Correo electrónico</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead className="w-28">
                    <span className="sr-only">Seleccionar</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pageItems.map((user) => {
                  const selected = user.id === selectedUserId

                  return (
                    <TableRow key={user.id ?? user.username}>
                      <TableCell>
                        <div className="flex min-w-48 items-center gap-2.5">
                          <UserAvatar user={user} size="sm" />
                          <div className="min-w-0">
                            <p className="truncate font-medium">
                              {user.name || "Sin nombre"}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">
                              {user.username ? `@${user.username}` : "Sin usuario"}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {user.email || "—"}
                      </TableCell>
                      <TableCell>
                        <Badge variant={user.active ? "outline" : "secondary"}>
                          {user.active ? "Activo" : "Inactivo"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          type="button"
                          size="sm"
                          variant={selected ? "secondary" : "outline"}
                          disabled={user.id === undefined}
                          onClick={() => selectUser(user)}
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
