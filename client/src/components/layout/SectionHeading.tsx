import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

/** Encabezado de sección para las tarjetas y listados de la home del portal. */
export function SectionHeading({
  id,
  title,
  description,
  action,
  className,
}: {
  id?: string
  title: string
  description?: string
  action?: ReactNode
  className?: string
}) {
  return (
    <div className={cn("flex items-end justify-between gap-3 px-1", className)}>
      <div className="space-y-0">
        <h2 id={id} className="font-heading text-lg font-medium">
          {title}
        </h2>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {action}
    </div>
  )
}
