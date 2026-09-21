import { IconDownload } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { useEffect, useMemo } from "react"
import { ApplicationError } from "@/components/applications/ApplicationUi"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { contentOptions } from "@/generated/@tanstack/react-query.gen"
import type { ApplicationDocumentResponse } from "@/generated/types.gen"

export function ApplicationDocumentPreviewDialog({ applicationId, document, onOpenChange }: {
  applicationId: string
  document: ApplicationDocumentResponse & { id: string }
  onOpenChange: (open: boolean) => void
}) {
  const content = useQuery({ ...contentOptions({ path: { applicationId, applicationDocumentId: document.id }, parseAs: "blob" }), gcTime: 0 })
  const objectUrl = useMemo(() => content.data ? URL.createObjectURL(content.data) : undefined, [content.data])
  useEffect(() => () => { if (objectUrl) URL.revokeObjectURL(objectUrl) }, [objectUrl])
  const download = () => {
    if (!objectUrl) return
    const link = window.document.createElement("a")
    link.href = objectUrl
    link.download = document.originalName ?? "documento"
    link.click()
  }
  return <Dialog open onOpenChange={onOpenChange}>
    <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-4xl">
      <DialogHeader><DialogTitle className="truncate pr-8">{document.originalName ?? "Documento"}</DialogTitle><DialogDescription>{document.requirementName ?? "Documento de la solicitud"}</DialogDescription></DialogHeader>
      {content.isPending ? <p role="status" className="flex h-64 items-center justify-center text-sm text-muted-foreground">Cargando documento…</p> : content.isError ? <ApplicationError error={content.error} title="No se pudo abrir el documento" retry={() => content.refetch()} /> : objectUrl ? (
        document.contentType?.startsWith("image/") ? <img src={objectUrl} alt={document.requirementName ?? "Documento adjunto"} className="max-h-[60vh] w-full rounded-lg border object-contain" />
          : <iframe src={objectUrl} title={document.originalName ?? "Vista previa del documento"} className="h-[60vh] w-full rounded-lg border bg-white" />
      ) : null}
      <DialogFooter><DialogClose render={<Button variant="outline" />}>Cerrar</DialogClose><Button onClick={download} disabled={!objectUrl}><IconDownload />Descargar</Button></DialogFooter>
    </DialogContent>
  </Dialog>
}
