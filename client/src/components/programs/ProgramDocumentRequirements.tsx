import { IconAlertTriangle, IconFileDescription, IconPencil, IconPlus, IconRefresh } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"

import { DataPagination } from "@/components/DataPagination"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { FormField } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import {
  create5Mutation,
  delete5Mutation,
  list2Options,
  list2QueryKey,
  update5Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ErrorResponse, ProgramDocumentRequirementResponse } from "@/generated/types.gen"
import { zCreateProgramDocumentRequirementRequest, zUpdateProgramDocumentRequirementRequest } from "@/generated/zod.gen"

const PAGE_SIZE = 10
const createDocumentSchema = zCreateProgramDocumentRequirementRequest.extend({
  code: zCreateProgramDocumentRequirementRequest.shape.code.trim().min(1, "Ingresá el código del documento."),
  name: zCreateProgramDocumentRequirementRequest.shape.name.trim().min(1, "Ingresá el nombre del documento."),
  description: zCreateProgramDocumentRequirementRequest.shape.description.unwrap(),
  required: zCreateProgramDocumentRequirementRequest.shape.required.unwrap(),
})
const updateDocumentSchema = zUpdateProgramDocumentRequirementRequest.extend({
  code: zUpdateProgramDocumentRequirementRequest.shape.code.trim().min(1, "Ingresá el código del documento."),
  name: zUpdateProgramDocumentRequirementRequest.shape.name.trim().min(1, "Ingresá el nombre del documento."),
  description: zUpdateProgramDocumentRequirementRequest.shape.description.unwrap(),
  required: zUpdateProgramDocumentRequirementRequest.shape.required.unwrap(),
})

