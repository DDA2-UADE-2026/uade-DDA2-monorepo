import { Badge } from "@/components/ui/badge"

export function AttendanceBadge({ attendance }: { attendance?: "PRESENT" | "ABSENT" | null }) {
  return <Badge variant={attendance === "PRESENT" ? "default" : attendance === "ABSENT" ? "secondary" : "outline"}>
    {attendance === "PRESENT" ? "Presente" : attendance === "ABSENT" ? "Ausente" : "Sin registrar"}
  </Badge>
}
