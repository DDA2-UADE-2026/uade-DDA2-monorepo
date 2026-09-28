import { IconAlertTriangle } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  close1Mutation,
  get1QueryKey,
  list5QueryKey,
  publishMutation,
} from "@/generated/@tanstack/react-query.gen"
import type { ActivityResponse } from "@/generated/types.gen"

export type ActivityStatusAction = "publish" | "close"

const copy = {
  publish: {
    title: "Publicar actividad",
    description:
      "Queda visible para la ciudadanía y se habilitan las inscripciones. Una actividad publicada ya no se puede editar.",
    confirm: "Publicar",
  },
  close: {
    title: "Cerrar actividad",
    description:
      "Deja de recibir inscripciones y se da por finalizada. El cierre es definitivo: no se puede reabrir.",
    confirm: "Cerrar",
  },
} as const

export function ActivityStatusDialog({ activity, action, onOpenChange, onDone }: {
  activity: Pick<ActivityResponse, "id" | "name">
  action: ActivityStatusAction
  onOpenChange: (open: boolean) => void
  onDone?: (activity: ActivityResponse) => void
}) {
  const queryClient = useQueryClient()
  const onSuccess = (saved: ActivityResponse) => {
    queryClient.invalidateQueries({ queryKey: list5QueryKey() })
    if (saved.id) queryClient.invalidateQueries({ queryKey: get1QueryKey({ path: { id: saved.id } }) })
    onDone?.(saved)
    onOpenChange(false)
  }
  const publish = useMutation({ ...publishMutation(), onSuccess })
  const close = useMutation({ ...close1Mutation(), onSuccess })

  const mutation = action === "publish" ? publish : close
  const texts = copy[action]

  return (
    <AlertDialog open onOpenChange={(open) => { if (!open && !mutation.isPending) onOpenChange(false) }}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{texts.title}</AlertDialogTitle>
          <AlertDialogDescription>
            «{activity.name ?? "Sin nombre"}» — {texts.description}
          </AlertDialogDescription>
        </AlertDialogHeader>

        {mutation.error && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No se pudo cambiar el estado</AlertTitle>
            <AlertDescription>
              {mutation.error.message ?? "Revisá el estado de la actividad e intentá nuevamente."}
            </AlertDescription>
          </Alert>
        )}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={mutation.isPending}>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            disabled={mutation.isPending || !activity.id}
            onClick={(event) => {
              event.preventDefault()
              if (!activity.id) return
              mutation.mutate({ path: { id: activity.id } })
            }}
          >
            {mutation.isPending ? "Guardando…" : texts.confirm}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
