import {
  IconAlertTriangle,
  IconChecklist,
  IconPencil,
  IconPlus,
  IconRefresh,
} from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { ProgramDocumentRequirements } from "@/components/programs/ProgramDocumentRequirements"
import {
  FormField,
  RoutePanel,
  formatRequirementValue,
  requirementLabels,
  requirementValueError,
  type RequirementType,
} from "@/components/programs/ProgramRouteUi"
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
import { InputGroup, InputGroupAddon, InputGroupInput, InputGroupText } from "@/components/ui/input-group"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import {
  create5Mutation,
  delete4Mutation,
  findAll2Options,
  findAll2QueryKey,
  findById3Options,
  update4Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ErrorResponse, ProgramRequirementResponse } from "@/generated/types.gen"
import {
  zCreateProgramRequirementRequest,
  zUpdateProgramRequirementRequest,
} from "@/generated/zod.gen"

const PAGE_SIZE = 10

function validateRequirementValue(
  value: { type: RequirementType; value: string },
  context: z.RefinementCtx,
) {
  const message = requirementValueError(value.type, value.value)
  if (message) context.addIssue({ code: "custom", path: ["value"], message })
}

const createRequirementSchema = zCreateProgramRequirementRequest.extend({
  type: zCreateProgramRequirementRequest.shape.type.unwrap(),
  description: zCreateProgramRequirementRequest.shape.description.unwrap(),
}).superRefine(validateRequirementValue)

const updateRequirementSchema = zUpdateProgramRequirementRequest.extend({
  type: zUpdateProgramRequirementRequest.shape.type.unwrap(),
  description: zUpdateProgramRequirementRequest.shape.description.unwrap(),
}).superRefine(validateRequirementValue)

const defaultRequirementValue = (type: RequirementType) => type === "HAS_CHILDREN" ? "true" : ""

