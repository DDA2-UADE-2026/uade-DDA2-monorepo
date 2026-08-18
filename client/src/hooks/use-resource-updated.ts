import { useContext } from "react"
import { SseContext } from "@/context/sse-context"

// Prende un badge de "hay novedades" para un recurso (ej. "intervenciones")
// cuando otro usuario lo modifica. No refetchea solo: el consumidor decide
// cuándo llamar a dismiss() (típicamente junto con queryClient.invalidateQueries).
export function useResourceUpdated(resource: string) {
  const context = useContext(SseContext)
  if (!context) {
    throw new Error("useResourceUpdated debe usarse dentro de <SseProvider>")
  }

  return {
    hasUpdate: resource in context.pendingUpdates,
    dismiss: () => context.dismiss(resource),
  }
}
