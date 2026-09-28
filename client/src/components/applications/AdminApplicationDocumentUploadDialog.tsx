import { IconFileDescription, IconLoader2, IconUpload, IconX } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useRef, useState } from "react"
import { toast } from "sonner"

import { ApplicationError } from "@/components/applications/ApplicationUi"
import {
  Attachment,
  AttachmentAction,
  AttachmentActions,
  AttachmentContent,
  AttachmentDescription,
  AttachmentMedia,
  AttachmentTitle,
  AttachmentTrigger,
} from "@/components/ui/attachment"
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
  content1QueryKey,
  get3QueryKey,
  list4QueryKey,
  list7QueryKey,
  put1Mutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ApplicationDocumentResponse, AvailableProgramDocumentRequirementResponse } from "@/generated/types.gen"
import { DOCUMENT_ACCEPT, formatDocumentSize, validateApplicationFile } from "@/lib/application-flow"

/**
 * Entrega asistida: el administrativo carga el archivo en nombre del titular para
 * completar un trámite iniciado en la ventanilla. El archivo queda atribuido a
 * quien lo sube y la entrega vuelve a quedar pendiente de revisión.
 */
export function AdminApplicationDocumentUploadDialog({ applicationId, requirement, existing, disabled, onOpenChange }: {
  applicationId: string
  requirement: AvailableProgramDocumentRequirementResponse & { id: string }
  existing?: ApplicationDocumentResponse
  disabled: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const input = useRef<HTMLInputElement>(null)
  const uploading = useRef(false)
  const [file, setFile] = useState<File | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)

  const upload = useMutation({
    ...put1Mutation(),
    retry: false,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: list7QueryKey({ path: { applicationId } }) }),
        queryClient.invalidateQueries({ queryKey: get3QueryKey({ path: { id: applicationId } }) }),
        queryClient.invalidateQueries({ queryKey: list4QueryKey() }),
        ...(existing?.id
          ? [queryClient.invalidateQueries({ queryKey: content1QueryKey({ path: { applicationId, applicationDocumentId: existing.id } }) })]
          : []),
      ])
      toast.success(existing ? "Documento reemplazado y enviado a revisión" : "Documento cargado en nombre del titular")
      onOpenChange(false)
    },
    onError: (error) => {
      // Una solicitud resuelta deja de admitir cambios: se refresca el detalle para reflejarlo.
      if (error.status === 409) queryClient.invalidateQueries({ queryKey: get3QueryKey({ path: { id: applicationId } }) })
    },
  })

  const busy = disabled || upload.isPending
  const selectFiles = (files: FileList | null) => {
    if (busy || !files?.length) return
    upload.reset()
    const nextFile = files[0]
    const error = files.length > 1 ? "Seleccioná un solo archivo para este documento." : validateApplicationFile(nextFile)
    setValidationError(error ?? null)
    setFile(error ? null : nextFile)
  }
  const submit = async () => {
    if (busy || uploading.current || !file) return
    const error = validateApplicationFile(file)
    if (error) { setValidationError(error); return }
    uploading.current = true
    try {
      await upload.mutateAsync({ path: { applicationId, requirementId: requirement.id }, body: { file } })
    } catch {
      // El archivo elegido se conserva para reintentar; el error se muestra en el diálogo.
    } finally { uploading.current = false }
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!upload.isPending) onOpenChange(open) }}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-lg" showCloseButton={!upload.isPending}>
        <DialogHeader>
          <DialogTitle>{existing ? "Reemplazar documento del titular" : "Cargar documento por el titular"}</DialogTitle>
          <DialogDescription>
            {requirement.name ?? "Documento de la solicitud"}{requirement.required ? " · Obligatorio" : " · Opcional"}
          </DialogDescription>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          La entrega queda a nombre del titular, pero el archivo se registra como cargado por vos.
        </p>
        {requirement.description && (
          <p className="whitespace-pre-line text-sm text-muted-foreground">{requirement.description}</p>
        )}
        {existing && (
          <p className="text-sm text-muted-foreground">
            El nuevo archivo reemplazará a {existing.originalName ?? "la entrega actual"} y volverá a quedar pendiente de revisión.
          </p>
        )}

        <input
          ref={input}
          type="file"
          className="sr-only"
          tabIndex={-1}
          accept={DOCUMENT_ACCEPT}
          aria-label="Archivo del documento"
          disabled={busy}
          onChange={(event) => { selectFiles(event.target.files); event.target.value = "" }}
        />
        <div
          onDragOver={(event) => { event.preventDefault(); if (!busy) setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => { event.preventDefault(); setDragging(false); selectFiles(event.dataTransfer.files) }}
        >
          {file ? (
            <Attachment
              className="w-full"
              state={upload.isPending ? "uploading" : upload.isError ? "error" : "idle"}
              aria-busy={upload.isPending}
            >
              <AttachmentMedia>
                {upload.isPending ? <IconLoader2 className="animate-spin" /> : <IconFileDescription />}
              </AttachmentMedia>
              <AttachmentContent>
                <AttachmentTitle>{file.name}</AttachmentTitle>
                <AttachmentDescription>
                  {upload.isPending
                    ? "Enviando documento…"
                    : upload.isError
                      ? "No se pudo enviar. Podés reintentar."
                      : `${formatDocumentSize(file.size)} · Listo para enviar`}
                </AttachmentDescription>
              </AttachmentContent>
              <AttachmentActions>
                <AttachmentAction
                  aria-label="Quitar archivo seleccionado"
                  disabled={busy}
                  onClick={() => { setFile(null); upload.reset(); setValidationError(null) }}
                >
                  <IconX />
                </AttachmentAction>
              </AttachmentActions>
            </Attachment>
          ) : (
            <Attachment
              state="idle"
              className={`w-full min-h-40 justify-center p-6 text-center ${dragging ? "border-primary bg-primary/5" : ""}`}
            >
              <AttachmentContent>
                <IconUpload className="mx-auto mb-3 size-8 text-muted-foreground" />
                <AttachmentTitle>Seleccioná un archivo o arrastralo aquí</AttachmentTitle>
                <AttachmentDescription>PDF, JPG o PNG · Hasta 10 MB</AttachmentDescription>
              </AttachmentContent>
              <AttachmentTrigger aria-label="Seleccionar archivo" disabled={busy} onClick={() => input.current?.click()} />
            </Attachment>
          )}
        </div>

        {validationError && <p role="alert" className="text-sm text-destructive">{validationError}</p>}
        {upload.error && <ApplicationError error={upload.error} title="No se pudo cargar el documento" />}
        {disabled && (
          <p role="alert" className="text-sm text-destructive">
            Esta solicitud ya no admite cambios en la documentación.
          </p>
        )}

        <DialogFooter>
          <DialogClose render={<Button variant="outline" disabled={upload.isPending} />}>Cancelar</DialogClose>
          <Button onClick={submit} disabled={busy || !file}>
            {upload.isPending ? "Enviando…" : existing ? "Reemplazar documento" : "Cargar documento"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
