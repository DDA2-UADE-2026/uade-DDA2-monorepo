import { useState } from "react"
import { IconLayoutSidebar } from "@tabler/icons-react"

import { Kbd, KbdGroup } from "@/components/ui/kbd"
import { SidebarGroup, SidebarGroupLabel } from "@/components/ui/sidebar"

/** Hint for the Cmd/Ctrl+B shortcut, hidden once the sidebar collapses to icons. */
function KeyboardNotice() {
  const [isMac] = useState(
    () => typeof navigator !== "undefined" && /Mac|iPhone|iPad|iPod/.test(navigator.platform),
  )

  return (
    <SidebarGroup className="mt-auto border border-transparent">
      <SidebarGroupLabel className="m-0 hidden flex-col items-center justify-center gap-1.5 text-center text-muted-foreground opacity-70 md:flex">
        <span className="text-nowrap whitespace-nowrap">Colapsá la barra lateral con</span>
        <div className="flex flex-row items-center justify-center gap-1.5 text-nowrap whitespace-nowrap">
          <Kbd>
            <IconLayoutSidebar className="size-3.5! text-inherit" />
          </Kbd>
          <span className="text-nowrap whitespace-nowrap">o usá</span>
          <KbdGroup>
            <Kbd>{isMac ? "⌘" : "Ctrl"}</Kbd>
            <Kbd>B</Kbd>
          </KbdGroup>
        </div>
      </SidebarGroupLabel>
    </SidebarGroup>
  )
}

export { KeyboardNotice }
