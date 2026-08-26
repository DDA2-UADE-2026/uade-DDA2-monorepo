import {
  IconAlertTriangle,
  IconEye,
  IconEyeOff,
  IconId,
  IconLock,
  IconMail,
  IconRefresh,
  IconUser,
  IconUserPlus,
} from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import * as z from "zod"

import {
  OutletNavContent,
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { UserAvatar } from "@/components/UserAvatar"
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
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldTitle,
} from "@/components/ui/field"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  createMutation,
  findAll1Options,
  findAllOptions,
  findAllQueryKey,
} from "@/generated/@tanstack/react-query.gen"
import { zCreateUserRequestWritable } from "@/generated/zod.gen"

// `active` y `roles` son opcionales en el DTO generado, pero el form siempre los completa.
const zCreateUserForm = zCreateUserRequestWritable.extend({
  active: z.boolean(),
  roles: z.array(z.string()),
})

export const Route = createFileRoute("/_app/gestion/usuarios/")({
  component: RouteComponent,
})

const PAGE_SIZE = 10

function getPaginationRange(
  current: number,
  total: number,
): (number | "ellipsis")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const pages: (number | "ellipsis")[] = [1]
  if (current > 3) pages.push("ellipsis")

  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  for (let page = start; page <= end; page++) pages.push(page)

  if (current < total - 2) pages.push("ellipsis")
  pages.push(total)

  return pages
}

