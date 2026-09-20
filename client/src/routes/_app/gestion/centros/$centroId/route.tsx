import { useQuery } from "@tanstack/react-query"
import { Link, Outlet, createFileRoute, redirect, useRouterState } from "@tanstack/react-router"

import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Badge } from "@/components/ui/badge"
import { getMunicipalCenterOptions } from "@/generated/@tanstack/react-query.gen"
import { cn } from "@/lib/utils"

export const Route = createFileRoute("/_app/gestion/centros/$centroId")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { centroId } = Route.useParams()
  const center = useQuery(getMunicipalCenterOptions({ path: { id: centroId } }))
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const base = `/gestion/centros/${centroId}`
  const tabs = [
    { label: "Servicios y profesionales", to: base },
    { label: "Agenda", to: `${base}/agenda` },
  ]
  const currentPath = pathname.replace(/\/+$/, "") || "/"
  const activePath = tabs
    .map((item) => item.to.replace(/\/+$/, "") || "/")
    .filter((itemPath) => currentPath === itemPath || currentPath.startsWith(`${itemPath}/`))
    .sort((left, right) => right.length - left.length)[0]

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs
          items={[
            { label: "Centros", to: "/gestion/centros" },
            { label: center.data?.name ?? "Centro" },
          ]}
        />
        {center.data && (
          <div className="ml-auto flex items-center gap-2">
            <Badge variant={center.data.active ? "default" : "secondary"}>
              {center.data.active ? "Activo" : "Inactivo"}
            </Badge>
          </div>
        )}
      </OutletNavSticky>
      <SidebarShellContent>
        <nav className="flex gap-6 overflow-x-auto border-b px-4 lg:px-6">
          {tabs.map((item) => {
            const itemPath = item.to.replace(/\/+$/, "") || "/"
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "shrink-0 whitespace-nowrap border-b-2 border-transparent px-1 py-3 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground",
                  itemPath === activePath && "border-primary text-foreground",
                )}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <Outlet />
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
