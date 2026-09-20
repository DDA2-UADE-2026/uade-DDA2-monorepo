import { IconAlertTriangle, IconCheck, IconDownload, IconEye } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useEffect, useMemo, useState } from "react"

import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { UserAvatar } from "@/components/UserAvatar"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import {
  content1Options,
  get3QueryKey,
  list7QueryKey,
  reviewMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ApplicationDocumentResponse } from "@/generated/types.gen"
import { formatApplicationDateTime, formatDocumentFileSize } from "@/lib/application-flow"

type ReviewDecision = "VALID" | "OBSERVED"

/**
 * Tabla de entregas de una solicitud con la vista previa y la revisión resueltas en diálogos.
 * La comparten la pantalla de revisión documental y el detalle administrativo de la solicitud.
 */
export function AdminDocumentsTable({
  applicationId,
  documents,
}: {
  applicationId: string
  documents: ApplicationDocumentResponse[]
}) {
  const [previewDocument, setPreviewDocument] = useState<ApplicationDocumentResponse | null>(null)
  const [reviewDocument, setReviewDocument] = useState<ApplicationDocumentResponse | null>(null)

  return (
    <>
      <div className="overflow-x-auto rounded-xl border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Requisito</TableHead>
              <TableHead>Archivo</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="hidden md:table-cell">Validado por</TableHead>
              <TableHead className="hidden lg:table-cell">Revisión</TableHead>
              <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {documents.map((document) => (
              <TableRow key={document.id ?? document.documentId}>
                <TableCell className="min-w-52 align-top">
                  <p className="font-medium">{document.requirementName ?? "Documento"}</p>
                  <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                    {document.requirementCode && <code>{document.requirementCode}</code>}
                    {document.required && <span>Obligatorio</span>}
                  </div>
                </TableCell>
                <TableCell className="min-w-48 align-top">
                  <p className="max-w-64 truncate">{document.originalName ?? "Archivo sin nombre"}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {formatDocumentFileSize(document.sizeBytes)}
                    {document.uploadedAt ? ` · ${formatApplicationDateTime(document.uploadedAt)}` : ""}
                  </p>
                </TableCell>
                <TableCell className="align-top">
                  <AdminDocumentStatusBadge status={document.status} />
                </TableCell>
                <TableCell className="hidden min-w-44 align-top md:table-cell">
                  <AdminDocumentReviewer document={document} />
                </TableCell>
                <TableCell className="hidden max-w-72 align-top lg:table-cell">
                  {document.observation ? (
                    <p className="line-clamp-2 text-sm text-muted-foreground" title={document.observation}>
                      {document.observation}
                    </p>
                  ) : document.reviewedAt ? (
                    <p className="text-xs text-muted-foreground">{formatApplicationDateTime(document.reviewedAt)}</p>
                  ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                  )}
                </TableCell>
                <TableCell className="align-top">
                  <div className="flex justify-end gap-1">
                    {document.id && (
                      <Button
                        type="button"
                        size="icon-sm"
                        variant="ghost"
                        aria-label={`Ver ${document.originalName ?? "documento"}`}
                        onClick={() => setPreviewDocument(document)}
                      >
                        <IconEye />
                      </Button>
                    )}
                    {document.id && document.status === "PENDING" && (
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => setReviewDocument(document)}
                      >
                        Revisar
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {previewDocument?.id && (
        <AdminDocumentPreviewDialog
          applicationId={applicationId}
          document={{ ...previewDocument, id: previewDocument.id }}
          onOpenChange={(open) => {
            if (!open) setPreviewDocument(null)
          }}
        />
      )}

      {reviewDocument?.id && (
        <AdminDocumentReviewDialog
          applicationId={applicationId}
          document={{ ...reviewDocument, id: reviewDocument.id }}
          onOpenChange={(open) => {
            if (!open) setReviewDocument(null)
          }}
        />
      )}
    </>
  )
}

/** Quién resolvió la entrega: sin revisión todavía no hay persona que mostrar. */
function AdminDocumentReviewer({ document }: { document: ApplicationDocumentResponse }) {
  const { reviewedByUserId: id, reviewedByUserName: name, reviewedByUserEmail: email } = document

  if (id == null && !name) return <span className="text-sm text-muted-foreground">—</span>

  return (
    <div className="flex min-w-0 items-center gap-2">
      <UserAvatar user={{ id, name, email }} size="sm" />
      <div className="min-w-0">
        <p className="max-w-44 truncate text-sm font-medium">{name || `Usuario #${id}`}</p>
        <p className="max-w-44 truncate text-xs text-muted-foreground">
          {email || (id != null ? `ID interno ${id}` : "—")}
        </p>
      </div>
    </div>
  )
}

export function AdminDocumentStatusBadge({ status }: { status?: ApplicationDocumentResponse["status"] }) {
  if (!status) return <Badge variant="outline">Sin estado</Badge>

  const config = {
    PENDING: { label: "Pendiente", className: "border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/50 dark:text-amber-300" },
    VALID: { label: "Válido", className: "border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/50 dark:text-emerald-300" },
    OBSERVED: { label: "Observado", className: "border-red-300 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950/50 dark:text-red-300" },
  } as const

  return <Badge variant="outline" className={config[status].className}>{config[status].label}</Badge>
}

export function AdminDocumentPreviewDialog({
  applicationId,
  document,
  onOpenChange,
}: {
  applicationId: string
  document: ApplicationDocumentResponse & { id: string }
  onOpenChange: (open: boolean) => void
}) {
  const content = useQuery(content1Options({
    path: { applicationId, applicationDocumentId: document.id },
  }))
  const objectUrl = useMemo(
    () => content.data ? URL.createObjectURL(content.data) : null,
    [content.data],
  )

  useEffect(() => () => {
    if (objectUrl) URL.revokeObjectURL(objectUrl)
  }, [objectUrl])

  const download = () => {
    if (!objectUrl) return
    const link = window.document.createElement("a")
    link.href = objectUrl
    link.download = document.originalName ?? "documento"
    link.click()
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-5xl">
        <DialogHeader>
          <DialogTitle className="truncate pr-8">{document.originalName ?? "Documento"}</DialogTitle>
          <DialogDescription>{document.requirementName ?? "Documento de la solicitud"}</DialogDescription>
        </DialogHeader>

        {content.isPending ? (
          <div className="flex h-[60vh] items-center justify-center rounded-lg border bg-muted/30 text-sm text-muted-foreground">
            Cargando vista previa…
          </div>
        ) : content.isError ? (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No se pudo abrir el documento</AlertTitle>
            <AlertDescription className="flex flex-col items-start gap-3">
              <span>{content.error.message ?? "Intentá nuevamente."}</span>
              <Button type="button" size="sm" variant="outline" onClick={() => content.refetch()}>Reintentar</Button>
            </AlertDescription>
          </Alert>
        ) : objectUrl ? (
          <iframe
            className="h-[65vh] w-full rounded-lg border bg-white"
            src={objectUrl}
            title={document.originalName ?? "Vista previa del documento"}
          />
        ) : null}

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" />}>Cerrar</DialogClose>
          <Button type="button" disabled={!objectUrl} onClick={download}>
            <IconDownload />
            Descargar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function AdminDocumentReviewDialog({
  applicationId,
  document,
  onOpenChange,
}: {
  applicationId: string
  document: ApplicationDocumentResponse & { id: string }
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const [decision, setDecision] = useState<ReviewDecision>("VALID")
  const [observation, setObservation] = useState("")
  const [submitted, setSubmitted] = useState(false)
  const observationMissing = decision === "OBSERVED" && observation.trim().length === 0
  const review = useMutation({
    ...reviewMutation(),
    onSuccess: async () => {
      // La revisión cambia la documentación pendiente que expone el detalle administrativo.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: list7QueryKey({ path: { applicationId } }) }),
        queryClient.invalidateQueries({ queryKey: get3QueryKey({ path: { id: applicationId } }) }),
      ])
      onOpenChange(false)
    },
    onError: showApiErrorToast,
  })

  const submitReview = () => {
    setSubmitted(true)
    if (observationMissing) return

    review.mutate({
      path: { applicationId, applicationDocumentId: document.id },
      body: {
        status: decision,
        observation: decision === "OBSERVED" ? observation.trim() : undefined,
      },
    })
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!review.isPending) onOpenChange(open) }}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Revisar documento</DialogTitle>
          <DialogDescription>
            Resolvé {document.originalName ?? document.requirementName ?? "el documento"}. Esta decisión no se puede repetir sobre la misma entrega.
          </DialogDescription>
        </DialogHeader>

        {review.isError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No se pudo guardar la revisión</AlertTitle>
            <AlertDescription>{review.error.message ?? "Verificá el estado de la solicitud e intentá nuevamente."}</AlertDescription>
          </Alert>
        )}

        <div className="grid grid-cols-2 gap-3">
          <Button
            type="button"
            variant={decision === "VALID" ? "default" : "outline"}
            aria-pressed={decision === "VALID"}
            disabled={review.isPending}
            onClick={() => setDecision("VALID")}
          >
            <IconCheck />
            Validar
          </Button>
          <Button
            type="button"
            variant={decision === "OBSERVED" ? "destructive" : "outline"}
            aria-pressed={decision === "OBSERVED"}
            disabled={review.isPending}
            onClick={() => setDecision("OBSERVED")}
          >
            <IconAlertTriangle />
            Observar
          </Button>
        </div>

        {decision === "OBSERVED" && (
          <div>
            <label className="text-sm font-medium" htmlFor="review-observation">Observación</label>
            <Textarea
              id="review-observation"
              className="mt-2"
              rows={5}
              maxLength={1000}
              value={observation}
              disabled={review.isPending}
              aria-invalid={submitted && observationMissing}
              placeholder="Indicá qué debe corregir la persona solicitante."
              onChange={(event) => setObservation(event.target.value)}
            />
            <div className="mt-1 flex justify-between gap-3 text-xs">
              <span className={submitted && observationMissing ? "text-destructive" : "text-muted-foreground"}>
                {submitted && observationMissing ? "La observación es obligatoria." : "Se mostrará a la persona solicitante."}
              </span>
              <span className="text-muted-foreground">{observation.length}/1000</span>
            </div>
          </div>
        )}

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={review.isPending} />}>
            Cancelar
          </DialogClose>
          <Button type="button" disabled={review.isPending} onClick={submitReview}>
            {review.isPending ? "Guardando…" : decision === "VALID" ? "Confirmar validación" : "Confirmar observación"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
