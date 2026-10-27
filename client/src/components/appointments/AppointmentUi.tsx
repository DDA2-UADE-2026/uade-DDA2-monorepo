import { IconAlertCircle } from "@tabler/icons-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import type { ErrorResponse } from "@/generated/types.gen"

export function AppointmentError({ error, title, retry }: { error: ErrorResponse; title: string; retry?: () => void }) {
  return (
    <Alert variant="destructive">
      <IconAlertCircle />
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-3">
        <span>{error.message ?? "Intentá nuevamente en unos instantes."}</span>
        {retry && <Button size="sm" variant="outline" onClick={retry}>Reintentar</Button>}
      </AlertDescription>
    </Alert>
  )
}
