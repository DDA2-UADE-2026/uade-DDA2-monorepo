export const DAYS = [
  { value: "MONDAY", label: "Lunes" },
  { value: "TUESDAY", label: "Martes" },
  { value: "WEDNESDAY", label: "Miércoles" },
  { value: "THURSDAY", label: "Jueves" },
  { value: "FRIDAY", label: "Viernes" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
] as const

export type DayOfWeekValue = (typeof DAYS)[number]["value"]

export const dayLabel = (day?: string): string =>
  DAYS.find((entry) => entry.value === day)?.label ?? day ?? "—"

export const shortTime = (time?: string): string => time?.slice(0, 5) ?? "—"

export interface TimeRangeValue {
  dayOfWeek: DayOfWeekValue
  startTime: string
  endTime: string
}

export function dayIndex(day?: string): number {
  const index = DAYS.findIndex((entry) => entry.value === day)
  return index === -1 ? DAYS.length : index
}
