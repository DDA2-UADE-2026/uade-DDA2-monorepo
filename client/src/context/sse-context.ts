import { createContext } from "react"
import type { SseEventPayload } from "@/lib/sse-event"

export interface SseContextValue {
  // Última novedad pendiente por recurso (ej. "roles" -> eventId). Se limpia con dismiss().
  pendingUpdates: Record<string, string>
  dismiss: (resource: string) => void
  // Feed acotado de los últimos eventos crudos, para el dashboard en vivo.
  feed: SseEventPayload[]
}

export const SseContext = createContext<SseContextValue | null>(null)
