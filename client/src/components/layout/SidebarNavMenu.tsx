import type { ComponentType } from "react"
import { Link } from "@tanstack/react-router"

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
}

export interface SidebarNavItem {
  title: string
  url: string
  icon: ComponentType<{ className?: string }>
  items?: readonly SidebarNavSubItem[]
}

/** Shared nav-item renderer for PortalSidebar/GestionSidebar — parent link plus an optional flat submenu. */
function SidebarNavMenu({ items, pathname }: { items: readonly SidebarNavItem[]; pathname: string }) {
  return (
    <SidebarMenu>
      {items.map((item) => (
        <SidebarMenuItem key={item.title}>
          <SidebarMenuButton
            isActive={pathname === item.url}
            tooltip={item.title}
            // TODO: drop the `as string` cast once this route exists as a real Route (see notes/routes.md).
            render={<Link to={item.url as string} />}
          >
            <item.icon />
            <span>{item.title}</span>
          </SidebarMenuButton>
          {item.items?.length ? (
            <SidebarMenuSub>
              {item.items.map((sub) => (
                <SidebarMenuSubItem key={sub.title}>
                  <SidebarMenuSubButton
                    isActive={pathname === sub.url}
                    // TODO: drop the `as string` cast once this route exists as a real Route (see notes/routes.md).
                    render={<Link to={sub.url as string} />}
                  >
                    {sub.title}
                  </SidebarMenuSubButton>
                </SidebarMenuSubItem>
              ))}
            </SidebarMenuSub>
          ) : null}
        </SidebarMenuItem>
      ))}
    </SidebarMenu>
  )
}

export { SidebarNavMenu }
