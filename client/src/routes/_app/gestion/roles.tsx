import {
  IconAlertTriangle,
  IconPencil,
  IconPlus,
  IconRefresh,
  IconShieldLock,
} from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import * as z from "zod"

import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
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
import { Switch } from "@/components/ui/switch"
import {
  create1Mutation,
  findAll1Options,
  findAll1QueryKey,
  findAll4Options,
  update1Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type { RoleResponse } from "@/generated/types.gen"
import { zCreateRoleRequest, zUpdateRoleRequest } from "@/generated/zod.gen"

// `permissions` es opcional en el DTO generado, pero el form siempre lo completa como array.
const zCreateRoleForm = zCreateRoleRequest.extend({
  permissions: z.array(z.string()),
})
const zUpdateRoleForm = zUpdateRoleRequest.extend({
  permissions: z.array(z.string()),
})

export const Route = createFileRoute("/_app/gestion/roles")({
  component: RouteComponent,
})

const PAGE_SIZE = 10

type DialogState = { role: RoleResponse | null } | null

function RouteComponent() {
  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } =
    useQuery(findAll1Options())
  const [page, setPage] = useState(1)
  const [dialogState, setDialogState] = useState<DialogState>(null)

  const totalItems = data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems =
    data?.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE) ?? []

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Roles y permisos" }]} />
        <OutletNavRightButton className="gap-1.5">
          <Button
            size="sm"
            onClick={() => setDialogState({ role: null })}
          >
            <IconPlus />
            Nuevo rol
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="py-2 lg:py-4 mx-2 sm:mx-4!">
            {dataUpdatedAt > 0 && (
              <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                <span>
                  Última actualización:{" "}
                  {new Date(dataUpdatedAt).toLocaleTimeString("es-AR")}
                </span>
                <Button
                  size="xs"
                  variant="ghost"
                  onClick={() => refetch()}
                  disabled={isFetching}
                >
                  <IconRefresh className={isFetching ? "animate-spin" : undefined} />
                  Actualizar
                </Button>
              </div>
            )}
            {isPending ? (
              <p className="text-sm text-muted-foreground">
                Cargando roles…
              </p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los roles.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>
                  Reintentar
                </Button>
              </div>
            ) : totalItems === 0 ? (
              <p className="text-sm text-muted-foreground">
                No hay roles configurados.
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Rol</TableHead>
                    <TableHead>Permisos</TableHead>
                    <TableHead className="w-px" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {pageItems.map((role) => (
                    <TableRow key={role.id ?? role.name}>
                      <TableCell className="align-top font-medium whitespace-nowrap">
                        {role.name}
                      </TableCell>
                      <TableCell>
                        {(role.permissions ?? []).length === 0 ? (
                          <span className="text-muted-foreground">
                            Sin permisos asignados
                          </span>
                        ) : (
                          <ul className="grid grid-cols-2 gap-x-4 gap-y-1 sm:grid-cols-3 xl:grid-cols-4">
                            {(role.permissions ?? []).map((permission) => (
                              <li
                                key={permission}
                                className="flex min-w-0 items-center gap-2 text-xs text-muted-foreground"
                              >
                                <span className="size-1 shrink-0 bg-muted-foreground/70" />
                                <span className="truncate font-mono" title={permission}>
                                  {permission}
                                </span>
                              </li>
                            ))}
                          </ul>
                        )}
                      </TableCell>
                      <TableCell className="align-top">
                        <Button
                          size="icon-sm"
                          variant="ghost"
                          aria-label={`Editar ${role.name ?? "rol"}`}
                          onClick={() => setDialogState({ role })}
                        >
                          <IconPencil />
                        </Button>
                      </TableCell>
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

      {dialogState && (
        <RoleFormDialog
          role={dialogState.role}
          onOpenChange={(open) => {
            if (!open) setDialogState(null)
          }}
        />
      )}
    </SidebarShell>
  )
}

function RoleFormDialog({
  role,
  onOpenChange,
}: {
  role: RoleResponse | null
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const permissions = useQuery(findAll4Options())
  const isEditing = role != null

  const createRole = useMutation({
    ...create1Mutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: findAll1QueryKey() })
      onOpenChange(false)
    },
  })
  const updateRole = useMutation({
    ...update1Mutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: findAll1QueryKey() })
      onOpenChange(false)
    },
  })

  const isPending = createRole.isPending || updateRole.isPending
  const isError = createRole.isError || updateRole.isError

  const form = useForm({
    defaultValues: {
      name: role?.name ?? "",
      permissions: role?.permissions ?? ([] as string[]),
    },
    validators: {
      onChange: isEditing ? zUpdateRoleForm : zCreateRoleForm,
    },
    onSubmit: ({ value }) => {
      if (isEditing && role.id != null) {
        updateRole.mutate({ path: { id: role.id }, body: value })
      } else {
        createRole.mutate({ body: value })
      }
    },
  })

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar rol" : "Nuevo rol"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Modificá el nombre y los permisos asignados a este rol."
              : "Definí el nombre y los permisos del nuevo rol."}
          </DialogDescription>
        </DialogHeader>
        {isError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No se pudo guardar el rol</AlertTitle>
            <AlertDescription>
              Revisá los datos e intentá de nuevo.
            </AlertDescription>
          </Alert>
        )}
        <form
          id="role-form"
          noValidate
          onSubmit={(e) => {
            e.preventDefault()
            e.stopPropagation()
            form.handleSubmit()
          }}
        >
          <FieldGroup>
            <form.Field
              name="name"
              children={(field) => {
                const isInvalid =
                  field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>
                      Nombre del rol
                    </FieldLabel>
                    <InputGroup>
                      <InputGroupAddon>
                        <IconShieldLock />
                      </InputGroupAddon>
                      <InputGroupInput
                        id={field.name}
                        name={field.name}
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                        aria-invalid={isInvalid}
                        placeholder="COORDINADOR"
                      />
                    </InputGroup>
                    {isInvalid && (
                      <FieldError errors={field.state.meta.errors} />
                    )}
                  </Field>
                )
              }}
            />
            <form.Field
              name="permissions"
              children={(field) => (
                <Field>
                  <FieldLabel>Permisos</FieldLabel>
                  {permissions.isPending ? (
                    <p className="text-sm text-muted-foreground">
                      Cargando permisos…
                    </p>
                  ) : permissions.isError ? (
                    <p className="text-sm text-muted-foreground">
                      No se pudieron cargar los permisos.
                    </p>
                  ) : permissions.data.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                      No hay permisos configurados.
                    </p>
                  ) : (
                    <div className="grid max-h-72 grid-cols-2 gap-x-4 gap-y-2 overflow-y-auto pr-1 sm:grid-cols-3 lg:grid-cols-4">
                      {permissions.data.map((permission) => {
                        const permissionName = permission.name ?? ""
                        const checked =
                          field.state.value?.includes(permissionName) ??
                          false
                        return (
                          <FieldLabel
                            key={permission.id ?? permissionName}
                            htmlFor={`permission-${permission.id}`}
                            className="min-w-0 w-full cursor-pointer items-center gap-2"
                          >
                            <Switch
                              id={`permission-${permission.id}`}
                              size="sm"
                              checked={checked}
                              onCheckedChange={(nextChecked) => {
                                const current = field.state.value ?? []
                                field.handleChange(
                                  nextChecked
                                    ? [...current, permissionName]
                                    : current.filter(
                                        (p) => p !== permissionName,
                                      ),
                                )
                              }}
                            />
                            <span
                              className="min-w-0 truncate font-mono text-xs"
                              title={permissionName}
                            >
                              {permissionName}
                            </span>
                          </FieldLabel>
                        )
                      })}
                    </div>
                  )}
                  <FieldDescription>
                    Seleccioná los permisos que tendrá este rol.
                  </FieldDescription>
                </Field>
              )}
            />
          </FieldGroup>
        </form>
        <DialogFooter>
          <DialogClose render={<Button variant="outline" type="button" />}>
            Cancelar
          </DialogClose>
          <Button type="submit" form="role-form" disabled={isPending}>
            {isPending
              ? "Guardando…"
              : isEditing
                ? "Guardar cambios"
                : "Crear rol"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
