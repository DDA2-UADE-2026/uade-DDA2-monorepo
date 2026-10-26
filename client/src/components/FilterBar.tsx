import type { FormEvent, ReactNode } from "react"

import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

/**
 * Search/filter row: fields stack full-width on mobile and line up at a fixed width
 * from `sm`, all bottom-aligned so labels, inputs, selects and buttons share one baseline.
 */
export function FilterBar({ onSubmit, label = "Filtros", className, children }: {
  onSubmit?: () => void
  label?: string
  className?: string
  children: ReactNode
}) {
  return (
    <form
      role="search"
      aria-label={label}
      className={cn("flex flex-wrap items-end gap-3", className)}
      onSubmit={(event: FormEvent) => { event.preventDefault(); onSubmit?.() }}
    >
      {children}
    </form>
  )
}

export function FilterField({ label, htmlFor, className, children }: {
  label: string
  htmlFor: string
  className?: string
  children: ReactNode
}) {
  return (
    <div className={cn("grid w-full gap-1.5 sm:w-48", className)}>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
    </div>
  )
}

export function FilterActions({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn("flex w-full flex-wrap items-center gap-2 sm:w-auto", className)}>{children}</div>
}
