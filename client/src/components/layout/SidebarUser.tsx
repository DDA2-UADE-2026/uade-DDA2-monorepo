import { Link, useLocation, useNavigate } from "@tanstack/react-router"
import { IconArrowsExchange, IconDotsVertical, IconLogout, IconPalette, IconUserCircle } from "@tabler/icons-react"

import { ThemeMenuItems } from "@/components/ThemeMenuItems"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { SidebarMenuButton, SidebarMenuItem, useSidebar } from "@/components/ui/sidebar"
import { useLogout, useMe } from "@/hooks/use-auth"
import { getUserInitials } from "@/lib/user-display"

/** Shared sidebar footer user menu — used by both PortalSidebar and GestionSidebar. */
function SidebarUser() {
  const { isMobile } = useSidebar()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { data } = useMe()
  const logout = useLogout()
  const perfilUrl = pathname.startsWith("/portal") ? "/portal/perfil" : "/gestion/perfil"

  const displayName = data?.user?.name || data?.user?.username || "Usuario"
  const email = data?.user?.email ?? ""
  const initials = getUserInitials(displayName)

  const handleLogout = () => {
    logout()
    navigate({ to: "/login" })
  }

  return (
    <SidebarMenuItem>
      <DropdownMenu>
        <DropdownMenuTrigger
          render={
            <SidebarMenuButton
              size="lg"
              className="data-open:bg-sidebar-accent data-open:text-sidebar-accent-foreground"
            />
          }
        >
          <Avatar size="sm">
            <AvatarFallback>{initials}</AvatarFallback>
          </Avatar>
          <div className="grid flex-1 text-left text-sm leading-tight">
            <span className="truncate font-medium">{displayName}</span>
            <span className="truncate text-xs text-sidebar-foreground/70">{email}</span>
          </div>
          <IconDotsVertical className="ml-auto size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent
          className="min-w-56 rounded-xl"
          side={isMobile ? "bottom" : "right"}
          align="end"
          sideOffset={4}
        >
          <DropdownMenuGroup>
            <DropdownMenuLabel className="p-0 font-normal">
              <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                <Avatar size="sm">
                  <AvatarFallback>{initials}</AvatarFallback>
                </Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">{displayName}</span>
                  <span className="truncate text-xs text-muted-foreground">{email}</span>
                </div>
              </div>
            </DropdownMenuLabel>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuItem className="rounded-lg" render={<Link to={perfilUrl} />}>
            <IconUserCircle />
            Mi perfil
          </DropdownMenuItem>
          <DropdownMenuItem className="rounded-lg" render={<Link to="/seleccionar-rol" />}>
            <IconArrowsExchange />
            Cambiar de rol
          </DropdownMenuItem>
          <DropdownMenuSub >
            <DropdownMenuSubTrigger className="rounded-lg">
              <IconPalette />
              Tema
            </DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              <ThemeMenuItems />
            </DropdownMenuSubContent>
          </DropdownMenuSub>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            variant="destructive"
            className="rounded-lg"
            onClick={handleLogout}
          >
            <IconLogout />
            Cerrar sesión
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </SidebarMenuItem>
  )
}

export { SidebarUser }
