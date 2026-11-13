import type { ReactNode } from "react"
import { render, screen } from "@testing-library/react"
import { expect, it, vi } from "vitest"

import { SidebarProvider } from "@/components/ui/sidebar"
import { PortalSidebar } from "./PortalSidebar"

const auth = vi.hoisted(() => ({ user: { permissions: ["applications:own:visits:view"] as string[] } as { permissions: string[] } | null }))
vi.mock("@/hooks/use-auth", () => ({ useMe: () => ({ data: auth.user ? { user: auth.user } : undefined }) }))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  useLocation: () => ({ pathname: "/portal/visitas" }),
  Link: ({ children, to, disabled, ...props }: { children?: ReactNode; to: string; disabled?: boolean }) => (
    <a {...props} href={disabled ? undefined : to} aria-disabled={disabled || undefined}>{children}</a>
  ),
}))
vi.mock("@/components/layout/AppSidebarHeader", () => ({ AppSidebarHeader: () => <div>Logo UADE</div> }))
vi.mock("@/components/layout/SidebarUser", () => ({ SidebarUser: () => <div>Ciudadana</div> }))

it("muestra la navegación ciudadana según la sesión y activa la ruta actual", () => {
  auth.user = { permissions: ["applications:own:visits:view"] }
  render(<SidebarProvider><PortalSidebar /></SidebarProvider>)

  expect(screen.getByText("Portal ciudadano")).toBeInTheDocument()
  expect(screen.getByText("Logo UADE")).toBeInTheDocument()
  expect(screen.getByText("Ciudadana")).toBeInTheDocument()
  expect(screen.getByRole("link", { name: "Mis visitas" })).toHaveAttribute("href", "/portal/visitas")
  expect(screen.getByRole("link", { name: "Mis visitas" })).toHaveAttribute("data-active")
  expect(screen.getByText("Mis solicitudes").closest("a")).toHaveAttribute("aria-disabled", "true")
  expect(screen.getByRole("link", { name: "Programas" })).toHaveAttribute("href", "/portal/programas")
})

it("mantiene solo los accesos públicos cuando la sesión todavía no está disponible", () => {
  auth.user = null
  render(<SidebarProvider><PortalSidebar /></SidebarProvider>)

  expect(screen.getByRole("link", { name: "Programas" })).toHaveAttribute("href", "/portal/programas")
  expect(screen.getByText("Mis solicitudes").closest("a")).toHaveAttribute("aria-disabled", "true")
})
