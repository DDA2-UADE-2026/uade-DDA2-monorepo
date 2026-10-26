import type { ComponentType } from "react"
import { Link } from "@tanstack/react-router"
import { IconLock } from "@tabler/icons-react"

import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar"

export interface SidebarNavSubItem {
  title: string
  url: string
  icon?: ComponentType<{ className?: string }>
  requiredPermissions?: string[]
}

export interface SidebarNavItem {
  title: string
  url: string
  icon: ComponentType<{ className?: string }>
  items?: readonly SidebarNavSubItem[]
  requiredPermissions?: string[]
}

/** Shared nav-item renderer for PortalSidebar/GestionSidebar — parent link plus an optional flat submenu. */
function SidebarNavMenu({ items, pathname, permissions }: { items: readonly SidebarNavItem[]; pathname: string; permissions: readonly string[] }) {
  return (
    <SidebarMenu>
      {items.map((item) => {
        const sectionRoot = item.url === "/gestion" || item.url === "/portal"
        const isActive = pathname === item.url || (!sectionRoot && pathname.startsWith(`${item.url}/`))
        const locked = !!item.requiredPermissions?.length && !item.requiredPermissions.some((p) => permissions.includes(p))

        return (
          <SidebarMenuItem key={item.title}>
            <SidebarMenuButton
              isActive={isActive}
              tooltip={locked ? `${item.title} (sin acceso)` : item.title}
              render={<Link disabled={locked} to={item.url} />}
            >
              <item.icon />
              <span>{item.title}</span>
              {locked && <IconLock className="ml-auto" aria-label="Sin acceso" />}
            </SidebarMenuButton>
            {item.items?.length ? (
              <SidebarMenuSub>
                {item.items.map((sub) => {
                  const subLocked = !!sub.requiredPermissions?.length && !sub.requiredPermissions.some((p) => permissions.includes(p))
                  return <SidebarMenuSubItem key={sub.title}>
                    <SidebarMenuSubButton
                      isActive={pathname === sub.url}
                      render={<Link disabled={subLocked} to={sub.url} />}
                    >
                      {sub.icon && <sub.icon />}
                      <span>{sub.title}</span>
                      {subLocked && <IconLock className="ml-auto" aria-label="Sin acceso" />}
                    </SidebarMenuSubButton>
                  </SidebarMenuSubItem>
                })}
              </SidebarMenuSub>
            ) : null}
          </SidebarMenuItem>
        )
      })}
    </SidebarMenu>
  )
}

export { SidebarNavMenu }
