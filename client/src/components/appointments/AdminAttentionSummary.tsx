import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import type { CommunityAttentionResponse } from "@/generated/types.gen"
import { APPOINTMENT_TIME_ZONE, formatAppointmentDate } from "@/lib/appointment-flow"

export function AdminAttentionSummary({ attention }: { attention?: CommunityAttentionResponse | null }) {
  return (
    <Card>
      <CardHeader><CardTitle>Constancia de asistencia</CardTitle></CardHeader>
      <CardContent className="space-y-2 text-sm">
        {!attention ? (
          <p className="text-muted-foreground">Sin registrar.</p>
        ) : (
          <>
            <Badge variant="outline">{attention.result === "ATENDIDO" ? "Atendido" : "Ausente"}</Badge>
            {attention.result === "ATENDIDO" && <>
              <p><strong>Fecha de atención:</strong> {formatAppointmentDate(attention.attendedOn ? `${attention.attendedOn}T12:00:00-03:00` : undefined)}</p>
              <p className="whitespace-pre-wrap wrap-break-word"><strong>Servicio prestado:</strong> {attention.description ?? "—"}</p>
            </>}
            {attention.updatedAt && <p className="text-muted-foreground">Última actualización: {new Intl.DateTimeFormat("es-AR", {
              dateStyle: "short", timeStyle: "short", timeZone: APPOINTMENT_TIME_ZONE,
            }).format(new Date(attention.updatedAt))}</p>}
          </>
        )}
      </CardContent>
    </Card>
  )
}
