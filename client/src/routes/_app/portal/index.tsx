import { createFileRoute } from "@tanstack/react-router"

import { CitizenActivitiesSection } from "@/components/activities/CitizenActivitiesSection"
import { RecentApplications } from "@/components/applications/RecentApplications"
import { UpcomingVisits } from "@/components/applications/UpcomingVisits"
import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { PortalOverview } from "@/components/layout/PortalOverview"
import { ProgramTicker } from "@/components/programs/ProgramTicker"
import { UserAvatar } from "@/components/UserAvatar"
import { useMe } from "@/hooks/use-auth"

export const Route = createFileRoute("/_app/portal/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { data } = useMe()
  const displayName = data?.user?.name || data?.user?.username || "Usuario"
  const permissions = data?.user?.permissions ?? []

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Inicio" }]} />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="flex w-full flex-col gap-6 p-4 lg:p-6">
            <header className="flex w-full items-center gap-4 px-1 py-2 sm:py-3">
              <UserAvatar
                addBlob
                blobProps="blur-xl!"
                user={data?.user ?? { name: displayName }}
                className="size-12 sm:size-14"
                fallbackClassName="text-base"
              />
              <div className="min-w-0 space-y-1.5">
                <h1 className="truncate font-heading text-xl font-medium sm:text-2xl">
                  Hola, {displayName}
                </h1>
                <p className="text-sm text-muted-foreground">
                  Encontrá programas y actividades, y seguí tus solicitudes, turnos y acompañamientos.
                </p>
              </div>
            </header>

            <ProgramTicker />

            <UpcomingVisits />

            <PortalOverview permissions={permissions} />

            {permissions.includes("applications:own:view") && <RecentApplications />}

            {permissions.includes("activities:own:view") && <CitizenActivitiesSection canEnroll={permissions.includes("activities:own:enroll")} />}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
