import { Link } from "@tanstack/react-router"
import type { ReactNode } from "react"

import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Button } from "@/components/ui/button"

type VisitsTab = "agenda" | "requests"

/** Encabezado y pestañas compartidas entre la agenda y los pedidos de reprogramación. */
export function VisitsSectionLayout({ tab, permissions, children }: { tab: VisitsTab; permissions: string[]; children: ReactNode }) {
  return <SidebarShell><OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={tab === "agenda" ? [{ label: "Visitas y entrevistas" }] : [{ label: "Visitas y entrevistas", to: "/gestion/visitas" }, { label: "Pedidos de reprogramación" }]} /></OutletNavSticky><SidebarShellContent><div className="min-h-0 flex-1 overflow-auto p-4 lg:p-6 space-y-5">
    <div><h1 className="font-heading text-2xl font-medium">Visitas y entrevistas</h1><p className="text-sm text-muted-foreground">Agenda social y pedidos de reprogramación de ciudadanos.</p></div>
    <nav className="flex gap-2" aria-label="Secciones de visitas">
      {permissions.includes("applications:visits:view") && <Button aria-current={tab === "agenda" ? "page" : undefined} variant={tab === "agenda" ? "default" : "outline"} render={<Link to="/gestion/visitas" />}>Agenda</Button>}
      {permissions.includes("applications:visits:reschedule-requests:view") && <Button aria-current={tab === "requests" ? "page" : undefined} variant={tab === "requests" ? "default" : "outline"} render={<Link to="/gestion/visitas/reprogramaciones" search={{ page: 1, estado: "PENDING" }} />}>Pedidos de reprogramación</Button>}
    </nav>
    {children}
  </div></SidebarShellContent></SidebarShell>
}
