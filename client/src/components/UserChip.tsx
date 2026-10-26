import type { ReactNode } from "react"

import { UserAvatar } from "@/components/UserAvatar"
import type { AvatarUser } from "@/lib/avatar"
import { cn } from "@/lib/utils"

// Avatar + name for any place a person is referenced. Pass the user id so the generated
// avatar matches the one shown for the same person elsewhere.
export function UserChip({ user, secondary, fallback = "—", className }: {
  user: AvatarUser
  secondary?: ReactNode
  fallback?: string
  className?: string
}) {
  if (!user.name && !user.email && !user.username) return <span className={cn("text-muted-foreground", className)}>{fallback}</span>

  return (
    <span className={cn("flex min-w-0 items-center gap-2.5", className)}>
      <UserAvatar user={user} size="sm" />
      <span className="min-w-0">
        <span className="block truncate font-medium">{user.name || user.username || user.email}</span>
        {secondary && <span className="block truncate text-xs text-muted-foreground">{secondary}</span>}
      </span>
    </span>
  )
}

// Smaller variant for running text and compact rows ("Profesional: <avatar> Nombre").
export function UserInline({ user, fallback = "—", className }: { user: AvatarUser; fallback?: string; className?: string }) {
  const label = user.name || user.username || user.email
  if (!label) return <span className={className}>{fallback}</span>

  return (
    <span className={cn("inline-flex max-w-full items-center gap-1.5 align-middle", className)}>
      <UserAvatar user={user} className="size-5 shrink-0" fallbackClassName="text-[9px]" />
      <span className="truncate">{label}</span>
    </span>
  )
}
