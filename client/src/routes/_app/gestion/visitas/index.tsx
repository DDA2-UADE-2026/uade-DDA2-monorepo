import { useState } from "react"
import { createFileRoute } from "@tanstack/react-router"
import { SocialRescheduleInbox } from "@/components/applications/SocialRescheduleInbox"
import { SocialVisitsBoard } from "@/components/applications/SocialVisitsBoard"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/_app/gestion/visitas/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const permissions = user.permissions ?? []
  const [tab, setTab] = useState<"visits" | "requests">(permissions.includes("applications:visits:view") ? "visits" : "requests")
  return <SidebarShell><OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={[{ label: "Visitas y entrevistas" }]} /></OutletNavSticky><SidebarShellContent><div className="min-h-0 flex-1 overflow-auto p-4 lg:p-6 space-y-5">
    <div><h1 className="font-heading text-2xl font-medium">Visitas y entrevistas</h1><p className="text-sm text-muted-foreground">Agenda social y pedidos de reprogramación de ciudadanos.</p></div>
    <div className="flex gap-2" role="tablist" aria-label="Secciones de visitas">{permissions.includes("applications:visits:view") && <Button role="tab" aria-selected={tab === "visits"} variant={tab === "visits" ? "default" : "outline"} onClick={() => setTab("visits")}>Agenda</Button>}{permissions.includes("applications:visits:reschedule-requests:view") && <Button role="tab" aria-selected={tab === "requests"} variant={tab === "requests" ? "default" : "outline"} onClick={() => setTab("requests")}>Reprogramaciones</Button>}</div>
    {tab === "visits" ? <SocialVisitsBoard permissions={permissions} currentUserId={user.id} /> : <SocialRescheduleInbox permissions={permissions} currentUserId={user.id} />}
  </div></SidebarShellContent></SidebarShell>
}
