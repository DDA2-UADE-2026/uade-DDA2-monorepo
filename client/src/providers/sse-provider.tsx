import { useEffect, useRef, useState } from "react"
import { SseContext } from "@/context/sse-context"
import type { ResourceUpdatedData, SseEventPayload } from "@/lib/sse-event"

const FEED_LIMIT = 50

type SseProviderProps = {
  children: React.ReactNode
}

// Requiere auth por cookie (JWT httpOnly): EventSource no puede mandar el
// header Authorization, así que hasta que el login migre a cookies este
// endpoint queda autenticado solo para clientes que sí manden cookie (curl,
// Postman, etc.) y el browser no podrá conectarse todavía.
export function SseProvider({ children }: SseProviderProps) {
  const [pendingUpdates, setPendingUpdates] = useState<Record<string, string>>({})
  const [feed, setFeed] = useState<SseEventPayload[]>([])
  const dismiss = useRef((resource: string) => {
    setPendingUpdates((current) => {
      if (!(resource in current)) return current
      const { [resource]: _removed, ...rest } = current
      return rest
    })
  }).current

  useEffect(() => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"
    const eventSource = new EventSource(`${baseUrl}/events/subscribe`, {
      withCredentials: true,
    })

    eventSource.onmessage = (event) => {
      const payload = JSON.parse(event.data) as SseEventPayload
      setFeed((current) => [payload, ...current].slice(0, FEED_LIMIT))

      if (payload.eventType === "resource.updated") {
        const data = payload.data as unknown as ResourceUpdatedData
        setPendingUpdates((current) => ({ ...current, [data.resource]: payload.eventId }))
      }
    }

    return () => eventSource.close()
  }, [])

  return (
    <SseContext.Provider value={{ pendingUpdates, dismiss, feed }}>
      {children}
    </SseContext.Provider>
  )
}
