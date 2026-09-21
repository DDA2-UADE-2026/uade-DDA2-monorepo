import { CatchBoundary, Outlet, createFileRoute, redirect, useLocation } from "@tanstack/react-router"

import { GestionSidebar } from "@/components/layout/GestionSidebar"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"
import { SectionErrorFallback } from "@/components/errors/SectionErrorFallback"
import { RouteErrorPage } from "@/components/errors/RouteErrorPage"
import { RouteNotFoundPage } from "@/components/errors/RouteNotFoundPage"

export const Route = createFileRoute("/_app/gestion")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() === "CIUDADANO") {
      throw redirect({ to: "/portal", replace: true })
    }
  },
  component: GestionLayout,
  // Boundary for the layout itself (its `beforeLoad`, its chrome) — a crash
  // here leaves no sidebar to render a section-sized fallback inside, so it
  // gets the full-page treatment. Pages below are caught by the
  // `CatchBoundary` further down instead.
  errorComponent: RouteErrorPage,
  // Claims unmatched URLs under /gestion (fuzzy not-found mode picks the
  // deepest route that defines this) so the way back points at gestión.
  notFoundComponent: () => <RouteNotFoundPage homeHref="/gestion" homeLabel="Volver a gestión" />,
})

function GestionLayout() {
  const { pathname } = useLocation()

  return (
    <SidebarProvider>
      <GestionSidebar />
      <SidebarInset>
        {/* Wrapped here (not via the route's `errorComponent`) so a crash in
            a nested page only replaces this slot — the sidebar keeps
            rendering instead of disappearing with the rest of the layout. */}
        <CatchBoundary
          getResetKey={() => pathname}
          errorComponent={(props) => <SectionErrorFallback {...props} homeHref="/gestion" />}
        >
          <Outlet />
        </CatchBoundary>
      </SidebarInset>
    </SidebarProvider>
  )
}
