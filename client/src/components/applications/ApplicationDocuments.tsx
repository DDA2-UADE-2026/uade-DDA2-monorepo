import { IconAlertCircle, IconEye, IconFileCheck, IconFileDescription, IconRefresh, IconUpload } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"

import { ApplicationDocumentPreviewDialog } from "@/components/applications/ApplicationDocumentPreviewDialog"
import { ApplicationDocumentUploadDialog } from "@/components/applications/ApplicationDocumentUploadDialog"
import { ApplicationError } from "@/components/applications/ApplicationUi"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Attachment, AttachmentContent, AttachmentDescription, AttachmentMedia, AttachmentTitle } from "@/components/ui/attachment"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { contentQueryKey, delete8Mutation, get1QueryKey, list4Options, list4QueryKey, listQueryKey } from "@/generated/@tanstack/react-query.gen"
import type { ApplicationDocumentResponse, ApplicationResponse, AvailableProgramDocumentRequirementResponse } from "@/generated/types.gen"
import { applicationDocumentRequirements, formatDocumentSize, isApplicationResolved } from "@/lib/application-flow"

export function ApplicationDocuments({ application }: { application: ApplicationResponse & { id: string } }) {
  const applicationId = application.id
  const queryClient = useQueryClient()
  const documents = useQuery(list4Options({ path: { applicationId } }))
  const [uploadTarget, setUploadTarget] = useState<(AvailableProgramDocumentRequirementResponse & { id: string }) | null>(null)
  const [preview, setPreview] = useState<(ApplicationDocumentResponse & { id: string }) | null>(null)
  const [finalized, setFinalized] = useState(false)
  const disabled = finalized || !application.status || isApplicationResolved(application.status)
  const remove = useMutation({
    ...delete8Mutation(),
    onSuccess: async (_data, variables) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: get1QueryKey({ path: { id: applicationId } }) }),
        queryClient.invalidateQueries({ queryKey: list4QueryKey({ path: { applicationId } }) }),
        queryClient.invalidateQueries({ queryKey: listQueryKey() }),
      ])
      queryClient.removeQueries({ queryKey: contentQueryKey({ path: variables.path }) })
      toast.success("Documento eliminado")
    },
    onError: (error) => {
      if (error.code === "APPLICATION_DOCUMENTS_FINALIZED") setFinalized(true)
      showApiErrorToast(error)
    },
  })
  const requirements = applicationDocumentRequirements(application, documents.data ?? [])
  const byRequirement = new Map(documents.data?.map((document) => [document.requirementId, document]))

  return <section id="documentacion" aria-labelledby="application-documents-title" className="scroll-mt-6 space-y-4">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div><h2 id="application-documents-title" className="font-heading text-xl font-medium">Documentación</h2><p className="mt-1 text-sm text-muted-foreground">Consultá tus entregas y completá los documentos solicitados.</p></div>
      <Button size="sm" variant="outline" disabled={documents.isFetching} onClick={() => { documents.refetch(); queryClient.invalidateQueries({ queryKey: get1QueryKey({ path: { id: applicationId } }) }) }}><IconRefresh className={documents.isFetching ? "animate-spin" : undefined} />Actualizar documentos</Button>
    </div>
    {disabled && <Alert><IconFileCheck /><AlertTitle>Documentación disponible para consulta</AlertTitle><AlertDescription>El estado de esta solicitud ya no permite cargar, reemplazar ni eliminar documentos.</AlertDescription></Alert>}
    {documents.isPending ? <Skeleton className="h-40 w-full" /> : documents.isError ? <ApplicationError error={documents.error} title="No pudimos cargar la documentación" retry={() => documents.refetch()} /> : requirements.length === 0 ? (
      <Card><CardContent className="text-sm text-muted-foreground">Esta edición no solicita documentación.</CardContent></Card>
    ) : <div className="space-y-4">
      {requirements.map((requirement) => {
        const document = byRequirement.get(requirement.id)
        const pending = application.pendingDocuments?.find((item) => item.requirementId === requirement.id)
        const observed = document?.status === "OBSERVED" || pending?.reason === "OBSERVED"
        const statusLabel = observed ? "Observado" : document?.status === "VALID" ? "Válido" : document ? "Pendiente de revisión" : requirement.required ? "Falta adjuntar" : "Sin adjuntar"
        return <Card key={requirement.id}>
          <CardHeader>
            <div className="flex flex-wrap justify-between gap-2">
              <Badge variant="outline">{requirement.required ? "Obligatorio" : "Opcional"}</Badge>
              <Badge variant={observed ? "destructive" : document?.status === "VALID" ? "default" : "secondary"}>{statusLabel}</Badge>
            </div>
            <CardTitle className="text-base">{requirement.name ?? "Documento solicitado"}</CardTitle>
            {requirement.description && <CardDescription className="whitespace-pre-line">{requirement.description}</CardDescription>}
          </CardHeader>
          <CardContent className="space-y-4">
            {observed && <Alert variant="destructive"><IconAlertCircle /><AlertTitle>Corregí este documento</AlertTitle><AlertDescription>{document?.observation ?? pending?.observation ?? "Revisá el archivo y enviá una nueva versión."}</AlertDescription></Alert>}
            {document && <Attachment className="w-full" state={observed ? "error" : "done"}>
              <AttachmentMedia><IconFileDescription /></AttachmentMedia>
              <AttachmentContent><AttachmentTitle>{document.originalName ?? "Archivo adjunto"}</AttachmentTitle><AttachmentDescription>{formatDocumentSize(document.sizeBytes)}{document.contentType ? ` · ${document.contentType === "application/pdf" ? "PDF" : document.contentType === "image/png" ? "PNG" : "JPG"}` : ""}</AttachmentDescription></AttachmentContent>
            </Attachment>}
            <div className="flex flex-wrap justify-end gap-2">
              {document?.id && <Button size="sm" variant="outline" onClick={() => setPreview({ ...document, id: document.id! })}><IconEye />Ver archivo</Button>}
              {document?.id && !disabled && <DeleteConfirmationButton size="sm" description={`Se eliminará el archivo de «${requirement.name ?? "este documento"}».`} disabled={remove.isPending} onConfirm={() => remove.mutateAsync({ path: { applicationId, applicationDocumentId: document.id! } })} />}
              {!disabled && <Button size="sm" disabled={remove.isPending} onClick={() => setUploadTarget(requirement)}><IconUpload />{document ? "Reemplazar" : "Adjuntar documento"}</Button>}
            </div>
          </CardContent>
        </Card>
      })}
    </div>}
    {uploadTarget && <ApplicationDocumentUploadDialog applicationId={applicationId} requirement={uploadTarget} existing={byRequirement.get(uploadTarget.id)} disabled={disabled} onOpenChange={(open) => { if (!open) setUploadTarget(null) }} />}
    {preview && <ApplicationDocumentPreviewDialog applicationId={applicationId} document={preview} onOpenChange={(open) => { if (!open) setPreview(null) }} />}
  </section>
}
