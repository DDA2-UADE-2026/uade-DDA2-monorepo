import { IconAlertTriangle, IconExternalLink, IconFileDescription, IconInfoCircle, IconPaperclip, IconUserCheck } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { useState, type ReactNode } from "react"
import { toast } from "sonner"

import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { UserChip } from "@/components/UserChip"
import { UserSelectionDialog } from "@/components/users/UserSelectionDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { assignMutation, get3Options, get3QueryKey, list9QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { SocialTicketResponse, TicketRoutingData, UserManagementResponse } from "@/generated/types.gen"
import { formatDateTime } from "@/lib/core-events"
import {
  formatFileSize,
  isTicketManageable,
  ticketCoreStatusLabel,
  ticketPriorityLabel,
  ticketPriorityStyles,
  ticketStateLabel,
  ticketStateStyles,
  type TicketState,
} from "@/lib/tickets"
import { cn } from "@/lib/utils"

export function TicketStateBadge({ state }: { state?: string }) {
  return <Badge variant="outline" className={cn(state && ticketStateStyles[state as TicketState])}>{ticketStateLabel(state)}</Badge>
}

export function TicketPriorityBadge({ priority }: { priority?: string }) {
  return <Badge variant="outline" className={cn(priority && ticketPriorityStyles[priority])}>{ticketPriorityLabel(priority)}</Badge>
}

export function TicketWorker({ workerId, users }: { workerId?: number; users: ReadonlyMap<number, UserManagementResponse> }) {
  if (workerId == null) return <span className="text-muted-foreground">Sin asignar</span>
  const user = users.get(workerId)
  return <UserChip user={{ id: workerId, name: user?.name ?? `Usuario #${workerId}`, username: user?.username, email: user?.email }} secondary={user?.email} />
}