export function ProgramDocumentRequirements({ editionId, disabled }: { editionId: string; disabled: boolean }) {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [dialog, setDialog] = useState<{ requirement: ProgramDocumentRequirementResponse | null } | null>(null)
  const [catalogLocked, setCatalogLocked] = useState(false)
  const requirements = useQuery(list2Options({ path: { editionId } }))
  const refresh = () => queryClient.invalidateQueries({ queryKey: list2QueryKey({ path: { editionId } }) })
  const handleMutationError = (error: ErrorResponse) => {
    if (error.code === "PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED") setCatalogLocked(true)
  }
  const remove = useMutation({
    ...delete5Mutation(),
    onSuccess: refresh,
    onError: (error) => {
      handleMutationError(error)
      showApiErrorToast(error)
    },
  })
  const actionsDisabled = disabled || catalogLocked
  const totalItems = requirements.data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = requirements.data?.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE) ?? []

  return (
    <section className="mt-8 border-t pt-6" aria-labelledby="document-requirements-title">
      <div className="mb-5 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <h3 id="document-requirements-title" className="font-heading text-lg font-semibold">Documentación</h3>
          <p className="mt-1 text-sm text-muted-foreground">Definí qué documentos deben presentar las personas postulantes.</p>
          <p className="mt-2 text-xs text-muted-foreground">La documentación se puede configurar hasta que la edición reciba su primera solicitud.</p>
        </div>
        <Button type="button" disabled={actionsDisabled} onClick={() => setDialog({ requirement: null })}>
          <IconPlus />
          Nuevo documento requerido
        </Button>
      </div>

      {catalogLocked && (
        <Alert className="mb-5">
          <IconAlertTriangle />
          <AlertTitle>Documentación bloqueada</AlertTitle>
          <AlertDescription>La edición ya tiene solicitudes. Los documentos requeridos quedan disponibles sólo para consulta.</AlertDescription>
        </Alert>
      )}

      {requirements.dataUpdatedAt > 0 && (
        <div className="mb-3 flex justify-end">
          <Button size="xs" variant="ghost" disabled={requirements.isFetching} onClick={() => requirements.refetch()}>
            <IconRefresh className={requirements.isFetching ? "animate-spin" : undefined} />
            Actualizar documentos
          </Button>
        </div>
      )}

      {requirements.isPending ? (
        <Card><CardContent className="text-sm text-muted-foreground">Cargando documentos requeridos…</CardContent></Card>
      ) : requirements.isError ? (
        <Alert variant="destructive">
          <IconAlertTriangle />
          <AlertTitle>No se pudieron cargar los documentos requeridos</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-3">
            <span>{requirements.error.message ?? "Intentá nuevamente."}</span>
            <Button size="sm" variant="outline" onClick={() => requirements.refetch()}>Reintentar</Button>
          </AlertDescription>
        </Alert>
      ) : totalItems === 0 ? (
        <Card className="border-dashed bg-muted/20 text-center">
          <CardHeader>
            <IconFileDescription className="mx-auto size-8 text-muted-foreground" />
            <CardTitle>Sin documentos requeridos</CardTitle>
            <CardDescription>Agregá el primer documento solicitado para esta edición.</CardDescription>
          </CardHeader>
        </Card>
      ) : (
        <div className="overflow-hidden rounded-xl border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Documento</TableHead>
                <TableHead>Código</TableHead>
                <TableHead>Presentación</TableHead>
                <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {pageItems.map((requirement) => (
                <TableRow key={requirement.id}>
                  <TableCell className="max-w-md whitespace-normal">
                    <p className="font-medium">{requirement.name}</p>
                    {requirement.description && <p className="mt-1 text-xs text-muted-foreground">{requirement.description}</p>}
                  </TableCell>
                  <TableCell className="font-mono text-xs">{requirement.code}</TableCell>
                  <TableCell><Badge variant={requirement.required ? "default" : "secondary"}>{requirement.required ? "Obligatoria" : "Opcional"}</Badge></TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button
                        type="button"
                        size="icon-sm"
                        variant="ghost"
                        aria-label={`Editar documento ${requirement.name ?? "requerido"}`}
                        disabled={actionsDisabled || !requirement.id}
                        onClick={() => setDialog({ requirement })}
                      >
                        <IconPencil />
                      </Button>
                      {requirement.id && (
                        <DeleteConfirmationButton
                          description={`Se eliminará el documento requerido «${requirement.name ?? requirement.code}».`}
                          disabled={actionsDisabled || remove.isPending}
                          size="sm"
                          onConfirm={() => remove.mutateAsync({ path: { editionId, requirementId: requirement.id! } })}
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
        <DataPagination className="mt-4 rounded-xl border" page={currentPage} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} />
      )}

      {dialog && (
        <DocumentRequirementDialog
          editionId={editionId}
          requirement={dialog.requirement}
          disabled={actionsDisabled}
          onError={handleMutationError}
          onOpenChange={(open) => { if (!open) setDialog(null) }}
        />
      )}
    </section>
  )
}

function DocumentRequirementDialog({ editionId, requirement, disabled, onError, onOpenChange }: {
  editionId: string
  requirement: ProgramDocumentRequirementResponse | null
  disabled: boolean
  onError: (error: ErrorResponse) => void
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const isEditing = requirement !== null
  const onSuccess = () => {
    queryClient.invalidateQueries({ queryKey: list2QueryKey({ path: { editionId } }) })
    onOpenChange(false)
  }
  const create = useMutation({ ...create5Mutation(), onSuccess, onError })
  const update = useMutation({ ...update5Mutation(), onSuccess, onError })
  const isPending = create.isPending || update.isPending
  const mutationError = create.error ?? update.error
  const form = useForm({
    defaultValues: {
      code: requirement?.code ?? "",
      name: requirement?.name ?? "",
      description: requirement?.description ?? "",
      required: requirement?.required ?? true,
    },
    validators: { onChange: isEditing ? updateDocumentSchema : createDocumentSchema },
    onSubmit: ({ value }) => {
      if (disabled || isPending) return
      create.reset()
      update.reset()
      const body = {
        code: value.code.trim().toUpperCase(),
        name: value.name.trim(),
        description: value.description.trim() || undefined,
        required: value.required,
      }
      if (requirement?.id) {
        update.mutate({ path: { editionId, requirementId: requirement.id }, body })
      } else {
        create.mutate({ path: { editionId }, body })
      }
    },
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!isPending) onOpenChange(open) }}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar documento requerido" : "Nuevo documento requerido"}</DialogTitle>
          <DialogDescription>Indicá qué documento se solicitará en esta edición y si su presentación es obligatoria.</DialogDescription>
        </DialogHeader>
        {mutationError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>{mutationError.message ?? "No se pudo guardar el documento requerido."}</AlertTitle>
            {mutationError.fields && mutationError.fields.length > 0 && (
              <AlertDescription>
                <ul className="list-disc pl-4">{mutationError.fields.map((field, index) => <li key={`${field.field}-${index}`}>{field.message}</li>)}</ul>
              </AlertDescription>
            )}
          </Alert>
        )}
        <form id="document-requirement-form" className="grid gap-5" noValidate onSubmit={(event) => {
          event.preventDefault()
          event.stopPropagation()
          form.handleSubmit()
        }}>
          <form.Field name="code" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Código" htmlFor="document-code" invalid={invalid} errors={field.state.meta.errors}>
                <Input id="document-code" name={field.name} value={field.state.value} maxLength={50} placeholder="DNI_FRONT" autoComplete="off" spellCheck={false} disabled={disabled || isPending} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value.toUpperCase())} aria-invalid={invalid} aria-describedby="document-code-help" />
                <p id="document-code-help" className="text-xs text-muted-foreground">Debe ser único en la edición, comenzar con una letra y contener sólo letras, números o guion bajo.</p>
              </FormField>
            )
          }} />
          <form.Field name="name" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Nombre" htmlFor="document-name" invalid={invalid} errors={field.state.meta.errors}>
                <Input id="document-name" name={field.name} value={field.state.value} maxLength={150} placeholder="Frente del DNI" disabled={disabled || isPending} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} />
              </FormField>
            )
          }} />
          <form.Field name="description" children={(field) => {
            const invalid = field.state.meta.isTouched && !field.state.meta.isValid
            return (
              <FormField label="Descripción" htmlFor="document-description" invalid={invalid} errors={field.state.meta.errors}>
                <Textarea id="document-description" name={field.name} rows={3} value={field.state.value} maxLength={500} placeholder="Indicaciones para presentar el documento (opcional)" disabled={disabled || isPending} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} />
                <p className="text-right text-xs text-muted-foreground">{field.state.value.length}/500</p>
              </FormField>
            )
          }} />
          <form.Field name="required" children={(field) => (
            <div className="flex items-center gap-3">
              <Checkbox id="document-required" name={field.name} checked={field.state.value} disabled={disabled || isPending} onBlur={field.handleBlur} onCheckedChange={(checked) => field.handleChange(checked)} />
              <label htmlFor="document-required" className="text-sm font-medium">Presentación obligatoria</label>
            </div>
          )} />
        </form>
        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={isPending} />}>Cancelar</DialogClose>
          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
            <Button type="submit" form="document-requirement-form" disabled={disabled || !canSubmit || isSubmitting || isPending}>
              {isPending ? "Guardando…" : isEditing ? "Guardar cambios" : "Crear documento requerido"}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
