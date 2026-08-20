import type { ComponentProps } from "react"

import { Separator } from "@/components/ui/separator"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { cn } from "@/lib/utils"

/**
 * Per-page header bar — mounted by each route's own component, not the shared
 * layout, so every page controls its own title and action buttons. Only the
 * chrome (height, sidebar trigger, border) is shared; height matches
 * AppSidebarHeader so the two line up.
 */
function PageHeader({ className, children, ...props }: ComponentProps<"header">) {
  return (
    <header
      className={cn(
        "sticky top-0 z-40 flex h-14 shrink-0 items-center gap-2 border-b border-border bg-background/80 px-4 backdrop-blur-sm",
        className
      )}
      {...props}
    >
      <SidebarTrigger />
      <Separator orientation="vertical" className="h-14" />
      {children}
    </header>
  )
}

export { PageHeader }
