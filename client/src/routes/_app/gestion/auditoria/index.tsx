import { IconRefresh, IconSearch, IconUserSearch } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState, type FormEvent } from "react"

import { AuditDiffDialog } from "@/components/audit/AuditDiffDialog"
import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { UserSelectionDialog } from "@/components/users/UserSelectionDialog"
import { UserAvatar } from "@/components/UserAvatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  listLogsByEntityOptions,
  listLogsByUserOptions,
  listLogsOptions,
} from "@/generated/@tanstack/react-query.gen"
import type {
  LogResponse,
  UserManagementResponse,
} from "@/generated/types.gen"

type EntityType = NonNullable<LogResponse["entityType"]>
type Filter =
  | { mode: "entity"; entityType: EntityType; entityId: string }
  | { mode: "user"; userId: number }

const PAGE_SIZE = 50

const filterModeLabels: Record<Filter["mode"], string> = {
  entity: "Entidad",
  user: "Usuario que realizó el cambio",
}

const actionLabels: Record<NonNullable<LogResponse["action"]>, string> = {
  CREATE: "Creación",
  UPDATE: "Modificación",
  DELETE: "Eliminación",
  LOGIN: "Inicio de sesión",
}

const entityLabels: Record<EntityType, string> = {
  PERMISSION: "Permiso",
  ROLE: "Rol",
  USER: "Usuario",
  ENROLLMENT_PERIOD: "Período de inscripción",
}

const actionStyles: Record<NonNullable<LogResponse["action"]>, string> = {
  CREATE:
    "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  UPDATE:
    "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-400",
  DELETE: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400",
  LOGIN:
    "border-violet-500/30 bg-violet-500/10 text-violet-700 dark:text-violet-400",
}

export const Route = createFileRoute("/_app/gestion/auditoria/")({
  component: RouteComponent,
})

