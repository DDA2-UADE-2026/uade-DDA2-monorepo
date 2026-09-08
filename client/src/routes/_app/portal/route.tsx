import { CatchBoundary, Outlet, createFileRoute, redirect, useLocation } from "@tanstack/react-router"

import { PortalSidebar } from "@/components/layout/PortalSidebar"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"
import { SectionErrorFallback } from "@/components/errors/SectionErrorFallback"

export const Route = createFileRoute("/_app/portal")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "CIUDADANO") {
      throw redirect({ to: "/gestion", replace: true })
    }
  },
  component: PortalLayout,
})

function PortalLayout() {
  const { pathname } = useLocation()

  return (
    <SidebarProvider>
      <PortalSidebar />
      <SidebarInset>
        {/* Wrapped here (not via the route's `errorComponent`) so a crash in
            a nested page only replaces this slot — the sidebar keeps
            rendering instead of disappearing with the rest of the layout. */}
        <CatchBoundary
          getResetKey={() => pathname}
          errorComponent={(props) => <SectionErrorFallback {...props} homeHref="/portal" />}
        >
          <Outlet />
        </CatchBoundary>
      </SidebarInset>
    </SidebarProvider>
  )
}
