import type { SocialTicketResponse } from "@/generated/types.gen"

export type TicketState = NonNullable<SocialTicketResponse["state"]>

export const TICKET_VIEW_PERMISSION = "tickets:management:view"
export const TICKET_MANAGE_PERMISSION = "tickets:management:manage"

export const TICKET_STATES: readonly TicketState[] = ["RECEIVED", "IN_PROGRESS", "WAITING_INFORMATION", "RESOLVED", "RETURNED", "CLOSED"]

// Mismo conjunto que SocialTicketEventService.TERMINAL: sin gestión activa en M8, no se puede asignar.
export const TERMINAL_TICKET_STATES: ReadonlySet<TicketState> = new Set(["RESOLVED", "RETURNED", "CLOSED"])

export const ticketStateLabels: Record<TicketState, string> = {
  RECEIVED: "Recibido",
  IN_PROGRESS: "En curso",
  WAITING_INFORMATION: "Esperando información",
  RESOLVED: "Resuelto",
  RETURNED: "Devuelto",
  CLOSED: "Cerrado",
}

export const ticketStateStyles: Record<TicketState, string> = {
  RECEIVED: "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-400",
  IN_PROGRESS: "border-violet-500/30 bg-violet-500/10 text-violet-700 dark:text-violet-400",
  WAITING_INFORMATION: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400",
  RESOLVED: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  RETURNED: "border-zinc-500/30 bg-zinc-500/10 text-zinc-700 dark:text-zinc-300",
  CLOSED: "border-zinc-500/30 bg-zinc-500/10 text-zinc-700 dark:text-zinc-300",
}

// Estado y prioridad vienen de M2 como texto libre: se traducen los conocidos y el resto se muestra tal cual.
const coreStatusLabels: Record<string, string> = {
  CREATED: "Creado",
  ROUTED: "Derivado",
  STARTED: "Iniciado",
  IN_PROGRESS: "En curso",
  INFORMATION_REQUIRED: "Información requerida",
  WAITING_INFORMATION: "Esperando información",
  RESOLVED: "Resuelto",
  REJECTED: "Rechazado",
  RETURNED: "Devuelto",
  REOPENED: "Reabierto",
  CLOSED: "Cerrado",
  CANCELLED: "Cancelado",
}

export const ticketPriorityLabels: Record<string, string> = {
  LOW: "Baja",
  MEDIUM: "Media",
  HIGH: "Alta",
  URGENT: "Urgente",
  CRITICAL: "Crítica",
}

export const ticketPriorityStyles: Record<string, string> = {
  LOW: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  MEDIUM: "border-yellow-500/40 bg-yellow-400/15 text-yellow-800 dark:text-yellow-300",
  HIGH: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400",
  URGENT: "border-violet-500/40 bg-violet-500/15 text-violet-800 dark:text-violet-300",
  CRITICAL: "border-violet-500/40 bg-violet-500/15 text-violet-800 dark:text-violet-300",
}

export const ticketStateLabel = (state?: string) => (state ? ticketStateLabels[state as TicketState] ?? state : "—")
export const ticketCoreStatusLabel = (status?: string) => (status ? coreStatusLabels[status] ?? status : "—")
export const ticketPriorityLabel = (priority?: string) => (priority ? ticketPriorityLabels[priority] ?? priority : "—")

export const isTicketManageable = (ticket: SocialTicketResponse) =>
  !!ticket.state && !TERMINAL_TICKET_STATES.has(ticket.state) && ticket.responsibleAreaId === "M8"

export function formatFileSize(bytes?: number) {
  if (bytes == null) return null
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
