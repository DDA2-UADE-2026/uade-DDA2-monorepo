import {
  IconAlertTriangle,
  IconCheck,
  IconDownload,
  IconEye,
  IconFileCheck,
  IconFileDescription,
  IconRefresh,
  IconSearch,
} from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect, useMemo, useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
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
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Input } from "@/components/ui/input"
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
  list5Options,
  list5QueryKey,
  reviewMutation,
} from "@/generated/@tanstack/react-query.gen"
import type {
  ApplicationDocumentResponse,
  ErrorResponse,
} from "@/generated/types.gen"

const PAGE_SIZE = 10
const uuidSchema = z.uuid()
const searchSchema = z.object({
  solicitudId: z.string().catch("").default(""),
})

type ReviewDecision = "VALID" | "OBSERVED"

export const Route = createFileRoute("/_app/gestion/documentos")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const searchedApplicationId = solicitudId.trim()
  const hasValidApplicationId = uuidSchema.safeParse(searchedApplicationId).success
  const [applicationIdDraft, setApplicationIdDraft] = useState<string | null>(null)
  const [searchError, setSearchError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [previewDocument, setPreviewDocument] = useState<ApplicationDocumentResponse | null>(null)
  const [reviewDocument, setReviewDocument] = useState<ApplicationDocumentResponse | null>(null)

  const documents = useQuery({
    ...list5Options({ path: { applicationId: searchedApplicationId } }),
    enabled: hasValidApplicationId,
  })

  const applicationIdInput = applicationIdDraft ?? searchedApplicationId

  const totalItems = documents.data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = documents.data?.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  ) ?? []

  const submitSearch = () => {
    const nextApplicationId = applicationIdInput.trim()
    if (!uuidSchema.safeParse(nextApplicationId).success) {
      setSearchError("Ingresá un UUID de solicitud válido.")
      return
    }

    setSearchError(null)
    setPage(1)
    setApplicationIdDraft(null)
    if (nextApplicationId === searchedApplicationId) {
      documents.refetch()
      return
    }

    navigate({ search: { solicitudId: nextApplicationId } })
  }

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Documentos" }]} />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-4 sm:mx-4 lg:py-6">
            <div className="mx-auto w-full max-w-7xl space-y-5">
              <div>
                <h1 className="font-heading text-xl font-semibold">Revisión de documentos</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  Consultá las entregas de una solicitud y resolvé los documentos pendientes.
                </p>
              </div>

              <form
                className="rounded-xl border bg-card p-4"
                noValidate
                onSubmit={(event) => {
                  event.preventDefault()
                  submitSearch()
                }}
              >
                <label className="text-sm font-medium" htmlFor="application-id">
                  ID de solicitud
                </label>
                <div className="mt-2 flex flex-col gap-2 sm:flex-row">
                  <Input
                    id="application-id"
                    className="font-mono"
                    value={applicationIdInput}
                    placeholder="00000000-0000-0000-0000-000000000000"
                    autoComplete="off"
                    spellCheck={false}
                    aria-invalid={Boolean(searchError)}
                    onChange={(event) => {
                      setApplicationIdDraft(event.target.value)
                      if (searchError) setSearchError(null)
                    }}
                  />
                  <Button type="submit" className="sm:min-w-28">
                    <IconSearch />
                    Buscar
                  </Button>
                </div>
                <p className={searchError ? "mt-2 text-sm text-destructive" : "mt-2 text-xs text-muted-foreground"}>
                  {searchError ?? "Usá el UUID interno de la solicitud."}
                </p>
              </form>

              {!searchedApplicationId ? (
                <Empty className="min-h-72 border">
                  <EmptyHeader>
                    <EmptyMedia variant="icon"><IconFileCheck /></EmptyMedia>
                    <EmptyTitle>Buscá una solicitud</EmptyTitle>
                    <EmptyDescription>
                      Ingresá su UUID para consultar los documentos entregados y su estado de revisión.
                    </EmptyDescription>
                  </EmptyHeader>
                </Empty>
              ) : !hasValidApplicationId ? (
                <Alert variant="destructive">
                  <IconAlertTriangle />
                  <AlertTitle>ID de solicitud inválido</AlertTitle>
                  <AlertDescription>Corregí el UUID para poder consultar sus documentos.</AlertDescription>
                </Alert>
              ) : documents.isPending ? (
                <div className="flex min-h-48 items-center justify-center text-sm text-muted-foreground">
                  Cargando documentos…
                </div>
              ) : documents.isError ? (
                <DocumentsQueryError error={documents.error} retry={() => documents.refetch()} />
              ) : totalItems === 0 ? (
                <Empty className="min-h-72 border">
                  <EmptyHeader>
                    <EmptyMedia variant="icon"><IconFileDescription /></EmptyMedia>
                    <EmptyTitle>Sin documentos entregados</EmptyTitle>
                    <EmptyDescription>
                      La solicitud seleccionada todavía no tiene archivos para revisar.
                    </EmptyDescription>
                  </EmptyHeader>
                </Empty>
              ) : (
                <>
                  <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
                    <div className="min-w-0">
                      <p className="text-sm font-medium">Solicitud</p>
                      <p className="truncate font-mono text-xs text-muted-foreground">
                        {searchedApplicationId}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      {documents.dataUpdatedAt > 0 && (
                        <span className="hidden sm:inline">
                          Actualizado {new Date(documents.dataUpdatedAt).toLocaleTimeString("es-AR")}
                        </span>
                      )}
                      <Button
                        type="button"
                        size="xs"
                        variant="ghost"
                        disabled={documents.isFetching}
                        onClick={() => documents.refetch()}
                      >
                        <IconRefresh className={documents.isFetching ? "animate-spin" : undefined} />
                        Actualizar
                      </Button>
                    </div>
                  </div>

                  <div className="overflow-x-auto rounded-xl border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Requisito</TableHead>
                          <TableHead>Archivo</TableHead>
                          <TableHead>Estado</TableHead>
                          <TableHead className="hidden lg:table-cell">Revisión</TableHead>
                          <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {pageItems.map((document) => (
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
                                {formatFileSize(document.sizeBytes)}
                                {document.uploadedAt ? ` · ${formatDateTime(document.uploadedAt)}` : ""}
                              </p>
                            </TableCell>
                            <TableCell className="align-top">
                              <DocumentStatusBadge status={document.status} />
                            </TableCell>
                            <TableCell className="hidden max-w-72 align-top lg:table-cell">
                              {document.observation ? (
                                <p className="line-clamp-2 text-sm text-muted-foreground" title={document.observation}>
                                  {document.observation}
                                </p>
                              ) : document.reviewedAt ? (
                                <div className="text-xs text-muted-foreground">
                                  <p>{formatDateTime(document.reviewedAt)}</p>
                                  {document.reviewedByUserId != null && <p>Usuario #{document.reviewedByUserId}</p>}
                                </div>
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
                </>
              )}
            </div>
          </div>
        </div>

        {!documents.isPending && !documents.isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
          />
        )}
      </SidebarShellContent>

      {previewDocument?.id && (
        <DocumentPreviewDialog
          applicationId={searchedApplicationId}
          document={{ ...previewDocument, id: previewDocument.id }}
          onOpenChange={(open) => {
            if (!open) setPreviewDocument(null)
          }}
        />
      )}

      {reviewDocument?.id && (
        <DocumentReviewDialog
          applicationId={searchedApplicationId}
          document={{ ...reviewDocument, id: reviewDocument.id }}
          onOpenChange={(open) => {
            if (!open) setReviewDocument(null)
          }}
        />
      )}
    </SidebarShell>
  )
}

function DocumentsQueryError({ error, retry }: { error: ErrorResponse; retry: () => void }) {
  const notFound = error.status === 404
  return (
    <Alert variant="destructive">
      <IconAlertTriangle />
      <AlertTitle>{notFound ? "No se encontró la solicitud" : "No se pudieron cargar los documentos"}</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-3">
        <span>{error.message ?? (notFound ? "Verificá el UUID ingresado." : "Intentá nuevamente.")}</span>
        <Button type="button" size="sm" variant="outline" onClick={retry}>Reintentar</Button>
      </AlertDescription>
    </Alert>
  )
}

function DocumentStatusBadge({ status }: { status?: ApplicationDocumentResponse["status"] }) {
  if (!status) return <Badge variant="outline">Sin estado</Badge>

  const config = {
    PENDING: { label: "Pendiente", className: "border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/50 dark:text-amber-300" },
    VALID: { label: "Válido", className: "border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/50 dark:text-emerald-300" },
    OBSERVED: { label: "Observado", className: "border-red-300 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950/50 dark:text-red-300" },
  } as const

  return <Badge variant="outline" className={config[status].className}>{config[status].label}</Badge>
}

function DocumentPreviewDialog({
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

function DocumentReviewDialog({
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
      await queryClient.invalidateQueries({
        queryKey: list5QueryKey({ path: { applicationId } }),
      })
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

function formatFileSize(sizeBytes?: number) {
  if (sizeBytes == null) return "Tamaño desconocido"
  if (sizeBytes < 1024) return `${sizeBytes} B`
  if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} KB`
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("es-AR", {
    dateStyle: "short",
    timeStyle: "short",
  })
}
