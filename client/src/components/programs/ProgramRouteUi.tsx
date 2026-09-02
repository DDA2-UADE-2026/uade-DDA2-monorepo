/* eslint-disable react-refresh/only-export-components */
import { Link, useRouterState } from "@tanstack/react-router"
import type { ComponentProps, ReactNode } from "react"

import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { cn } from "@/lib/utils"

export function RouteTabs({ items }: { items: { label: string; to: string }[] }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const currentPath = pathname.replace(/\/+$/, "") || "/"
  const activePath = items
    .map((item) => item.to.replace(/\/+$/, "") || "/")
    .filter((itemPath) => currentPath === itemPath || currentPath.startsWith(`${itemPath}/`))
    .sort((left, right) => right.length - left.length)[0]

  return <nav className="flex gap-6 overflow-x-auto border-b px-4 lg:px-6">
    {items.map((item) => {
      const itemPath = item.to.replace(/\/+$/, "") || "/"
      return <Link key={item.to} to={item.to} className={cn("shrink-0 whitespace-nowrap border-b-2 border-transparent px-1 py-3 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground", itemPath === activePath && "border-primary text-foreground")}>{item.label}</Link>
    })}
  </nav>
}

export function RoutePanel({ children }: { children: ReactNode }) {
  return <div className="mx-auto w-full max-w-4xl p-4 lg:p-6">{children}</div>
}

export function LoadingOrError({ pending, error, retry }: { pending: boolean; error: boolean; retry?: () => void }) {
  if (pending) return <p className="text-sm text-muted-foreground">Cargando…</p>
  if (error) return <div className="space-y-2"><p className="text-sm text-destructive">No se pudieron cargar los datos.</p>{retry && <button className="text-sm underline" onClick={retry}>Reintentar</button>}</div>
  return null
}

export const inputClass = "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30"
export const textareaClass = "flex min-h-28 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30"

export function FormField({
  label,
  children,
  htmlFor,
  invalid = false,
  errors,
}: {
  label: string
  children: ReactNode
  htmlFor?: string
  invalid?: boolean
  errors?: ComponentProps<typeof FieldError>["errors"]
}) {
  return <Field className="gap-1.5" data-invalid={invalid}>
    <FieldLabel htmlFor={htmlFor}>{label}</FieldLabel>
    {children}
    {invalid && <FieldError errors={errors} />}
  </Field>
}

export const statusLabels = { DRAFT: "Borrador", ACTIVE: "Activa", SUSPENDED: "Suspendida", CLOSED: "Cerrada" } as const
export const requirementLabels = { MIN_AGE: "Edad mínima", MAX_INCOME: "Ingreso máximo", RESIDENCY_YEARS: "Años de residencia", HAS_CHILDREN: "Tiene hijos" } as const
export const benefitLabels = { TAX_EXEMPTION: "Exención impositiva", HOUSING_SUBSIDY: "Subsidio habitacional", FOOD_ASSISTANCE: "Asistencia alimentaria", UTILITY_SUBSIDY: "Subsidio de servicios" } as const

export function parseLocalDate(value: string) {
  if (!value) return undefined
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, month - 1, day)
}

export function formatLocalDate(value?: Date) {
  if (!value) return ""
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, "0")
  const day = String(value.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}
