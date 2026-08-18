import { useContext } from "react"
import { SseContext } from "@/context/sse-context"

// Últimos eventos recibidos por SSE, más nuevo primero. Pensado para el
// dashboard: hoy solo va a traer eventos "resource.updated" (internos),
// pero cuando el consumer de Kafka reenvíe eventos externos por el mismo
// hub van a aparecer acá sin cambios en este hook.
export function useLiveEventFeed() {
  const context = useContext(SseContext)
  if (!context) {
    throw new Error("useLiveEventFeed debe usarse dentro de <SseProvider>")
  }

  return context.feed
}
