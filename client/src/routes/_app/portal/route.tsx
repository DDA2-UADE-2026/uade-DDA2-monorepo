import { CatchBoundary, Outlet, createFileRoute, redirect, useLocation } from "@tanstack/react-router"

import { PortalSidebar } from "@/components/layout/PortalSidebar"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"
import { SectionErrorFallback } from "@/components/errors/SectionErrorFallback"
import { RouteErrorPage } from "@/components/errors/RouteErrorPage"
import { RouteNotFoundPage } from "@/components/errors/RouteNotFoundPage"

export const Route = createFileRoute("/_app/portal")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "CIUDADANO") {
      throw redirect({ to: "/gestion", replace: true })
    }
  },
  component: PortalLayout,
  // Boundary for the layout itself (its `beforeLoad`, its chrome) — a crash
  // here leaves no sidebar to render a section-sized fallback inside, so it
  // gets the full-page treatment. Pages below are caught by the
  // `CatchBoundary` further down instead.
  errorComponent: RouteErrorPage,
  // Claims unmatched URLs under /portal (fuzzy not-found mode picks the
  // deepest route that defines this) so the way back points at el portal.
  notFoundComponent: () => <RouteNotFoundPage homeHref="/portal" homeLabel="Volver al portal" />,
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
