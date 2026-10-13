import type { ReactNode } from "react"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"

export function ActivityPage({ title, portal = false, children, pagination }: {
  title?: string
  portal?: boolean
  children: ReactNode
  pagination?: ReactNode
}) {
  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={title ? [
          { label: "Actividades", to: portal ? "/portal/actividades" : "/gestion/actividades" },
          { label: title },
        ] : [{ label: "Actividades" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 p-4 lg:p-6">{children}</main>
        </div>
        {pagination}
      </SidebarShellContent>
    </SidebarShell>
  )
}
