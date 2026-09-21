import { IconAlertCircle, IconFileText } from "@tabler/icons-react"
import type { ReactNode } from "react"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs, type OutletBreadcrumbItem } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import type { ApplicationResponse, ErrorResponse } from "@/generated/types.gen"
import { applicationStatusLabels } from "@/lib/application-flow"

export function ApplicationPage({ breadcrumbs, children }: { breadcrumbs: OutletBreadcrumbItem[]; children: ReactNode }) {
  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={breadcrumbs} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full max-w-5xl space-y-6 p-4 lg:p-6">{children}</main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

export function ApplicationStatusBadge({ status }: { status: ApplicationResponse["status"] }) {
  return <Badge variant={status === "APPROVED" ? "default" : status === "REJECTED" ? "destructive" : "secondary"}>
    {status ? applicationStatusLabels[status] : "Sin estado"}
  </Badge>
}

export function ApplicationError({ error, title, retry }: { error: ErrorResponse; title: string; retry?: () => void }) {
  return (
    <Alert variant="destructive">
      <IconAlertCircle />
      <AlertTitle>{error.status === 404 ? "No encontramos la solicitud o el programa" : title}</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-3">
        <span>{error.message ?? "Intentá nuevamente en unos instantes."}</span>
        {error.fields?.length ? <ul className="list-disc pl-4">{error.fields.map((field, index) => <li key={index}>{field.message}</li>)}</ul> : null}
        {retry && <Button size="sm" variant="outline" onClick={retry}>Reintentar</Button>}
      </AlertDescription>
    </Alert>
  )
}

export function ApplicationLoading() {
  return <div role="status" aria-label="Cargando solicitud" className="space-y-5">
    <Skeleton className="h-8 w-64" /><Skeleton className="h-36 w-full" /><Skeleton className="h-60 w-full" />
    <span className="sr-only">Cargando solicitud…</span>
  </div>
}

export function ApplicationHeading({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
    <div>
      <h1 className="flex items-center gap-2 font-heading text-2xl font-medium"><IconFileText className="size-6 text-primary" />{title}</h1>
      {description && <p className="mt-2 text-sm text-muted-foreground">{description}</p>}
    </div>
    {action}
  </div>
}