function RouteComponent() {
  const [mode, setMode] = useState<Filter["mode"]>("user")
  const [entityType, setEntityType] = useState<EntityType>("USER")
  const [entityId, setEntityId] = useState("")
  const [selectedUser, setSelectedUser] =
    useState<UserManagementResponse | null>(null)
  const [userSelectionOpen, setUserSelectionOpen] = useState(false)
  const [filter, setFilter] = useState<Filter | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)
  const [page, setPage] = useState(1)

  const entityFilter = filter?.mode === "entity" ? filter : null
  const userFilter = filter?.mode === "user" ? filter : null

  const allLogsQuery = useQuery({
    ...listLogsOptions(),
    enabled: filter === null,
  })
  const entityQuery = useQuery({
    ...listLogsByEntityOptions({
      path: {
        entityType: entityFilter?.entityType ?? "USER",
        entityId: entityFilter?.entityId ?? "-",
      },
    }),
    enabled: entityFilter !== null,
  })
  const userQuery = useQuery({
    ...listLogsByUserOptions({
      path: { userId: userFilter?.userId ?? 1 },
    }),
    enabled: userFilter !== null,
  })

  const activeQuery =
    filter === null ? allLogsQuery : filter.mode === "user" ? userQuery : entityQuery
  const logs = activeQuery.data ?? []
  const totalItems = logs.length
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = logs.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  )

  const submitFilter = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setValidationError(null)

    if (mode === "entity") {
      const normalizedEntityId = entityId.trim()
      if (!normalizedEntityId) {
        setValidationError("Ingresá el identificador de la entidad.")
        return
      }
      setFilter({ mode, entityType, entityId: normalizedEntityId })
    } else {
      if (selectedUser?.id === undefined) {
        setValidationError("Seleccioná un usuario.")
        return
      }
      setFilter({ mode, userId: selectedUser.id })
    }

    setPage(1)
  }

  const changeMode = (nextMode: Filter["mode"]) => {
    setMode(nextMode)
    setFilter(null)
    setValidationError(null)
    setPage(1)
  }

  const clearFilter = () => {
    setFilter(null)
    setValidationError(null)
    setPage(1)
  }

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs
          items={[{ label: "Auditoría" }, { label: "Cambios" }]}
        />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-2 sm:mx-4! lg:py-4">
            <form
              className="mb-5 flex flex-col gap-3 rounded-2xl border bg-muted/20 p-3 lg:flex-row lg:items-end"
              onSubmit={submitFilter}
            >
              <div className="grid gap-1.5">
                <Label htmlFor="audit-filter-mode">Buscar por</Label>
                <Select
                  value={mode}
                  onValueChange={(value) => value && changeMode(value)}
                >
                  <SelectTrigger id="audit-filter-mode" className="w-full">
                    <SelectValue>
                      {(value: Filter["mode"]) => filterModeLabels[value]}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className={"min-w-62"}>
                    {Object.entries(filterModeLabels).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {mode === "entity" ? (
                <>
                  <div className="grid gap-1.5">
                    <Label htmlFor="audit-entity-type">Tipo de entidad</Label>
                    <Select
                      value={entityType}
                      onValueChange={(value) => value && setEntityType(value)}
                    >
                      <SelectTrigger id="audit-entity-type" className="w-full">
                        <SelectValue>
                          {(value: EntityType) => entityLabels[value]}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent className={"min-w-54"}>
                        {Object.entries(entityLabels).map(([value, label]) => (
                          <SelectItem key={value} value={value}>
                            {label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="grid min-w-0 flex-1 gap-1.5">
                    <Label htmlFor="audit-entity-id">ID de entidad</Label>
                    <Input
                      id="audit-entity-id"
                      value={entityId}
                      onChange={(event) => setEntityId(event.target.value)}
                      placeholder="Ingresá el identificador"
                      aria-invalid={validationError !== null}
                    />
                  </div>
                </>
              ) : (
                <div className="grid min-w-0 flex-1 gap-1.5">
                  <Label htmlFor="audit-user-picker">Usuario</Label>
                  <Button
                    id="audit-user-picker"
                    type="button"
                    variant="outline"
                    className="min-w-0 justify-start"
                    onClick={() => setUserSelectionOpen(true)}
                    aria-invalid={validationError !== null}
                  >
                    {selectedUser ? (
                      <>
                        <UserAvatar user={selectedUser} className="size-5" />
                        <span className="truncate">
                          {selectedUser.name || selectedUser.username}
                          {selectedUser.username && ` (@${selectedUser.username})`}
                        </span>
                      </>
                    ) : (
                      <>
                        <IconUserSearch />
                        Seleccionar usuario
                      </>
                    )}
                  </Button>
                </div>
              )}

              <Button type="submit" className="lg:self-end">
                <IconSearch />
                Buscar
              </Button>

              {filter && (
                <Button
                  type="button"
                  variant="ghost"
                  className="lg:self-end"
                  onClick={clearFilter}
                >
                  Ver todos los registros
                </Button>
              )}
            </form>

            {validationError && (
              <p className="-mt-2 mb-4 text-sm text-destructive">
                {validationError}
              </p>
            )}

            {activeQuery.isPending ? (
              <p className="text-sm text-muted-foreground">
                Cargando cambios…
              </p>
            ) : activeQuery.isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los cambios de auditoría.</p>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => activeQuery.refetch()}
                >
                  Reintentar
                </Button>
              </div>
            ) : totalItems === 0 ? (
              <p className="text-sm text-muted-foreground">
                {filter
                  ? "No hay cambios registrados para este criterio."
                  : "No hay cambios registrados."}
              </p>
            ) : (
              <>
                <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                  <span>
                    Última actualización:{" "}
                    {new Date(activeQuery.dataUpdatedAt).toLocaleTimeString(
                      "es-AR",
                    )}
                  </span>
                  <Button
                    size="xs"
                    variant="ghost"
                    onClick={() => activeQuery.refetch()}
                    disabled={activeQuery.isFetching}
                  >
                    <IconRefresh
                      className={
                        activeQuery.isFetching ? "animate-spin" : undefined
                      }
                    />
                    Actualizar
                  </Button>
                </div>

                <div className="overflow-x-auto">
                  <Table className="min-w-3xl">
                    <TableHeader>
                      <TableRow>
                        <TableHead>Fecha y hora</TableHead>
                        <TableHead>Acción</TableHead>
                        <TableHead>Entidad</TableHead>
                        <TableHead>ID de entidad</TableHead>
                        <TableHead>Actor</TableHead>
                        <TableHead>Diff</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {pageItems.map((log) => (
                        <TableRow
                          key={
                            log.id ?? `${log.createdAt}-${log.entityId}`
                          }
                        >
                          <TableCell className="whitespace-nowrap text-muted-foreground">
                            {formatDateTime(log.createdAt)}
                          </TableCell>
                          <TableCell>
                            {log.action ? (
                              <Badge
                                variant="outline"
                                className={actionStyles[log.action]}
                              >
                                {actionLabels[log.action]}
                              </Badge>
                            ) : (
                              "—"
                            )}
                          </TableCell>
                          <TableCell>
                            {log.entityType
                              ? entityLabels[log.entityType]
                              : "—"}
                          </TableCell>
                          <TableCell className="font-mono text-xs text-muted-foreground">
                            {log.entityId ?? "—"}
                          </TableCell>
                          <TableCell>
                            <ActorCell log={log} />
                          </TableCell>
                          <TableCell>
                            {log.entityId &&
                            (log.oldValues || log.newValues) ? (
                              <AuditDiffDialog log={log} />
                            ) : (
                              "—"
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </>
            )}
          </div>
        </div>

        {!activeQuery.isPending &&
          !activeQuery.isError &&
          totalItems > 0 && (
            <DataPagination
              page={currentPage}
              totalPages={totalPages}
              totalItems={totalItems}
              pageSize={PAGE_SIZE}
              onPageChange={setPage}
            />
          )}
      </SidebarShellContent>

      <UserSelectionDialog
        open={userSelectionOpen}
        onOpenChange={setUserSelectionOpen}
        selectedUserId={selectedUser?.id}
        onSelect={(user) => {
          setSelectedUser(user)
          setValidationError(null)
        }}
        title="Seleccionar actor"
        description="Elegí el usuario cuyos cambios querés consultar."
      />
    </SidebarShell>
  )
}

function ActorCell({ log }: { log: LogResponse }) {
  if (!log.actor) {
    return <span className="text-muted-foreground">Sistema</span>
  }

  return (
    <div className="min-w-40">
      <p className="font-medium">{log.actor.name || "Usuario eliminado"}</p>
      {log.actor.username && (
        <p className="text-xs text-muted-foreground">@{log.actor.username}</p>
      )}
    </div>
  )
}

function formatDateTime(value?: string) {
  if (!value) return "—"

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"

  return date.toLocaleString("es-AR", {
    dateStyle: "short",
    timeStyle: "medium",
  })
}
