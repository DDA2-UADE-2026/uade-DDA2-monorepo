import {
  IconAlertTriangle,
  IconCalendarEvent,
  IconPencil,
  IconPlus,
  IconRefresh,
} from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect, useState, type ComponentProps } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { FormField, RoutePanel, parseLocalDate } from "@/components/programs/ProgramRouteUi"
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
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import {
  closeEnrollmentPeriodMutation,
  createEnrollmentPeriodMutation,
  findById3Options,
  getEnrollmentPeriodOptions,
  getEnrollmentPeriodQueryKey,
  listEnrollmentPeriodsOptions,
  listEnrollmentPeriodsQueryKey,
  openEnrollmentPeriodMutation,
  reopenEnrollmentPeriodMutation,
  suspendEnrollmentPeriodMutation,
  updateEnrollmentPeriodMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { EnrollmentPeriodResponse, ErrorResponse } from "@/generated/types.gen"
import {
  zCreateEnrollmentPeriodRequest,
  zUpdateEnrollmentPeriodRequest,
} from "@/generated/zod.gen"

const PAGE_SIZE = 10

const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
})

function validateDateRange(
  value: { openDate: string; closeDate: string },
  context: z.RefinementCtx,
) {
  if (value.closeDate < value.openDate) {
    context.addIssue({
      code: "custom",
      path: ["closeDate"],
      message: "La fecha de cierre no puede ser anterior a la fecha de apertura.",
    })
  }
}

const createPeriodSchema = zCreateEnrollmentPeriodRequest.extend({
  openDate: zCreateEnrollmentPeriodRequest.shape.openDate.unwrap(),
  closeDate: zCreateEnrollmentPeriodRequest.shape.closeDate.unwrap(),
  notes: zCreateEnrollmentPeriodRequest.shape.notes.unwrap(),
}).superRefine(validateDateRange)

const updatePeriodSchema = zUpdateEnrollmentPeriodRequest.extend({
  openDate: zUpdateEnrollmentPeriodRequest.shape.openDate.unwrap(),
  closeDate: zUpdateEnrollmentPeriodRequest.shape.closeDate.unwrap(),
  notes: zUpdateEnrollmentPeriodRequest.shape.notes.unwrap(),
}).superRefine(validateDateRange)

const periodStatusLabels = {
  SCHEDULED: "Programado",
  OPEN: "Abierto",
  SUSPENDED: "Suspendido",
  CLOSED: "Cerrado",
} as const

