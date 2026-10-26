import { Badge } from "@/components/ui/badge"
import { severityBadgeClass, severityDotClass, type Severity } from "@/lib/social-management"
import { cn } from "@/lib/utils"

export function SeverityBadge({ severity, children, className }: { severity: Severity; children: React.ReactNode; className?: string }) {
  return (
    <Badge variant="outline" className={cn(severityBadgeClass[severity], className)}>
      {children}
    </Badge>
  )
}

export function SeverityDot({ severity, className }: { severity: Severity; className?: string }) {
  return <span aria-hidden="true" className={cn("size-2 shrink-0 rounded-full", severityDotClass[severity], className)} />
}
