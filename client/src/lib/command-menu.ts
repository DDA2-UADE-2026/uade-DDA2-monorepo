import type { ComponentType } from "react"
import type { SidebarNavItem } from "@/components/layout/SidebarNavMenu"

export interface CommandNavItem {
  title: string
  url: string
}

export interface CommandActionItem {
  title: string
  icon?: ComponentType<{ className?: string }>
  keywords?: string[]
  onSelect: () => void
}

/** Flattens sidebar nav items (parent + nested sub-items) into a flat list for the command menu. */
export function flattenNavItems(items: readonly SidebarNavItem[]): CommandNavItem[] {
  return items.flatMap((item) => [
    { title: item.title, url: item.url },
    ...(item.items ?? []).map((sub) => ({ title: sub.title, url: sub.url })),
  ])
}

// _auth has no sidebar to derive from — kept as its own flat list here, including
// /callback so the OAuth redirect target stays reachable from the palette too.
export const AUTH_NAV: readonly CommandNavItem[] = [
  { title: "Iniciar sesión", url: "/login" },
  { title: "Registrarse", url: "/register" },
  { title: "Seleccionar rol", url: "/seleccionar-rol" },
  { title: "Callback de autenticación", url: "/callback" },
]

/**
 * Extension point for the command menu: push entries here from any module
 * (e.g. a route file or feature component) to add custom actions without
 * touching AppCommandShortcut. Rendered as the "Acciones" group, ahead of
 * navigation entries.
 */
export const commandActions: CommandActionItem[] = []