type PeriodStatus = keyof typeof periodStatusLabels
type DialogState = { enrollmentPeriodId: string | null } | null

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/periodos-inscripcion",
)({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId, edicionId } = Route.useParams()
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [dialogState, setDialogState] = useState<DialogState>(null)

  const edition = useQuery(findById3Options({ path: { id: edicionId } }))
  const periods = useQuery(listEnrollmentPeriodsOptions({
    path: { programId: programaId, editionId: edicionId },
    query: { page: page - 1, size: PAGE_SIZE },
  }))

  const items = periods.data?.content ?? []
  const totalItems = periods.data?.totalElements ?? 0
  const totalPages = Math.max(1, periods.data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)
  const editionClosed = edition.data?.status === "CLOSED"

  return (
    <RoutePanel>
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <h3 className="font-heading text-lg font-semibold">Períodos de inscripción</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Administrá las fechas en las que esta convocatoria recibe postulaciones.
          </p>
        </div>
        <Button
          onClick={() => setDialogState({ enrollmentPeriodId: null })}
          disabled={editionClosed || edition.isPending || edition.isError}
        >
          <IconPlus />
          Nuevo período
        </Button>
      </div>

      {editionClosed && (
        <Alert variant="destructive" className="mb-5">
          <IconAlertTriangle />
          <AlertTitle>Convocatoria cerrada</AlertTitle>
          <AlertDescription>
            Podés consultar los períodos existentes, pero no crear períodos nuevos.
          </AlertDescription>
        </Alert>
      )}

      {periods.dataUpdatedAt > 0 && (
        <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <span>
            Última actualización: {new Date(periods.dataUpdatedAt).toLocaleTimeString("es-AR")}
          </span>
          <Button
            size="xs"
            variant="ghost"
            onClick={() => periods.refetch()}
            disabled={periods.isFetching}
          >
            <IconRefresh className={periods.isFetching ? "animate-spin" : undefined} />
            Actualizar
          </Button>
        </div>
      )}

      {periods.isPending ? (
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            Cargando períodos de inscripción…
          </CardContent>
        </Card>
      ) : periods.isError ? (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>No se pudieron cargar los períodos</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-3">
            <span>{periods.error.message ?? "Intentá nuevamente."}</span>
            <Button size="sm" variant="outline" onClick={() => periods.refetch()}>
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : items.length === 0 ? (
        <Card className="border-dashed bg-muted/20 text-center">
          <CardHeader>
            <IconCalendarEvent className="mx-auto size-8 text-muted-foreground" />
            <CardTitle>Sin períodos configurados</CardTitle>
            <CardDescription>
              Creá el primer período para definir cuándo se aceptarán postulaciones.
            </CardDescription>
          </CardHeader>
        </Card>
      ) : (
        <div className="overflow-hidden rounded-xl border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Apertura</TableHead>
                <TableHead>Cierre</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="hidden max-w-sm md:table-cell">Observaciones</TableHead>
                <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((period) => (
                <TableRow key={period.id}>
                  <TableCell className="font-medium">{formatDate(period.openDate)}</TableCell>
                  <TableCell>{formatDate(period.closeDate)}</TableCell>
                  <TableCell><PeriodStatusBadge status={period.status} /></TableCell>
                  <TableCell className="hidden max-w-sm truncate text-muted-foreground md:table-cell">
                    {period.notes || "—"}
                  </TableCell>
                  <TableCell>
                    {period.id && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        aria-label="Editar período de inscripción"
                        onClick={() => setDialogState({ enrollmentPeriodId: period.id! })}
                      >
                        <IconPencil />
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {!periods.isPending && !periods.isError && totalItems > 0 && (
        <DataPagination
          className="mt-4 rounded-xl border"
          page={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={PAGE_SIZE}
          onPageChange={(nextPage) => navigate({ search: { page: nextPage } })}
        />
      )}

      {dialogState && (
        <EnrollmentPeriodDialog
          programId={programaId}
          editionId={edicionId}
          enrollmentPeriodId={dialogState.enrollmentPeriodId}
          editionStartDate={edition.data?.startDate}
          editionEndDate={edition.data?.endDate}
          onOpenChange={(open) => {
            if (!open) setDialogState(null)
          }}
        />
      )}
    </RoutePanel>
  )
}

function EnrollmentPeriodDialog({
  programId,
  editionId,
  enrollmentPeriodId,
  editionStartDate,
  editionEndDate,
  onOpenChange,
}: {
  programId: string
  editionId: string
  enrollmentPeriodId: string | null
  editionStartDate?: string
  editionEndDate?: string
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const isEditing = enrollmentPeriodId !== null
  const periodPath = {
    programId,
    editionId,
    enrollmentPeriodId: enrollmentPeriodId ?? "",
  }
  const period = useQuery({
    ...getEnrollmentPeriodOptions({ path: periodPath }),
    enabled: isEditing,
  })

  const invalidateList = () => queryClient.invalidateQueries({
    queryKey: listEnrollmentPeriodsQueryKey({ path: { programId, editionId } }),
  })
  const invalidatePeriod = () => {
    if (!enrollmentPeriodId) return
    queryClient.invalidateQueries({
      queryKey: getEnrollmentPeriodQueryKey({ path: periodPath }),
    })
  }
  const refresh = () => {
    invalidatePeriod()
    invalidateList()
  }
  const syncPeriod = (data: EnrollmentPeriodResponse) => {
    queryClient.setQueryData(
      getEnrollmentPeriodQueryKey({ path: periodPath }),
      data,
    )
    invalidateList()
  }

  const createPeriod = useMutation({
    ...createEnrollmentPeriodMutation(),
    onSuccess: () => {
      invalidateList()
      onOpenChange(false)
    },
    onError: showApiErrorToast,
  })
  const updatePeriod = useMutation({
    ...updateEnrollmentPeriodMutation(),
    onSuccess: () => {
      refresh()
      onOpenChange(false)
    },
    onError: showApiErrorToast,
  })
  const openPeriod = useMutation({ ...openEnrollmentPeriodMutation(), onSuccess: syncPeriod, onError: showApiErrorToast })
  const suspendPeriod = useMutation({ ...suspendEnrollmentPeriodMutation(), onSuccess: syncPeriod, onError: showApiErrorToast })
  const reopenPeriod = useMutation({ ...reopenEnrollmentPeriodMutation(), onSuccess: syncPeriod, onError: showApiErrorToast })
  const closePeriod = useMutation({ ...closeEnrollmentPeriodMutation(), onSuccess: syncPeriod, onError: showApiErrorToast })

  const resetErrors = () => {
    createPeriod.reset()
    updatePeriod.reset()
    openPeriod.reset()
    suspendPeriod.reset()
    reopenPeriod.reset()
    closePeriod.reset()
  }
  const isPending = createPeriod.isPending || updatePeriod.isPending ||
    openPeriod.isPending || suspendPeriod.isPending || reopenPeriod.isPending || closePeriod.isPending
  const status = period.data?.status
  const isClosed = status === "CLOSED"

  const form = useForm({
    defaultValues: { openDate: "", closeDate: "", notes: "" },
    validators: { onChange: isEditing ? updatePeriodSchema : createPeriodSchema },
    onSubmit: ({ value }) => {
      resetErrors()
      const body = { ...value, notes: value.notes.trim() || undefined }
      if (enrollmentPeriodId) {
        updatePeriod.mutate({ path: periodPath, body })
      } else {
        createPeriod.mutate({ path: { programId, editionId }, body })
      }
    },
  })

  useEffect(() => {
    if (!period.data) return
    form.reset({
      openDate: period.data.openDate ?? "",
      closeDate: period.data.closeDate ?? "",
      notes: period.data.notes ?? "",
    })
  }, [form, period.data])

  const dateBounds = createDateBounds(editionStartDate, editionEndDate)

  return (
    <Dialog open onOpenChange={(open) => { if (!isPending) onOpenChange(open) }}>
      <DialogContent className="max-h-[calc(100vh-2rem)] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2 pr-8">
            <DialogTitle>{isEditing ? "Editar período" : "Nuevo período de inscripción"}</DialogTitle>
            {isEditing && <PeriodStatusBadge status={status} />}
          </div>
          <DialogDescription>
            {isEditing
              ? "Actualizá las fechas, las observaciones o el estado del período."
              : `Definí un rango${editionStartDate && editionEndDate ? ` entre ${formatDate(editionStartDate)} y ${formatDate(editionEndDate)}` : ""}.`}
          </DialogDescription>
        </DialogHeader>

        {period.isPending && isEditing ? (
          <Card><CardContent className="text-sm text-muted-foreground">Cargando período…</CardContent></Card>
        ) : period.isError ? (
          <ApiErrorAlert error={period.error} fallback="No se pudo cargar el período." />
        ) : (
          <>
            {isClosed && (
              <Alert variant="destructive">
                <IconAlertTriangle />
                <AlertTitle>Período cerrado</AlertTitle>
                <AlertDescription>
                  El cierre es definitivo. Los datos quedan disponibles sólo para consulta.
                </AlertDescription>
              </Alert>
            )}

            {isEditing && status && status !== "CLOSED" && (
              <Card size="sm">
                <CardHeader>
                  <CardTitle>Estado del período</CardTitle>
                  <CardDescription>{statusHelpText(status)}</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-wrap gap-2">
                  {status === "SCHEDULED" && (
                    <Button type="button" size="sm" disabled={isPending} onClick={() => {
                      resetErrors()
                      openPeriod.mutate({ path: periodPath })
                    }}>
                      Abrir período
                    </Button>
                  )}
                  {status === "OPEN" && (
                    <Button type="button" size="sm" variant="outline" disabled={isPending} onClick={() => {
                      resetErrors()
                      suspendPeriod.mutate({ path: periodPath })
                    }}>
                      Suspender
                    </Button>
                  )}
                  {status === "SUSPENDED" && (
                    <Button type="button" size="sm" disabled={isPending} onClick={() => {
                      resetErrors()
                      reopenPeriod.mutate({ path: periodPath })
                    }}>
                      Reabrir
                    </Button>
                  )}
                  {(status === "OPEN" || status === "SUSPENDED") && (
                    <AlertDialog>
                      <AlertDialogTrigger render={<Button type="button" size="sm" variant="destructive" disabled={isPending} />}>
                        Cerrar definitivamente
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>¿Cerrar este período?</AlertDialogTitle>
                          <AlertDialogDescription>
                            El período no podrá volver a abrirse ni modificarse. Esta acción no se puede deshacer.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel disabled={closePeriod.isPending}>Cancelar</AlertDialogCancel>
                          <AlertDialogAction type="button" variant="destructive" disabled={closePeriod.isPending} onClick={() => {
                            resetErrors()
                            closePeriod.mutate({ path: periodPath })
                          }}>
                            {closePeriod.isPending ? "Cerrando…" : "Cerrar período"}
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  )}
                </CardContent>
              </Card>
            )}

            <form
              id="enrollment-period-form"
              className="grid gap-5"
              noValidate
              onSubmit={(event) => {
                event.preventDefault()
                event.stopPropagation()
                form.handleSubmit()
              }}
            >
              <div className="grid gap-4 sm:grid-cols-2">
                <form.Field name="openDate" children={(field) => {
                  const invalid = field.state.meta.isTouched && !field.state.meta.isValid
                  return (
                    <FormField label="Fecha de apertura" invalid={invalid} errors={field.state.meta.errors}>
                      <ProgramDatePicker
                        value={field.state.value}
                        onChange={field.handleChange}
                        disabled={dateBounds}
                        readOnly={isClosed || isPending}
                      />
                    </FormField>
                  )
                }} />
                <form.Subscribe selector={(state) => state.values.openDate} children={(openDate) => (
                  <form.Field name="closeDate" children={(field) => {
                    const invalid = field.state.meta.isTouched && !field.state.meta.isValid
                    return (
                      <FormField label="Fecha de cierre" invalid={invalid} errors={field.state.meta.errors}>
                        <ProgramDatePicker
                          value={field.state.value}
                          onChange={field.handleChange}
                          disabled={createDateBounds(openDate || editionStartDate, editionEndDate)}
                          readOnly={isClosed || isPending}
                        />
                      </FormField>
                    )
                  }} />
                )} />
              </div>
              <form.Field name="notes" children={(field) => {
                const invalid = field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <FormField label="Observaciones" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}>
                    <Textarea
                      id={field.name}
                      name={field.name}
                      rows={4}
                      maxLength={1000}
                      placeholder="Información administrativa opcional"
                      value={field.state.value}
                      disabled={isClosed || isPending}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                    />
                    <p className="text-right text-xs text-muted-foreground">{field.state.value.length}/1000</p>
                  </FormField>
                )
              }} />
            </form>
          </>
        )}

        <DialogFooter>
          <DialogClose render={<Button variant="outline" type="button" disabled={isPending} />}>
            {isEditing ? "Cerrar" : "Cancelar"}
          </DialogClose>
          {(!isEditing || (!period.isPending && !period.isError)) && (
            <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
              <Button
                type="submit"
                form="enrollment-period-form"
                disabled={!canSubmit || isSubmitting || isPending || isClosed}
              >
                {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear período"}
              </Button>
            )} />
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function PeriodStatusBadge({ status }: { status?: EnrollmentPeriodResponse["status"] }) {
  if (!status) return <Badge variant="outline">Sin estado</Badge>

  const styles: Record<PeriodStatus, string> = {
    SCHEDULED: "border-blue-500/30 bg-blue-500/10 text-blue-700 dark:text-blue-300",
    OPEN: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
    SUSPENDED: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300",
    CLOSED: "border-destructive/30 bg-destructive/10 text-destructive",
  }

  return <Badge variant="outline" className={styles[status]}>{periodStatusLabels[status]}</Badge>
}

function ApiErrorAlert({ error, fallback }: { error: ErrorResponse; fallback: string }) {
  return (
    <Alert variant="destructive">
      <IconAlertTriangle />
      <AlertTitle>{error.message ?? fallback}</AlertTitle>
      {error.fields && error.fields.length > 0 && (
        <AlertDescription>
          <ul className="list-disc pl-4">
            {error.fields.map((field, index) => (
              <li key={`${field.field ?? "field"}-${index}`}>{field.message}</li>
            ))}
          </ul>
        </AlertDescription>
      )}
    </Alert>
  )
}

function statusHelpText(status: PeriodStatus) {
  if (status === "SCHEDULED") {
    return "Podés abrirlo cuando la convocatoria esté activa y la fecha actual se encuentre dentro del rango."
  }
  if (status === "OPEN") {
    return "Está recibiendo postulaciones. Podés suspenderlo temporalmente o cerrarlo de forma definitiva."
  }
  if (status === "SUSPENDED") {
    return "No recibe postulaciones. Podés reabrirlo dentro de sus fechas o cerrarlo definitivamente."
  }
  return "El período está cerrado."
}

function formatDate(value?: string) {
  const date = value ? parseLocalDate(value) : undefined
  return date ? date.toLocaleDateString("es-AR") : "—"
}

function createDateBounds(
  minimum?: string,
  maximum?: string,
): ComponentProps<typeof ProgramDatePicker>["disabled"] {
  const bounds = []
  const minimumDate = minimum ? parseLocalDate(minimum) : undefined
  const maximumDate = maximum ? parseLocalDate(maximum) : undefined
  if (minimumDate) bounds.push({ before: minimumDate })
  if (maximumDate) bounds.push({ after: maximumDate })
  return bounds
}
