import { IconArrowLeft, IconCheck, IconClipboardList } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"
import { z } from "zod"

import { ApplicationDocuments } from "@/components/applications/ApplicationDocuments"
import { ApplicationError, ApplicationHeading, ApplicationLoading, ApplicationPage, ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { get1Options } from "@/generated/@tanstack/react-query.gen"
import { formatApplicationDate, isApplicationResolved } from "@/lib/application-flow"

export const Route = createFileRoute("/_app/portal/solicitudes/$solicitudId/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useParams()
  const validId = z.uuid().safeParse(solicitudId).success
  const query = useQuery({ ...get1Options({ path: { id: solicitudId } }), enabled: validId })
  const application = query.data
  const pending = application?.pendingDocuments?.length ?? 0
  return <ApplicationPage breadcrumbs={[
    { label: "Mis solicitudes", to: "/portal/solicitudes" },
    { label: application?.applicationNumber != null ? `Solicitud N.º ${application.applicationNumber}` : "Detalle de solicitud" },
  ]}>
    <Button size="sm" variant="ghost" render={<Link to="/portal/solicitudes" search={{ page: 1 }} />}><IconArrowLeft />Mis solicitudes</Button>
    {!validId ? <ApplicationError error={{ status: 404, message: "El enlace de la solicitud no es válido." }} title="Solicitud no encontrada" /> : query.isPending ? <ApplicationLoading /> : query.isError ? <ApplicationError error={query.error} title="No pudimos cargar la solicitud" retry={() => query.refetch()} /> : application ? <>
      <ApplicationHeading title={`Solicitud N.º ${application.applicationNumber ?? "—"}`} description={`Presentada el ${formatApplicationDate(application.submittedAt)}`} action={<ApplicationStatusBadge status={application.status} />} />
      <Card>
        <CardHeader><CardTitle className="font-heading text-xl">{application.programName ?? "Programa solicitado"}</CardTitle><CardDescription>{application.programEditionName ?? "Edición del programa"}</CardDescription></CardHeader>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-2">
          <div><p className="text-muted-foreground">Fecha de presentación</p><p className="mt-1 font-medium">{formatApplicationDate(application.submittedAt)}</p></div>
          <div><p className="text-muted-foreground">Última actualización</p><p className="mt-1 font-medium">{formatApplicationDate(application.updatedAt)}</p></div>
        </CardContent>
      </Card>
      {!isApplicationResolved(application.status) && (pending > 0 ? <Alert><IconClipboardList /><AlertTitle>Tenés {pending} documento{pending === 1 ? " pendiente" : "s pendientes"}</AlertTitle><AlertDescription>Adjuntá los documentos faltantes o reemplazá los observados para completar tu documentación.</AlertDescription></Alert> : <Alert><IconCheck /><AlertTitle>Sin documentos obligatorios faltantes u observados</AlertTitle><AlertDescription>Las entregas pendientes de revisión se muestran a continuación. Podés volver a consultar el estado desde Mis solicitudes.</AlertDescription></Alert>)}
      <ApplicationDocuments key={solicitudId} application={{ ...application, id: solicitudId }} />
    </> : null}
  </ApplicationPage>
}