function RouteComponent() {
  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } =
    useQuery(findAllOptions())
  const [page, setPage] = useState(1)
  const [createOpen, setCreateOpen] = useState(false)

  const totalItems = data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems =
    data?.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE) ?? []

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavContent>Usuarios</OutletNavContent>
        <OutletNavRightButton className="gap-1.5">
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <IconUserPlus />
            Nuevo usuario
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
                Cargando usuarios…
              </p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los usuarios.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>
                  Reintentar
                </Button>
              </div>
            ) : totalItems === 0 ? (
              <p className="text-sm text-muted-foreground">
                No hay usuarios registrados.
              </p>
            ) : (
              <Table className="">
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">
                      <span className="sr-only">Avatar</span>
                    </TableHead>
                    <TableHead>Nombre</TableHead>
                    <TableHead>Usuario</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Roles</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead>Creado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {pageItems.map((user) => (
                    <TableRow key={user.id ?? user.username}>
                      <TableCell className="pr-0">
                        <UserAvatar user={user} size="sm" />
                      </TableCell>
                      <TableCell className="font-medium">
                        {user.name}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {user.username}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {user.email}
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {(user.roles ?? []).map((role) => (
                            <span
                              key={role}
                              className="rounded-full border border-border bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                            >
                              {role}
                            </span>
                          ))}
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="flex items-center gap-1.5 text-xs">
                          <span
                            className={`size-2 shrink-0 rounded-full ${
                              user.active
                                ? "bg-emerald-500"
                                : "bg-muted-foreground"
                            }`}
                          />
                          {user.active ? "Activo" : "Inactivo"}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {user.createdAt
                          ? new Date(user.createdAt).toLocaleDateString(
                              "es-AR",
                            )
                          : "—"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>

        {!isPending && !isError && totalItems > 0 && (
          <div className="flex shrink-0 flex-col-reverse items-center gap-3 border-t px-4 py-3 sm:flex-row sm:justify-between lg:px-6">
            <p className="text-sm text-muted-foreground">
              Mostrando {(currentPage - 1) * PAGE_SIZE + 1}-
              {Math.min(currentPage * PAGE_SIZE, totalItems)} de {totalItems}{" "}

            </p>
            <Pagination className="mx-0 w-auto">
              <PaginationContent>
                <PaginationItem>
                  <PaginationPrevious
                    href="#"
                    text="Anterior"
                    aria-disabled={currentPage === 1}
                    className={
                      currentPage === 1
                        ? "pointer-events-none opacity-50"
                        : undefined
                    }
                    onClick={(e) => {
                      e.preventDefault()
                      setPage((p) => Math.max(1, p - 1))
                    }}
                  />
                </PaginationItem>
                {getPaginationRange(currentPage, totalPages).map(
                  (item, index) =>
                    item === "ellipsis" ? (
                      <PaginationItem key={`ellipsis-${index}`}>
                        <PaginationEllipsis />
                      </PaginationItem>
                    ) : (
                      <PaginationItem key={item}>
                        <PaginationLink
                          href="#"
                          isActive={item === currentPage}
                          onClick={(e) => {
                            e.preventDefault()
                            setPage(item)
                          }}
                        >
                          {item}
                        </PaginationLink>
                      </PaginationItem>
                    ),
                )}
                <PaginationItem>
                  <PaginationNext
                    href="#"
                    text="Siguiente"
                    aria-disabled={currentPage === totalPages}
                    className={
                      currentPage === totalPages
                        ? "pointer-events-none opacity-50"
                        : undefined
                    }
                    onClick={(e) => {
                      e.preventDefault()
                      setPage((p) => Math.min(totalPages, p + 1))
                    }}
                  />
                </PaginationItem>
              </PaginationContent>
            </Pagination>
          </div>
        )}
      </SidebarShellContent>

      {createOpen && (
        <CreateUserDialog onOpenChange={(open) => !open && setCreateOpen(false)} />
      )}
    </SidebarShell>
  )
}

function CreateUserDialog({
  onOpenChange,
}: {
  onOpenChange: (open: boolean) => void
}) {
  const [showPassword, setShowPassword] = useState(false)
  const queryClient = useQueryClient()

  const roles = useQuery(findAll1Options())
  const createUser = useMutation({
    ...createMutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: findAllQueryKey() })
      onOpenChange(false)
    },
  })

  const form = useForm({
    defaultValues: {
      name: "",
      username: "",
      email: "",
      password: "",
      active: true,
      roles: [] as string[],
    },
    validators: {
      onChange: zCreateUserForm,
    },
    onSubmit: ({ value }) => {
      createUser.mutate({ body: value })
    },
  })

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nuevo usuario</DialogTitle>
          <DialogDescription>
            Creá una cuenta y asignale los roles correspondientes.
          </DialogDescription>
        </DialogHeader>
        {createUser.isError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No se pudo crear el usuario</AlertTitle>
            <AlertDescription>
              Revisá los datos e intentá de nuevo.
            </AlertDescription>
          </Alert>
        )}
        <form
          id="create-user-form"
          className="max-h-[70vh] overflow-y-auto px-1"
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
                      Nombre completo
                    </FieldLabel>
                    <InputGroup>
                      <InputGroupAddon>
                        <IconUser />
                      </InputGroupAddon>
                      <InputGroupInput
                        id={field.name}
                        name={field.name}
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                        aria-invalid={isInvalid}
                        placeholder="Ada Lovelace"
                        autoComplete="name"
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
              name="username"
              children={(field) => {
                const isInvalid =
                  field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>Usuario</FieldLabel>
                    <InputGroup>
                      <InputGroupAddon>
                        <IconId />
                      </InputGroupAddon>
                      <InputGroupInput
                        id={field.name}
                        name={field.name}
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                        aria-invalid={isInvalid}
                        placeholder="ada.lovelace"
                        autoComplete="username"
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
              name="email"
              children={(field) => {
                const isInvalid =
                  field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>
                      Correo electrónico
                    </FieldLabel>
                    <InputGroup>
                      <InputGroupAddon>
                        <IconMail />
                      </InputGroupAddon>
                      <InputGroupInput
                        id={field.name}
                        name={field.name}
                        type="email"
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                        aria-invalid={isInvalid}
                        placeholder="ada@municipio.gob.ar"
                        autoComplete="email"
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
              name="password"
              children={(field) => {
                const isInvalid =
                  field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>
                      Contraseña inicial
                    </FieldLabel>
                    <InputGroup>
                      <InputGroupAddon>
                        <IconLock />
                      </InputGroupAddon>
                      <InputGroupInput
                        id={field.name}
                        name={field.name}
                        type={showPassword ? "text" : "password"}
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                        aria-invalid={isInvalid}
                        placeholder="••••••••"
                        autoComplete="new-password"
                      />
                      <InputGroupAddon align="inline-end">
                        <InputGroupButton
                          type="button"
                          size="icon-xs"
                          aria-label={
                            showPassword
                              ? "Ocultar contraseña"
                              : "Mostrar contraseña"
                          }
                          onClick={() => setShowPassword((v) => !v)}
                        >
                          {showPassword ? <IconEyeOff /> : <IconEye />}
                        </InputGroupButton>
                      </InputGroupAddon>
                    </InputGroup>
                    {isInvalid && (
                      <FieldError errors={field.state.meta.errors} />
                    )}
                  </Field>
                )
              }}
            />
            <form.Field
              name="active"
              children={(field) => (
                <FieldLabel htmlFor={field.name}>
                  <Field orientation="horizontal">
                    <FieldContent>
                      <FieldTitle>Usuario activo</FieldTitle>
                      <FieldDescription>
                        Puede iniciar sesión en el sistema.
                      </FieldDescription>
                    </FieldContent>
                    <Switch
                      id={field.name}
                      checked={field.state.value}
                      onCheckedChange={(checked) =>
                        field.handleChange(checked)
                      }
                    />
                  </Field>
                </FieldLabel>
              )}
            />
            <form.Field
              name="roles"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor={field.name}>Roles</FieldLabel>
                  {roles.isPending ? (
                    <p className="text-sm text-muted-foreground">
                      Cargando roles…
                    </p>
                  ) : roles.isError ? (
                    <p className="text-sm text-muted-foreground">
                      No se pudieron cargar los roles.
                    </p>
                  ) : roles.data.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                      No hay roles configurados.
                    </p>
                  ) : (
                    <Select
                      multiple
                      value={field.state.value}
                      onValueChange={(value) => field.handleChange(value)}
                    >
                      <SelectTrigger id={field.name} className="w-full">
                        <SelectValue placeholder="Seleccioná uno o más roles">
                          {(value: string[]) =>
                            value.length === 0
                              ? "Seleccioná uno o más roles"
                              : value.join(", ")
                          }
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {roles.data.map((role) => {
                          const roleName = role.name ?? ""
                          return (
                            <SelectItem
                              key={role.id ?? roleName}
                              value={roleName}
                            >
                              {roleName}
                            </SelectItem>
                          )
                        })}
                      </SelectContent>
                    </Select>
                  )}
                </Field>
              )}
            />
          </FieldGroup>
        </form>
        <DialogFooter>
          <DialogClose render={<Button variant="outline" type="button" />}>
            Cancelar
          </DialogClose>
          <Button
            type="submit"
            form="create-user-form"
            disabled={createUser.isPending}
          >
            {createUser.isPending ? "Creando…" : "Crear usuario"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
