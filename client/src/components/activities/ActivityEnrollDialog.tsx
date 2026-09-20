import { IconAlertTriangle, IconCalendarEvent, IconMapPin, IconUsers } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { formatActivityDateRange, seatsLabel } from "@/components/activities/activity-ui"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
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
import { enrollMutation, get4QueryKey, list8QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CitizenActivityResponse } from "@/generated/types.gen"

export function ActivityEnrollDialog({ activity, onOpenChange, onEnrolled }: {
  activity: CitizenActivityResponse
  onOpenChange: (open: boolean) => void
  onEnrolled?: (activityId: string) => void
}) {
  const queryClient = useQueryClient()
  const enroll = useMutation({
    ...enrollMutation(),
    onSuccess: () => {
      // El cupo disponible cambia para todos, así que se refresca el listado.
      queryClient.invalidateQueries({ queryKey: list8QueryKey() })
      if (activity.id) queryClient.invalidateQueries({ queryKey: get4QueryKey({ path: { id: activity.id } }) })
      toast.success("Te inscribiste en la actividad")
      if (activity.id) onEnrolled?.(activity.id)
      onOpenChange(false)
    },
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!enroll.isPending) onOpenChange(open) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Confirmar inscripción</DialogTitle>
          <DialogDescription>
            Vas a inscribirte en «{activity.name || "esta actividad"}». La inscripción es personal y por única vez.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2.5 rounded-xl bg-muted/60 p-4 text-sm">
          <p className="flex items-start gap-2 text-muted-foreground">
            <IconMapPin className="mt-0.5 size-4 shrink-0 text-primary" />
            <span className="min-w-0">{activity.location || "Lugar a confirmar"}</span>
          </p>
          <p className="flex items-start gap-2 text-muted-foreground">
            <IconCalendarEvent className="mt-0.5 size-4 shrink-0 text-primary" />
            <span className="min-w-0">{formatActivityDateRange(activity.startDate, activity.endDate)}</span>
          </p>
          <p className="flex items-start gap-2 text-muted-foreground">
            <IconUsers className="mt-0.5 size-4 shrink-0 text-primary" />
            <span className="min-w-0">{seatsLabel(activity)}</span>
          </p>
        </div>

        {enroll.error && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No pudimos confirmar tu inscripción</AlertTitle>
            <AlertDescription>
              {enroll.error.message ?? "Intentá nuevamente en unos instantes."}
            </AlertDescription>
          </Alert>
        )}

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={enroll.isPending} />}>
            Cancelar
          </DialogClose>
          <Button
            disabled={enroll.isPending || !activity.id}
            onClick={() => { if (activity.id) enroll.mutate({ path: { id: activity.id } }) }}
          >
            {enroll.isPending ? "Inscribiendo…" : "Confirmar inscripción"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
