import { Link } from "@tanstack/react-router"
import { IconCube } from "@tabler/icons-react"

import { BRAND_NAME } from "@/components/auth/AuchBrand"
import { SidebarHeader } from "@/components/ui/sidebar"

/** Shared sidebar header — just the app logo icon+text, collapsing to icon-only. */
function AppSidebarHeader() {
  return (
    <SidebarHeader className="border-b border-sidebar-border h-12 lg:h-14 flex items-center justify-center relative overflow-hidden">
      <Link
        to="/"
        className="flex w-full items-center gap-2 overflow-hidden font-heading text-sm font-semibold group-data-[collapsible=icon]:justify-start justify-center"
      >
        <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-primary text-primary-foreground">
          <IconCube className="size-4.5" />
        </div>
        <span className="truncate">{BRAND_NAME}</span>
      </Link>
      <span className="absolute -bottom-3 left-1/2 -translate-1/2 h-6 w-36 z-0
        blur-xl bg-blue-500 opacity-50 dark:inline hidden"/>
    </SidebarHeader>
  )
}

export { AppSidebarHeader }
