export function formatProgramDate(value?: string): string {
  if (!value) return "A confirmar"

  const [year, month, day] = value.split("-").map(Number)
  return new Intl.DateTimeFormat("es-AR", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(year, month - 1, day))
}
