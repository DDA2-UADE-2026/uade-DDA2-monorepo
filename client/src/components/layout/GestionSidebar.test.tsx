import type { ReactNode } from "react"
import { render, screen } from "@testing-library/react"
import { expect, it, vi } from "vitest"

import { SidebarProvider } from "@/components/ui/sidebar"
import { GestionSidebar } from "./GestionSidebar"

const auth = vi.hoisted(() => ({ permissions: [] as string[] }))
vi.mock("@/hooks/use-auth", () => ({ useMe: () => ({ data: { user: { permissions: auth.permissions } } }) }))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  useLocation: () => ({ pathname: "/gestion/programas" }),
  Link: ({ children, to, disabled, ...props }: { children?: ReactNode; to: string; disabled?: boolean }) => (
    <a {...props} href={disabled ? undefined : to} aria-disabled={disabled || undefined}>{children}</a>
  ),
}))
vi.mock("@/components/layout/AppSidebarHeader", () => ({ AppSidebarHeader: () => <div>Portal municipal</div> }))
vi.mock("@/components/layout/SidebarUser", () => ({ SidebarUser: () => <div>Usuario actual</div> }))
vi.mock("@/components/BackendStatusPill", () => ({ BackendStatusPill: () => <span>Backend operativo</span> }))

it("muestra accesos según permisos de sesión y marca la ruta actual", () => {
  auth.permissions = ["centers:management:view", "programs:management:view", "applications:management:view", "users:view"]
  render(<SidebarProvider><GestionSidebar /></SidebarProvider>)

  expect(screen.getByText("Gestión municipal")).toBeInTheDocument()
  expect(screen.getByText("Seguridad y Análisis")).toBeInTheDocument()
  expect(screen.getByText("Debug y estado")).toBeInTheDocument()
  expect(screen.getByText("Portal municipal")).toBeInTheDocument()
  expect(screen.getByText("Usuario actual")).toBeInTheDocument()
  expect(screen.getByText("Backend operativo")).toBeInTheDocument()

  expect(screen.getByRole("link", { name: "Programas" })).toHaveAttribute("href", "/gestion/programas")
  expect(screen.getByRole("link", { name: "Programas" })).toHaveAttribute("data-active")
  expect(screen.getByRole("link", { name: "Solicitudes" })).toHaveAttribute("href", "/gestion/solicitudes")
  expect(screen.getByRole("link", { name: "Usuarios" })).toHaveAttribute("href", "/gestion/usuarios")
  const restrictedRolesLink = screen.getByText("Roles y permisos").closest("a")
  expect(restrictedRolesLink).toHaveAttribute("aria-disabled", "true")
  expect(restrictedRolesLink).not.toHaveAttribute("href")
  expect(screen.getByRole("link", { name: "API local (Autogen)" })).toHaveAttribute("href", "/gestion/debug/documentacion-local")
})
