import { Link } from "@tanstack/react-router"
import { SidebarHeader } from "@/components/ui/sidebar"
import AppLogoIconThemed from "@/components/branding/AppLogoIconThemed"
import AppLogoTitleThemed from "@/components/branding/AppLogoTitleThemed"

/** Shared sidebar header — just the app logo icon+text, collapsing to icon-only. */
function AppSidebarHeader() {
  return (
    <SidebarHeader className="border-b border-sidebar-border h-12 lg:h-14 flex items-center justify-center relative overflow-hidden">
      <Link
        to="/"
        className="flex w-full items-center gap-2 font-semibold group-data-[collapsible=icon]:justify-start justify-center"
      >
        <AppLogoIconThemed className="shrink-0 z-10! h-6.5" />
        <AppLogoTitleThemed className="truncate z-15! h-7" />
      </Link>
      <span className="absolute -bottom-3 left-1/2 -translate-1/2 h-6 w-36 z-0
        blur-xl bg-emerald-400 dark:bg-emerald-400 opacity-40 dark:opacity-60"/>
    </SidebarHeader>
  )
}

export { AppSidebarHeader }