type DialogState = { requirement: ProgramRequirementResponse | null } | null

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId/requisitos",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { edicionId } = Route.useParams()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [dialogState, setDialogState] = useState<DialogState>(null)

  const edition = useQuery(findById3Options({ path: { id: edicionId } }))
  const requirements = useQuery(findAll2Options({ path: { editionId: edicionId } }))
  const refresh = () => queryClient.invalidateQueries({
    queryKey: findAll2QueryKey({ path: { editionId: edicionId } }),
  })
  const remove = useMutation({ ...delete4Mutation(), onSuccess: refresh, onError: showApiErrorToast })

  const totalItems = requirements.data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = requirements.data?.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  ) ?? []
  const editionClosed = edition.data?.status === "CLOSED"
  const actionsDisabled = editionClosed || edition.isPending || edition.isError

  return (
    <RoutePanel>
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <h3 className="font-heading text-lg font-semibold">Requisitos</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Definí las condiciones que deben cumplir quienes se postulen.
          </p>
        </div>
        <Button
          onClick={() => setDialogState({ requirement: null })}
          disabled={actionsDisabled}
        >
          <IconPlus />
          Nuevo requisito
        </Button>
      </div>

      {editionClosed && (
        <Alert variant="destructive" className="mb-5">
          <IconAlertTriangle />
          <AlertTitle>Edición cerrada</AlertTitle>
          <AlertDescription>
            Los requisitos y la documentación quedan disponibles sólo para consulta.
          </AlertDescription>
        </Alert>
      )}

      {requirements.dataUpdatedAt > 0 && (
        <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <span>
            Última actualización: {new Date(requirements.dataUpdatedAt).toLocaleTimeString("es-AR")}
          </span>
          <Button
            size="xs"
            variant="ghost"
            onClick={() => requirements.refetch()}
            disabled={requirements.isFetching}
          >
            <IconRefresh className={requirements.isFetching ? "animate-spin" : undefined} />
            <span className="sr-only sm:not-sr-only">Refrescar</span>
          </Button>
        </div>
      )}

      {requirements.isPending ? (
        <Card>
          <CardContent className="text-sm text-muted-foreground">Cargando requisitos…</CardContent>
        </Card>
      ) : requirements.isError ? (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>No se pudieron cargar los requisitos</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-3">
            <span>{requirements.error.message ?? "Intentá nuevamente."}</span>
            <Button size="sm" variant="outline" onClick={() => requirements.refetch()}>
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : totalItems === 0 ? (
        <Card className="border-dashed bg-muted/20 text-center">
          <CardHeader>
            <IconChecklist className="mx-auto size-8 text-muted-foreground" />
            <CardTitle>Sin requisitos configurados</CardTitle>
            <CardDescription>
              Agregá el primer requisito para esta edición.
            </CardDescription>
          </CardHeader>
        </Card>
      ) : (
        <div className="overflow-hidden rounded-xl border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tipo</TableHead>
                <TableHead>Valor</TableHead>
                <TableHead className="hidden md:table-cell">Descripción</TableHead>
                <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {pageItems.map((requirement) => (
                <TableRow key={requirement.id}>
                  <TableCell className="font-medium">
                    {requirement.type ? requirementLabels[requirement.type] : "—"}
                  </TableCell>
                  <TableCell>{formatRequirementValue(requirement.type, requirement.value)}</TableCell>
                  <TableCell className="hidden max-w-md truncate text-muted-foreground md:table-cell">
                    {requirement.description || "—"}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button
                        type="button"
                        size="icon-sm"
                        variant="ghost"
                        aria-label="Editar requisito"
                        disabled={actionsDisabled}
                        onClick={() => setDialogState({ requirement })}
                      >
                        <IconPencil />
                      </Button>
                      {requirement.id && (
                        <DeleteConfirmationButton
                          description="Se eliminará este requisito."
                          disabled={actionsDisabled || remove.isPending}
                          size="sm"
                          onConfirm={() => {
                            remove.reset()
                            return remove.mutateAsync({
                              path: { editionId: edicionId, requirementId: requirement.id! },
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

      {!requirements.isPending && !requirements.isError && totalItems > 0 && (
        <DataPagination
          className="mt-4 rounded-xl border"
          page={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
        />
      )}

      <ProgramDocumentRequirements key={edicionId} editionId={edicionId} disabled={actionsDisabled} />

      {dialogState && (
        <RequirementDialog
          editionId={edicionId}
          requirement={dialogState.requirement}
          disabled={actionsDisabled}
          onOpenChange={(open) => {
            if (!open) setDialogState(null)
          }}
        />
      )}
    </RoutePanel>
  )
}

function RequirementDialog({
  editionId,
  requirement,
  disabled,
  onOpenChange,
}: {
  editionId: string
  requirement: ProgramRequirementResponse | null
  disabled: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const isEditing = requirement !== null
  const refresh = () => queryClient.invalidateQueries({
    queryKey: findAll2QueryKey({ path: { editionId } }),
  })

  const create = useMutation({
    ...create5Mutation(),
    onSuccess: () => {
      refresh()
      onOpenChange(false)
    },
  })
  const update = useMutation({
    ...update4Mutation(),
    onSuccess: () => {
      refresh()
      onOpenChange(false)
    },
  })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error

  const form = useForm({
    defaultValues: {
      type: (requirement?.type ?? "MIN_AGE") as RequirementType,
      // El backend acepta "TRUE"/"False"; se normaliza para que el selector Sí/No lo reconozca.
      value: requirement?.type === "HAS_CHILDREN" ? (requirement.value ?? "").trim().toLowerCase() : requirement?.value ?? "",
      description: requirement?.description ?? "",
    },
    validators: {
      onChange: isEditing ? updateRequirementSchema : createRequirementSchema,
    },
    onSubmit: ({ value }) => {
      create.reset()
      update.reset()
      const body = {
        ...value,
        value: value.value.trim(),
        description: value.description.trim() || undefined,
      }
      if (requirement?.id) {
        update.mutate({
          path: { editionId, requirementId: requirement.id },
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
          <DialogTitle>{isEditing ? "Editar requisito" : "Nuevo requisito"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Modificá la condición requerida para esta edición."
              : "Definí una nueva condición para las personas postulantes."}
          </DialogDescription>
        </DialogHeader>

        {mutationError && (
          <ApiErrorAlert error={mutationError} fallback="No se pudo guardar el requisito." />
        )}

        <form
          id="requirement-form"
          className="grid gap-5"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            event.stopPropagation()
            form.handleSubmit()
          }}
        >
          <form.Field name="type" listeners={{
            // Cada tipo usa otro formato de valor: al cambiarlo se arranca de cero.
            onChange: ({ value }) => form.setFieldValue("value", defaultRequirementValue(value)),
          }} children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Tipo" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}>
                <Select
                  name={field.name}
                  value={field.state.value}
                  disabled={disabled || isPending}
                  onValueChange={(nextType) => field.handleChange(nextType as RequirementType)}
                >
                  <SelectTrigger id={field.name} className="w-full" onBlur={field.handleBlur} aria-invalid={invalid}>
                    <SelectValue>{requirementLabels[field.state.value]}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(requirementLabels).map(([key, label]) => (
                      <SelectItem key={key} value={key}>{label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormField>
            )
          }} />

          <form.Subscribe selector={(state) => state.values.type} children={(type) => (
            <form.Field name="value" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <FormField label={valueLabels[type]} htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}>
                  <RequirementValueInput
                    id={field.name}
                    type={type}
                    value={field.state.value}
                    disabled={disabled || isPending}
                    invalid={invalid}
                    onBlur={field.handleBlur}
                    onChange={field.handleChange}
                  />
                </FormField>
              )
            }} />
          )} />

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
              form="requirement-form"
              disabled={disabled || !canSubmit || isSubmitting || isPending}
            >
              {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear requisito"}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

const valueLabels: Record<RequirementType, string> = {
  MIN_AGE: "Edad mínima",
  MAX_INCOME: "Ingreso máximo mensual",
  RESIDENCY_YEARS: "Años de residencia mínimos",
  HAS_CHILDREN: "¿Se requiere tener hijos?",
}

function RequirementValueInput({ id, type, value, disabled, invalid, onBlur, onChange }: {
  id: string
  type: RequirementType
  value: string
  disabled: boolean
  invalid: boolean
  onBlur: () => void
  onChange: (value: string) => void
}) {
  if (type === "HAS_CHILDREN") {
    return (
      <ToggleGroup
        id={id}
        variant="outline"
        aria-label={valueLabels[type]}
        aria-invalid={invalid}
        disabled={disabled}
        value={[value]}
        onValueChange={(next: string[]) => { if (next[0]) onChange(next[0]); onBlur() }}
      >
        <ToggleGroupItem value="true">Sí, debe tener hijos</ToggleGroupItem>
        <ToggleGroupItem value="false">No, no debe tener hijos</ToggleGroupItem>
      </ToggleGroup>
    )
  }

  const isIncome = type === "MAX_INCOME"
  const preview = isIncome && !requirementValueError(type, value) ? formatRequirementValue(type, value) : null
  return (
    <>
      <InputGroup>
        {isIncome && <InputGroupAddon><InputGroupText>$</InputGroupText></InputGroupAddon>}
        <InputGroupInput
          id={id}
          name={id}
          inputMode={isIncome ? "decimal" : "numeric"}
          autoComplete="off"
          placeholder={isIncome ? "150000" : type === "MIN_AGE" ? "18" : "2"}
          value={value}
          disabled={disabled}
          aria-invalid={invalid}
          onBlur={onBlur}
          // Solo dígitos (y un separador decimal en el ingreso); la coma se convierte en punto para el backend.
          onChange={(event) => onChange(isIncome
            ? event.target.value.replace(",", ".").replace(/[^\d.]/g, "")
            : event.target.value.replace(/\D/g, ""))}
        />
        {!isIncome && <InputGroupAddon align="inline-end"><InputGroupText>años</InputGroupText></InputGroupAddon>}
      </InputGroup>
      {isIncome && <p className="text-xs text-muted-foreground">{preview ? `Se mostrará como ${preview}.` : "Monto en pesos, sin separador de miles. Podés usar hasta dos decimales."}</p>}
    </>
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
