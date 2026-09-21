import { IconPhoto, IconLoader2, IconUpload, IconX } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useRef, useState } from "react"
import { toast } from "sonner"

import { ApplicationError } from "@/components/applications/ApplicationUi"
import { Attachment, AttachmentAction, AttachmentActions, AttachmentContent, AttachmentDescription, AttachmentMedia, AttachmentTitle, AttachmentTrigger } from "@/components/ui/attachment"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import {
  createProgramImageMutation,
  findById2QueryKey,
  getAvailableProgramQueryKey,
  list1QueryKey,
  listAvailableProgramsQueryKey,
  updateProgramImageMutation,
} from "@/generated/@tanstack/react-query.gen"
import { formatDocumentSize } from "@/lib/application-flow"
import { PROGRAM_IMAGE_ACCEPT, validateProgramImageFile } from "@/lib/program-images"

export function ProgramImageUploadDialog({ programId, hasImage, onOpenChange }: {
  programId: string
  hasImage: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const input = useRef<HTMLInputElement>(null)
  const uploading = useRef(false)
  const [file, setFile] = useState<File | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)

  const refreshPrograms = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: findById2QueryKey({ path: { id: programId } }) }),
      queryClient.invalidateQueries({ queryKey: list1QueryKey() }),
      queryClient.invalidateQueries({ queryKey: getAvailableProgramQueryKey({ path: { id: programId } }) }),
      queryClient.invalidateQueries({ queryKey: listAvailableProgramsQueryKey() }),
    ])
    toast.success(hasImage ? "Imagen de portada reemplazada" : "Imagen de portada agregada")
    onOpenChange(false)
  }

  const createImage = useMutation({
    ...createProgramImageMutation(),
    retry: false,
    onSuccess: refreshPrograms,
  })
  const updateImage = useMutation({
    ...updateProgramImageMutation(),
    retry: false,
    onSuccess: refreshPrograms,
  })
  const upload = hasImage ? updateImage : createImage
  const busy = upload.isPending

  const selectFiles = (files: FileList | null) => {
    if (busy || !files?.length) return
    upload.reset()
    const nextFile = files[0]
    const error = files.length > 1
      ? "Seleccioná una sola imagen para la portada."
      : validateProgramImageFile(nextFile)
    setValidationError(error ?? null)
    setFile(error ? null : nextFile)
  }

  const submit = async () => {
    if (busy || uploading.current || !file) return
    const error = validateProgramImageFile(file)
    if (error) {
      setValidationError(error)
      return
    }

    uploading.current = true
    try {
      await upload.mutateAsync({ path: { programId }, body: { file } })
    } catch {
      // La mutación muestra el error y conserva el archivo para permitir reintentar.
    } finally {
      uploading.current = false
    }
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!upload.isPending) onOpenChange(open) }}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-lg" showCloseButton={!upload.isPending}>
        <DialogHeader>
          <DialogTitle>{hasImage ? "Reemplazar imagen de portada" : "Subir imagen de portada"}</DialogTitle>
          <DialogDescription>
            Esta imagen se mostrará en el listado y en el detalle público del programa.
          </DialogDescription>
        </DialogHeader>

        {hasImage && (
          <p className="text-sm text-muted-foreground">
            La nueva imagen reemplazará la portada actual.
          </p>
        )}

        <input
          ref={input}
          type="file"
          className="sr-only"
          tabIndex={-1}
          accept={PROGRAM_IMAGE_ACCEPT}
          aria-label="Imagen de portada"
          disabled={busy}
          onChange={(event) => {
            selectFiles(event.target.files)
            event.target.value = ""
          }}
        />

        <div
          onDragOver={(event) => {
            event.preventDefault()
            if (!busy) setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => {
            event.preventDefault()
            setDragging(false)
            selectFiles(event.dataTransfer.files)
          }}
        >
          {file ? (
            <Attachment className="w-full" state={upload.isPending ? "uploading" : upload.isError ? "error" : "idle"} aria-busy={upload.isPending}>
              <AttachmentMedia>
                {upload.isPending ? <IconLoader2 className="animate-spin" /> : <IconPhoto />}
              </AttachmentMedia>
              <AttachmentContent>
                <AttachmentTitle>{file.name}</AttachmentTitle>
                <AttachmentDescription>
                  {upload.isPending
                    ? "Subiendo imagen…"
                    : upload.isError
                      ? "No se pudo subir. Podés reintentar."
                      : `${formatDocumentSize(file.size)} · Lista para subir`}
                </AttachmentDescription>
              </AttachmentContent>
              <AttachmentActions>
                <AttachmentAction
                  aria-label="Quitar imagen seleccionada"
                  disabled={busy}
                  onClick={() => {
                    setFile(null)
                    upload.reset()
                    setValidationError(null)
                  }}
                >
                  <IconX />
                </AttachmentAction>
              </AttachmentActions>
            </Attachment>
          ) : (
            <Attachment state="idle" className={`min-h-40 w-full justify-center p-6 text-center ${dragging ? "border-primary bg-primary/5" : ""}`}>
              <AttachmentContent>
                <IconUpload className="mx-auto mb-3 size-8 text-muted-foreground" />
                <AttachmentTitle>Seleccioná una imagen o arrastrala aquí</AttachmentTitle>
                <AttachmentDescription>JPG o PNG · Hasta 10 MB</AttachmentDescription>
              </AttachmentContent>
              <AttachmentTrigger aria-label="Seleccionar imagen" disabled={busy} onClick={() => input.current?.click()} />
            </Attachment>
          )}
        </div>

        {validationError && <p role="alert" className="text-sm text-destructive">{validationError}</p>}
        {upload.error && <ApplicationError error={upload.error} title="No se pudo subir la imagen de portada" />}

        <DialogFooter>
          <DialogClose render={<Button variant="outline" disabled={upload.isPending} />}>Cancelar</DialogClose>
          <Button onClick={submit} disabled={busy || !file}>
            {upload.isPending ? "Subiendo…" : hasImage ? "Reemplazar portada" : "Subir portada"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
