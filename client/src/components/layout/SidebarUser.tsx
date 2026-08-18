import { Link } from "@tanstack/react-router"
import { IconArrowsExchange, IconDotsVertical, IconLogout, IconUserCircle } from "@tabler/icons-react"

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { SidebarMenuButton, SidebarMenuItem, useSidebar } from "@/components/ui/sidebar"

// Mock signed-in user until the real session/auth wiring lands.
const MOCK_USER = {
  name: "Usuario",
  email: "usuario@municipio.gob.ar",
  initials: "US",
}

/** Shared sidebar footer user menu — used by both PortalSidebar and GestionSidebar. */
function SidebarUser() {
  const { isMobile } = useSidebar()

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
            <AvatarFallback>{MOCK_USER.initials}</AvatarFallback>
          </Avatar>
          <div className="grid flex-1 text-left text-sm leading-tight">
            <span className="truncate font-medium">{MOCK_USER.name}</span>
            <span className="truncate text-xs text-sidebar-foreground/70">{MOCK_USER.email}</span>
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
                  <AvatarFallback>{MOCK_USER.initials}</AvatarFallback>
                </Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">{MOCK_USER.name}</span>
                  <span className="truncate text-xs text-muted-foreground">{MOCK_USER.email}</span>
                </div>
              </div>
            </DropdownMenuLabel>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            className="rounded-lg"
            onClick={() => {
              // TODO: link to the real profile route once it exists.
              console.log("go to profile")
            }}
          >
            <IconUserCircle />
            Mi perfil
          </DropdownMenuItem>
          <DropdownMenuItem className="rounded-lg" render={<Link to="/seleccionar-rol" />}>
            <IconArrowsExchange />
            Cambiar de rol
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            variant="destructive"
            className="rounded-lg"
            onClick={() => {
              // TODO: wire up to the real sign-out flow once it's available.
              console.log("sign out")
            }}
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
