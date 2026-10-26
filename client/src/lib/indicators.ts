import type { IndicatorsResponse } from "@/generated/types.gen"

export type Indicators = IndicatorsResponse
export type Bucket = NonNullable<IndicatorsResponse["bucket"]>

const integer = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 })
const compact = new Intl.NumberFormat("es-AR", { notation: "compact", maximumFractionDigits: 1 })
const percent = new Intl.NumberFormat("es-AR", { style: "percent", maximumFractionDigits: 0 })
const decimal = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 1 })

/** Entero con separador de miles; desde 10.000 se compacta (12,9 mil). */
export function formatCount(value: number | null | undefined): string {
  if (value == null) return "—"
  return Math.abs(value) >= 10_000 ? compact.format(value) : integer.format(value)
}

export function formatRate(value: number | null | undefined): string {
  return value == null ? "—" : percent.format(value)
}

export function formatDays(value: number | null | undefined): string {
  if (value == null) return "—"
  return `${decimal.format(value)} ${value === 1 ? "día" : "días"}`
}

/** Variación porcentual contra el período anterior; null si no hay base de comparación. */
export function delta(current: number, previous: number): number | null {
  if (previous === 0) return null
  return (current - previous) / previous
}

const shortMonth = new Intl.DateTimeFormat("es-AR", { month: "short", year: "2-digit" })
const shortDay = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "short" })
const longDate = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "long", year: "numeric" })

function parseDate(value: string): Date {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, month - 1, day)
}

/** Etiqueta de un intervalo para el eje: "6 oct." en semanas, "oct. 26" en meses. */
export function formatBucket(value: string, bucket: Bucket): string {
  return (bucket === "WEEK" ? shortDay : shortMonth).format(parseDate(value))
}

/** Etiqueta completa para tooltips y tablas: "Semana del 6 de octubre de 2026" u "octubre de 2026". */
export function formatBucketLong(value: string, bucket: Bucket): string {
  const date = parseDate(value)
  if (bucket === "WEEK") return `Semana del ${longDate.format(date)}`
  return new Intl.DateTimeFormat("es-AR", { month: "long", year: "numeric" }).format(date)
}

export function formatDate(value: string): string {
  return longDate.format(parseDate(value))
}

export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

export type RangePreset = "30d" | "90d" | "12m" | "anio"

export const RANGE_PRESETS: Record<RangePreset, string> = {
  "30d": "Últimos 30 días",
  "90d": "Últimos 90 días",
  "12m": "Últimos 12 meses",
  anio: "Este año",
}

/** Fechas (inclusive) de un rango predefinido, con "hoy" como fin. */
export function presetRange(preset: RangePreset, today = new Date()): { from: string; to: string } {
  const from = new Date(today)
  if (preset === "30d") from.setDate(from.getDate() - 29)
  else if (preset === "90d") from.setDate(from.getDate() - 89)
  else if (preset === "12m") { from.setFullYear(from.getFullYear() - 1); from.setDate(from.getDate() + 1) }
  else { from.setMonth(0, 1) }
  return { from: toIsoDate(from), to: toIsoDate(today) }
}
