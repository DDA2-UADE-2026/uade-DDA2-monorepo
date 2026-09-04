import {
  IconAlertTriangle,
  IconGift,
  IconPencil,
  IconPlus,
  IconRefresh,
} from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"

import { DataPagination } from "@/components/DataPagination"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { benefitLabels, FormField, RoutePanel } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
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
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import {
  create5Mutation,
  delete5Mutation,
  findAll3Options,
  findAll3QueryKey,
  findById3Options,
  update5Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ErrorResponse, ProgramBenefitResponse } from "@/generated/types.gen"
import {
  zCreateProgramBenefitRequest,
  zUpdateProgramBenefitRequest,
} from "@/generated/zod.gen"

const PAGE_SIZE = 10
const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
})

const createBenefitSchema = zCreateProgramBenefitRequest.extend({
  benefitType: zCreateProgramBenefitRequest.shape.benefitType.unwrap(),
  description: zCreateProgramBenefitRequest.shape.description.unwrap(),
})

const updateBenefitSchema = zUpdateProgramBenefitRequest.extend({
  benefitType: zUpdateProgramBenefitRequest.shape.benefitType.unwrap(),
  description: zUpdateProgramBenefitRequest.shape.description.unwrap(),
})

type BenefitType = NonNullable<ProgramBenefitResponse["benefitType"]>
type BenefitFormValues = {
  benefitType: BenefitType
  description: string
  amount?: number
}
type DialogState = { benefit: ProgramBenefitResponse | null } | null

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/beneficios",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { edicionId } = Route.useParams()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [dialogState, setDialogState] = useState<DialogState>(null)

  const edition = useQuery(findById3Options({ path: { id: edicionId } }))
  const benefits = useQuery(findAll3Options({ path: { editionId: edicionId } }))
  const refresh = () => queryClient.invalidateQueries({
    queryKey: findAll3QueryKey({ path: { editionId: edicionId } }),
  })
  const remove = useMutation({ ...delete5Mutation(), onSuccess: refresh, onError: showApiErrorToast })

  const totalItems = benefits.data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = benefits.data?.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  ) ?? []
  const editionClosed = edition.data?.status === "CLOSED"
  const actionsDisabled = editionClosed || edition.isPending || edition.isError

  return (
    <RoutePanel>
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <h3 className="font-heading text-lg font-semibold">Beneficios</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Configurá las prestaciones disponibles para esta convocatoria.
          </p>
        </div>
        <Button
          onClick={() => setDialogState({ benefit: null })}
          disabled={actionsDisabled}
        >
          <IconPlus />
          Nuevo beneficio
        </Button>
      </div>

      {editionClosed && (
        <Alert variant="destructive" className="mb-5">
          <IconAlertTriangle />
          <AlertTitle>Convocatoria cerrada</AlertTitle>
          <AlertDescription>
            Los beneficios quedan disponibles sólo para consulta.
          </AlertDescription>
        </Alert>
      )}

      {benefits.dataUpdatedAt > 0 && (
        <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <span>
            Última actualización: {new Date(benefits.dataUpdatedAt).toLocaleTimeString("es-AR")}
          </span>
          <Button
            size="xs"
            variant="ghost"
            onClick={() => benefits.refetch()}
            disabled={benefits.isFetching}
          >
            <IconRefresh className={benefits.isFetching ? "animate-spin" : undefined} />
            Actualizar
          </Button>
        </div>
      )}

      {benefits.isPending ? (
        <Card>
          <CardContent className="text-sm text-muted-foreground">Cargando beneficios…</CardContent>
        </Card>
      ) : benefits.isError ? (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>No se pudieron cargar los beneficios</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-3">
            <span>{benefits.error.message ?? "Intentá nuevamente."}</span>
            <Button size="sm" variant="outline" onClick={() => benefits.refetch()}>
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : totalItems === 0 ? (
        <Card className="border-dashed bg-muted/20 text-center">
          <CardHeader>
            <IconGift className="mx-auto size-8 text-muted-foreground" />
            <CardTitle>Sin beneficios configurados</CardTitle>
            <CardDescription>
              Agregá el primer beneficio para esta convocatoria.
            </CardDescription>
          </CardHeader>
        </Card>
      ) : (
        <div className="overflow-hidden rounded-xl border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tipo</TableHead>
                <TableHead className="hidden md:table-cell">Descripción</TableHead>
                <TableHead>Monto</TableHead>
                <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {pageItems.map((benefit) => (
                <TableRow key={benefit.id}>
                  <TableCell className="font-medium">
                    {benefit.benefitType ? benefitLabels[benefit.benefitType] : "—"}
                  </TableCell>
                  <TableCell className="hidden max-w-md truncate text-muted-foreground md:table-cell">
                    {benefit.description || "—"}
                  </TableCell>
                  <TableCell>
                    {benefit.amount == null ? "—" : currencyFormatter.format(benefit.amount)}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button
                        type="button"
                        size="icon-sm"
                        variant="ghost"
                        aria-label="Editar beneficio"
                        disabled={actionsDisabled}
                        onClick={() => setDialogState({ benefit })}
                      >
                        <IconPencil />
                      </Button>
                      {benefit.id && (
                        <DeleteConfirmationButton
                          description="Se eliminará este beneficio."
                          disabled={actionsDisabled || remove.isPending}
                          size="sm"
                          onConfirm={() => {
                            remove.reset()
                            return remove.mutateAsync({
                              path: { editionId: edicionId, benefitId: benefit.id! },
                            })
                          }}
                        />
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {!benefits.isPending && !benefits.isError && totalItems > 0 && (
        <DataPagination
          className="mt-4 rounded-xl border"
          page={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
        />
      )}

      {dialogState && (
        <BenefitDialog
          editionId={edicionId}
          benefit={dialogState.benefit}
          disabled={actionsDisabled}
          onOpenChange={(open) => {
            if (!open) setDialogState(null)
          }}
        />
      )}
    </RoutePanel>
  )
}

function BenefitDialog({
  editionId,
  benefit,
  disabled,
  onOpenChange,
}: {
  editionId: string
  benefit: ProgramBenefitResponse | null
  disabled: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const isEditing = benefit !== null
  const refresh = () => queryClient.invalidateQueries({
    queryKey: findAll3QueryKey({ path: { editionId } }),
  })

  const create = useMutation({
    ...create5Mutation(),
    onSuccess: () => {
      refresh()
      onOpenChange(false)
    },
  })
  const update = useMutation({
    ...update5Mutation(),
    onSuccess: () => {
      refresh()
      onOpenChange(false)
    },
  })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error
  const defaultValues: BenefitFormValues = {
    benefitType: (benefit?.benefitType ?? "FOOD_ASSISTANCE") as BenefitType,
    description: benefit?.description ?? "",
    amount: benefit?.amount,
  }

  const form = useForm({
    defaultValues,
    validators: {
      onChange: isEditing ? updateBenefitSchema : createBenefitSchema,
    },
    onSubmit: ({ value }) => {
      create.reset()
      update.reset()
      const body = {
        ...value,
        description: value.description.trim() || undefined,
      }
      if (benefit?.id) {
        update.mutate({
          path: { editionId, benefitId: benefit.id },
          body,
        })
      } else {
        create.mutate({ path: { editionId }, body })
      }
    },
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!isPending) onOpenChange(open) }}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar beneficio" : "Nuevo beneficio"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Modificá la prestación configurada para esta convocatoria."
              : "Definí una nueva prestación para las personas beneficiarias."}
          </DialogDescription>
        </DialogHeader>

        {mutationError && (
          <ApiErrorAlert error={mutationError} fallback="No se pudo guardar el beneficio." />
        )}

        <form
          id="benefit-form"
          className="grid gap-5"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            event.stopPropagation()
            form.handleSubmit()
          }}
        >
          <form.Field name="benefitType" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Tipo" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}>
                <Select
                  name={field.name}
                  value={field.state.value}
                  disabled={disabled || isPending}
                  onValueChange={(nextType) => field.handleChange(nextType as BenefitType)}
                >
                  <SelectTrigger id={field.name} className="w-full" onBlur={field.handleBlur} aria-invalid={invalid}>
                    <SelectValue>{benefitLabels[field.state.value]}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(benefitLabels).map(([key, label]) => (
                      <SelectItem key={key} value={key}>{label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormField>
            )
          }} />

          <form.Field name="amount" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Monto" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}>
                <Input
                  id={field.name}
                  name={field.name}
                  type="number"
                  min="0"
                  step="0.01"
                  value={field.state.value ?? ""}
                  disabled={disabled || isPending}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(
                    event.target.value === "" ? undefined : Number(event.target.value),
                  )}
                  aria-invalid={invalid}
                />
              </FormField>
            )
          }} />

          <form.Field name="description" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Descripción" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}>
                <Textarea
                  id={field.name}
                  name={field.name}
                  rows={4}
                  maxLength={500}
                  placeholder="Información complementaria opcional"
                  value={field.state.value}
                  disabled={disabled || isPending}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(event.target.value)}
                  aria-invalid={invalid}
                />
                <p className="text-right text-xs text-muted-foreground">{field.state.value.length}/500</p>
              </FormField>
            )
          }} />
        </form>

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={isPending} />}>
            Cancelar
          </DialogClose>
          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
            <Button
              type="submit"
              form="benefit-form"
              disabled={disabled || !canSubmit || isSubmitting || isPending}
            >
              {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear beneficio"}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function ApiErrorAlert({ error, fallback }: { error: ErrorResponse; fallback: string }) {
  return (
    <Alert variant="destructive" className="mb-4">
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
