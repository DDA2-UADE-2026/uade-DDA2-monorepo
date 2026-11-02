export const coreStatusLabels: Record<string, string> = {
  ROUTED: "Ruteado",
  NO_SUBSCRIBERS: "Sin suscriptores",
  REJECTED: "Rechazado",
  DUPLICATE: "Duplicado",
  DELIVERED: "Entregado",
  PENDING: "Pendiente",
  PENDING_RETRY: "Reintento pendiente",
  RETRYING: "Reintentando",
  DEAD: "Fallido (DLQ)",
  FAILED: "Fallido",
}

export const coreStatusLabel = (status?: string) => (status ? coreStatusLabels[status] ?? status : "—")

export function formatDateTime(value?: string) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"
  return date.toLocaleString("es-AR", { dateStyle: "short", timeStyle: "medium" })
}
