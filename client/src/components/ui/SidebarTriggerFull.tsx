"use client"

import { IconLayoutSidebar } from "@tabler/icons-react";
import { useSidebar } from "@/components/ui/sidebar";

export function SidebarTriggerFull({
  className,
  ...props
}: React.ComponentProps<"button">) {
  const { toggleSidebar } = useSidebar()

  return (
    <button
      className={"group w-full h-full flex items-center justify-center hover:bg-primary/5 transition-colors duration-300 " + className}
      onClick={() => toggleSidebar()}
      {...props}
    >
      <IconLayoutSidebar className="size-4.5 xl:size-5 group-active:scale-[0.97]" />
      <span className="sr-only">Toggle Sidebar</span>
    </button>
  )
}