/** Panel lateral con el detalle completo del ticket y la asignación del administrativo responsable. */
export function TicketSheet({ ticket, users, canManage, canPickUsers, onClose }: {
  ticket: SocialTicketResponse | null
  users: ReadonlyMap<number, UserManagementResponse>
  canManage: boolean
  canPickUsers: boolean
  onClose: () => void
}) {
  const queryClient = useQueryClient()
  const [pickerOpen, setPickerOpen] = useState(false)
  const ticketId = ticket?.id
  const detail = useQuery({ ...get3Options({ path: { ticketId: ticketId ?? "" } }), enabled: !!ticketId })

  const current = detail.data?.ticket ?? ticket
  const routing = detail.data?.routing as Partial<TicketRoutingData> | undefined
  const attachments = detail.data?.attachments ?? []
  const assignedId = current?.assignedWorkerId ?? undefined
  const manageable = !!current && isTicketManageable(current)
  const canAssign = canManage && canPickUsers && manageable

  const assign = useMutation({
    ...assignMutation(),
    onSuccess: async (data, variables) => {
      queryClient.setQueryData(get3QueryKey({ path: { ticketId: variables.path.ticketId } }), data)
      await queryClient.invalidateQueries({ queryKey: list9QueryKey() })
      setPickerOpen(false)
      const worker = data.ticket?.assignedWorkerId != null ? users.get(data.ticket.assignedWorkerId)?.name : undefined
      toast.success(`Ticket asignado a ${worker ?? "el administrativo seleccionado"}`)
    },
    onError: showApiErrorToast,
  })

  return (
    <Sheet open={ticket !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="w-full gap-0 data-[side=right]:sm:max-w-2xl">
        <SheetHeader className="border-b">
          <SheetTitle className="pr-8">{routing?.summary || current?.publicId || "Ticket"}</SheetTitle>
          <SheetDescription className="font-mono text-xs break-all">{current?.publicId}{current?.id && ` · ${current.id}`}</SheetDescription>
          {current && (
            <div className="flex flex-wrap gap-1.5 pt-1">
              <TicketStateBadge state={current.state} />
              <TicketPriorityBadge priority={current.currentPriority} />
              {current.requiresAttention && <Badge variant="outline" className="border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400"><IconAlertTriangle />Requiere atención</Badge>}
              {current.informationPending && <Badge variant="outline" className="border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400"><IconInfoCircle />Información pendiente</Badge>}
            </div>
          )}
        </SheetHeader>

        {current && (
          <div className="min-h-0 flex-1 space-y-6 overflow-y-auto p-4">
            <section className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <h3 className="font-heading font-medium">Administrativo asignado</h3>
                {canAssign && (
                  <Button size="sm" variant="outline" disabled={assign.isPending} onClick={() => setPickerOpen(true)}>
                    <IconUserCheck />
                    {assign.isPending ? "Asignando…" : assignedId != null ? "Reasignar" : "Asignar"}
                  </Button>
                )}
              </div>
              <div className="rounded-lg border p-3 text-sm">
                <TicketWorker workerId={assignedId} users={users} />
              </div>
              {canManage && !manageable && <p className="text-xs text-muted-foreground">El ticket no tiene una gestión activa en M8: no se puede asignar.</p>}
              {canManage && manageable && !canPickUsers && <p className="text-xs text-muted-foreground">Para asignar necesitás además el permiso users:view.</p>}
            </section>

            <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
              <Field label="Estado en M8"><TicketStateBadge state={current.state} /></Field>
              <Field label="Estado en origen">{ticketCoreStatusLabel(current.currentStatus)}</Field>
              <Field label="Prioridad"><TicketPriorityBadge priority={current.currentPriority} /></Field>
              <Field label="Área responsable">{current.responsibleAreaId ?? "—"}</Field>
              <Field label="Ciudadano" wide>
                {current.anonymous ? <span className="text-muted-foreground">Anónimo</span> : <Mono>{current.citizenId}</Mono>}
              </Field>
              <Field label="Solicitud vinculada" wide>
                {current.applicationId ? (
                  <Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: current.applicationId }} className="inline-flex items-center gap-1 font-mono text-xs text-primary underline-offset-4 hover:underline break-all">
                    <IconFileDescription className="size-4 shrink-0" />{current.applicationId}
                  </Link>
                ) : <span className="text-muted-foreground">Sin vincular</span>}
              </Field>
              <Field label="Última actualización en origen">{formatDateTime(current.sourceUpdatedAt)}</Field>
              <Field label="Vencimiento de resolución">{formatDateTime(routing?.resolutionDueAt)}</Field>
              <Field label="Correlación" wide><Mono>{detail.data?.correlationId}</Mono></Field>
              <Field label="Causalidad de respuesta" wide><Mono>{detail.data?.responseCausationId}</Mono></Field>
              {current.closedReason && <Field label="Motivo de cierre" wide><p className="whitespace-pre-wrap">{current.closedReason}</p></Field>}
              {detail.data?.reopenReason && <Field label="Motivo de reapertura" wide><p className="whitespace-pre-wrap">{detail.data.reopenReason}</p></Field>}
            </dl>

            {detail.isPending ? <Skeleton className="h-48 rounded-xl" /> : detail.isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudo cargar el detalle del ticket: {detail.error.message ?? "el servidor no respondió."}</p>
                <Button size="sm" variant="outline" onClick={() => void detail.refetch()}>Reintentar</Button>
              </div>
            ) : <>
              <Section title="Derivación">
                <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                  <Field label="Tipo de pedido">{routing?.requestType ?? "—"}</Field>
                  <Field label="Tipo de ticket">{routing?.ticketType ?? "—"}</Field>
                  <Field label="Categoría">{routing?.category ?? "—"}</Field>
                  <Field label="Subcategoría">{routing?.subcategory ?? "—"}</Field>
                  <Field label="Descripción" wide><p className="whitespace-pre-wrap">{routing?.description || "—"}</p></Field>
                  {routing?.reassignment && <Field label="Reasignación" wide>Derivado nuevamente desde otra área</Field>}
                </dl>
              </Section>

              {routing?.location && (
                <Section title="Ubicación">
                  <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                    <Field label="Dirección" wide>{routing.location.addressLine || [routing.location.street, routing.location.streetNumber].filter(Boolean).join(" ") || "—"}</Field>
                    {routing.location.reference && <Field label="Referencia" wide>{routing.location.reference}</Field>}
                    {routing.location.neighborhoodId && <Field label="Barrio" wide><Mono>{routing.location.neighborhoodId}</Mono></Field>}
                  </dl>
                </Section>
              )}

              {routing?.escalation?.active && (
                <Section title="Escalamiento">
                  <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                    <Field label="Motivo">{routing.escalation.reasonCode ?? "—"}</Field>
                    <Field label="Escalado">{formatDateTime(routing.escalation.escalatedAt)}</Field>
                  </dl>
                </Section>
              )}

              {routing?.formData && Object.keys(routing.formData).length > 0 && (
                <Section title="Formulario">
                  <dl className="divide-y rounded-lg border text-sm">
                    {Object.entries(routing.formData).map(([key, value]) => (
                      <div key={key} className="grid gap-1 p-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,2fr)] sm:gap-4">
                        <dt className="font-mono text-xs text-muted-foreground break-all">{key}</dt>
                        <dd className="min-w-0 break-words">{formatValue(value)}</dd>
                      </div>
                    ))}
                  </dl>
                </Section>
              )}

              {detail.data?.informationResponse && (
                <Section title="Respuesta de información">
                  <JsonBlock value={detail.data.informationResponse} />
                </Section>
              )}

              <Section title="Adjuntos">
                {attachments.length === 0 ? <p className="text-sm text-muted-foreground">Sin adjuntos.</p> : (
                  <ul className="divide-y rounded-lg border text-sm">
                    {attachments.map((attachment, index) => (
                      <li key={`${attachment.url}-${index}`} className="flex items-center gap-3 p-3">
                        <IconPaperclip className="size-4 shrink-0 text-muted-foreground" />
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-medium">{attachment.fileName}</p>
                          <p className="truncate text-xs text-muted-foreground">{[attachment.contentType, formatFileSize(attachment.sizeBytes)].filter(Boolean).join(" · ")}</p>
                        </div>
                        <Button size="sm" variant="ghost" render={<a href={attachment.url} target="_blank" rel="noopener noreferrer" />} nativeButton={false}>
                          <IconExternalLink />Abrir
                        </Button>
                      </li>
                    ))}
                  </ul>
                )}
              </Section>

              <details className="group rounded-lg border">
                <summary className="cursor-pointer px-3 py-2 text-sm font-medium">Datos de derivación (JSON)</summary>
                <div className="border-t p-3"><JsonBlock value={detail.data?.routing} /></div>
              </details>
            </>}
          </div>
        )}
      </SheetContent>

      {pickerOpen && current?.id && (
        <UserSelectionDialog
          open
          onOpenChange={(next) => { if (!assign.isPending) setPickerOpen(next) }}
          selectedUserId={assignedId}
          title={assignedId != null ? "Reasignar ticket" : "Asignar ticket"}
          description="Elegí al administrativo que va a gestionar el ticket. Tiene que estar activo y tener un rol con el permiso tickets:management:manage."
          onSelect={(selected) => {
            if (selected.id == null || selected.id === assignedId) { setPickerOpen(false); return }
            assign.mutate({ path: { ticketId: current.id! }, body: { workerId: selected.id } })
          }}
        />
      )}
    </Sheet>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-2">
      <h3 className="font-heading font-medium">{title}</h3>
      {children}
    </section>
  )
}

function Field({ label, wide, children }: { label: string; wide?: boolean; children: ReactNode }) {
  return (
    <div className={cn("min-w-0", wide && "col-span-2")}>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-0.5">{children}</dd>
    </div>
  )
}

function Mono({ children }: { children?: string }) {
  return children ? <span className="font-mono text-xs break-all">{children}</span> : <span className="text-muted-foreground">—</span>
}

function JsonBlock({ value }: { value: unknown }) {
  return <pre className="max-h-96 overflow-auto rounded-lg border bg-muted/40 p-3 font-mono text-xs leading-relaxed">{JSON.stringify(value ?? null, null, 2)}</pre>
}

function formatValue(value: unknown): ReactNode {
  if (value == null || value === "") return <span className="text-muted-foreground">—</span>
  if (typeof value === "boolean") return value ? "Sí" : "No"
  if (typeof value === "string" || typeof value === "number") return String(value)
  return <pre className="overflow-auto font-mono text-xs">{JSON.stringify(value, null, 2)}</pre>
}
