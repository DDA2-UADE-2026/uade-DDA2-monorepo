import { z } from "zod"

export const PAGE_SIZE = 10

export const centerSearchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
  search: z.string().catch("").default(""),
  estado: z.enum(["todos", "activos", "inactivos"]).catch("todos").default("todos"),
})

export type CenterSearch = z.infer<typeof centerSearchSchema>

export interface CenterListQuery {
  page: number
  size: number
  search?: string
  active?: boolean
}

export function toCenterListQuery(search: CenterSearch): CenterListQuery {
  const trimmed = search.search.trim()
  return {
    page: Math.max(0, search.page - 1),
    size: PAGE_SIZE,
    ...(trimmed ? { search: trimmed } : {}),
    ...(search.estado === "activos" ? { active: true } : {}),
    ...(search.estado === "inactivos" ? { active: false } : {}),
  }
}
