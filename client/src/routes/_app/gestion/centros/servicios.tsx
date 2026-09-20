import { IconAlertTriangle, IconClock, IconPencil, IconPlus, IconPower, IconRefresh, IconSearch } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { centerSearchSchema, toCenterListQuery } from "@/components/centros/centerFilters"
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
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import {
  activateMunicipalServiceMutation,
  createMunicipalServiceMutation,
  deactivateMunicipalServiceMutation,
  listMunicipalServicesOptions,
  listMunicipalServicesQueryKey,
  updateMunicipalServiceMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { MunicipalServiceResponse } from "@/generated/types.gen"
import { zCreateMunicipalServiceRequest, zUpdateMunicipalServiceRequest } from "@/generated/zod.gen"

// `durationMinutes` es opcional en el DTO generado, pero el form siempre lo
// completa como número (la duración estimada es obligatoria y positiva).
const durationField = z.number({ error: "La duración debe ser un número positivo de minutos." }).int().positive("La duración debe ser un número positivo de minutos.")

const createServiceSchema = zCreateMunicipalServiceRequest.extend({
  name: zCreateMunicipalServiceRequest.shape.name.trim().min(1, "Ingresá el nombre del servicio."),
  description: zCreateMunicipalServiceRequest.shape.description.trim().min(1, "Ingresá la descripción del servicio."),
  durationMinutes: durationField,
})

const updateServiceSchema = zUpdateMunicipalServiceRequest.extend({
  name: zUpdateMunicipalServiceRequest.shape.name.trim().min(1, "Ingresá el nombre del servicio."),
  description: zUpdateMunicipalServiceRequest.shape.description.trim().min(1, "Ingresá la descripción del servicio."),
  durationMinutes: durationField,
})

export const Route = createFileRoute("/_app/gestion/centros/servicios")({
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
  const [dialog, setDialog] = useState<{ service: MunicipalServiceResponse | null } | null>(null)
  const [statusChange, setStatusChange] = useState<MunicipalServiceResponse | null>(null)
  const [searchInput, setSearchInput] = useState(search.search)

  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    listMunicipalServicesOptions({ query: toCenterListQuery(search) }),
  )
  const services = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(search.page, totalPages)
  const setPage = (page: number) => navigate({ search: { ...search, page } })
  const applySearch = () => navigate({ search: { ...search, search: searchInput.trim(), page: 1 } })
  const setEstado = (estado: "todos" | "activos" | "inactivos") =>
    navigate({ search: { ...search, estado, page: 1 } })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: listMunicipalServicesQueryKey() })
  const activate = useMutation({ ...activateMunicipalServiceMutation(), onSuccess: () => { invalidate(); setStatusChange(null) } })
  const deactivate = useMutation({ ...deactivateMunicipalServiceMutation(), onSuccess: () => { invalidate(); setStatusChange(null) } })
  const statusPending = activate.isPending || deactivate.isPending
  const statusError = activate.error ?? deactivate.error

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Centros", to: "/gestion/centros" }, { label: "Catálogo de servicios" }]} />
        <OutletNavRightButton className="gap-1.5">
          <Button size="sm" onClick={() => setDialog({ service: null })}>
            <IconPlus />
            Nuevo servicio
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-2 sm:mx-4! lg:py-4">
            <p className="mb-3 text-sm text-muted-foreground">
              El catálogo es compartido por todos los centros: editar un servicio se refleja donde se ofrezca.
            </p>
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
                  placeholder="Buscar por nombre…"
                  aria-label="Buscar servicios"
                />
              </InputGroup>
              <div className="flex gap-2">
                <Select value={search.estado} onValueChange={(value) => setEstado(value as "todos" | "activos" | "inactivos")}>
                  <SelectTrigger className="w-36" aria-label="Filtrar por estado">
                    <SelectValue />
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
              <p className="text-sm text-muted-foreground">Cargando servicios…</p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los servicios.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>Reintentar</Button>
              </div>
            ) : services.length === 0 ? (
              <p className="text-sm text-muted-foreground">No hay servicios registrados con ese criterio.</p>
            ) : (
              <Table>
                <TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Descripción</TableHead><TableHead>Duración</TableHead><TableHead>Estado</TableHead><TableHead>Actualizado</TableHead><TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead></TableRow></TableHeader>
                <TableBody>
                  {services.map((service) => (
                    <TableRow key={service.id}>
                      <TableCell className="font-medium">{service.name}</TableCell>
                      <TableCell className="max-w-xl whitespace-normal text-muted-foreground">{service.description || "—"}</TableCell>
                      <TableCell className="text-muted-foreground">
                        <span className="flex items-center gap-1.5 text-xs"><IconClock className="size-3.5 shrink-0" />{service.durationMinutes ?? "—"} min</span>
                      </TableCell>
                      <TableCell>
                        <span className="flex items-center gap-1.5 text-xs">
                          <span className={`size-2 shrink-0 rounded-full ${service.active ? "bg-emerald-500" : "bg-muted-foreground"}`} />
                          {service.active ? "Activo" : "Inactivo"}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{service.updatedAt ? new Date(service.updatedAt).toLocaleDateString("es-AR") : "—"}</TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-1">
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="ghost"
                            aria-label={`Editar servicio ${service.name ?? ""}`}
                            onClick={() => setDialog({ service })}
                          >
                            <IconPencil />
                          </Button>
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="ghost"
                            aria-label={`${service.active ? "Desactivar" : "Activar"} servicio ${service.name ?? ""}`}
                            onClick={() => { activate.reset(); deactivate.reset(); setStatusChange(service) }}
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
        <ServiceDialog
          service={dialog.service}
          onOpenChange={(open) => { if (!open) setDialog(null) }}
        />
      )}
      <AlertDialog open={statusChange !== null} onOpenChange={(open) => { if (!open && !statusPending) setStatusChange(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {statusChange?.active ? "Desactivar servicio" : "Activar servicio"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {statusChange?.active
                ? `«${statusChange?.name}» dejará de ofrecerse en todos los centros, pero se conservan sus asignaciones e historial.`
                : `Se volverá a validar que las disponibilidades que «${statusChange?.name}» volvería efectivas tengan cobertura y no se superpongan. Si alguna falla, toda la activación se rechaza.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          {statusError && (
            <Alert variant="destructive">
              <IconAlertTriangle />
              <AlertTitle>No se pudo cambiar el estado</AlertTitle>
              <AlertDescription>{statusError.message ?? "Revisá las asignaciones del servicio e intentá nuevamente."}</AlertDescription>
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

function ServiceDialog({ service, onOpenChange }: {
  service: MunicipalServiceResponse | null
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const isEditing = service !== null
  const create = useMutation({
    ...createMunicipalServiceMutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listMunicipalServicesQueryKey() })
      onOpenChange(false)
    },
  })
  const update = useMutation({
    ...updateMunicipalServiceMutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listMunicipalServicesQueryKey() })
      onOpenChange(false)
    },
  })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error
  const form = useForm({
    defaultValues: {
      name: service?.name ?? "",
      description: service?.description ?? "",
      durationMinutes: service?.durationMinutes ?? 30,
    },
    validators: { onChange: isEditing ? updateServiceSchema : createServiceSchema },
    onSubmit: ({ value }) => {
      if (isPending) return
      create.reset()
      update.reset()
      const body = {
        name: value.name.trim(),
        description: value.description.trim(),
        durationMinutes: value.durationMinutes,
      }
      if (service?.id) {
        update.mutate({ path: { id: service.id }, body })
      } else {
        create.mutate({ body })
      }
    },
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!isPending) onOpenChange(open) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar servicio" : "Nuevo servicio"}</DialogTitle>
          <DialogDescription>
            {isEditing ? "Los cambios se reflejan en todos los centros que lo ofrecen." : "Definí el servicio una sola vez en el catálogo municipal."}
          </DialogDescription>
        </DialogHeader>

        {mutationError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>{mutationError.message ?? "No se pudo guardar el servicio."}</AlertTitle>
            {mutationError.fields && mutationError.fields.length > 0 && (
              <AlertDescription>
                <ul className="list-disc pl-4">{mutationError.fields.map((field, index) => <li key={`${field.field}-${index}`}>{field.message}</li>)}</ul>
              </AlertDescription>
            )}
          </Alert>
        )}

        <form
          id="service-form"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            event.stopPropagation()
            form.handleSubmit()
          }}
        >
          <FieldGroup>
            <form.Field name="name" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Nombre</FieldLabel>
                  <InputGroup>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                      placeholder="Asesoramiento jurídico"
                      maxLength={150}
                      autoFocus
                    />
                  </InputGroup>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Field name="description" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Descripción</FieldLabel>
                  <Textarea
                    id={field.name}
                    name={field.name}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                    aria-invalid={invalid}
                    placeholder="Describí en qué consiste la atención"
                    className="min-h-24"
                    maxLength={1000}
                  />
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Field name="durationMinutes" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Duración estimada (minutos)</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon><IconClock /></InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      type="number"
                      min={1}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.valueAsNumber)}
                      aria-invalid={invalid}
                    />
                  </InputGroup>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />
          </FieldGroup>
        </form>

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={isPending} />}>
            Cancelar
          </DialogClose>
          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
            <Button type="submit" form="service-form" disabled={!canSubmit || isSubmitting || isPending}>
              {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear servicio"}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
