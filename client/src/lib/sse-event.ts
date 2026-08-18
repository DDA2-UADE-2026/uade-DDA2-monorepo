// Espejo del DTO del server (SseEventPayload): mismo formato común de evento
// que usa el TPO (tipo, id, fecha, módulo de origen, datos), tanto para
// eventos internos ("resource.updated") como para los eventos async de Kafka
// que más adelante se reenvíen por el mismo canal.
export interface SseEventPayload {
  eventType: string
  eventId: string
  occurredAt: string
  sourceModule: string
  data: Record<string, unknown>
}

export interface ResourceUpdatedData {
  resource: string
  resourceId: string
  action: "CREATE" | "UPDATE" | "DELETE"
}
