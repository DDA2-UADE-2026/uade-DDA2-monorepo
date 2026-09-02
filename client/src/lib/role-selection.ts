import type { LoginResponse, UserResponse } from "@/generated/types.gen"

const ROLE_SELECTION_STORAGE_KEY = "role-selection"
const DEFAULT_SELECTION_TTL_SECONDS = 300

export type PendingRoleSelection = {
  selectionToken: string
  expiresAt: number
  user: UserResponse
}

export function storePendingRoleSelection(
  response: LoginResponse,
): PendingRoleSelection | null {
  const selectionToken = response.selectionToken
  const user = response.user
  const roles = user?.roles?.filter(Boolean) ?? []

  if (!response.requiresRoleSelection || !selectionToken || !user || roles.length < 2) {
    clearPendingRoleSelection()
    return null
  }

  const ttlSeconds = response.selectionExpiresIn ?? DEFAULT_SELECTION_TTL_SECONDS
  const pending = {
    selectionToken,
    expiresAt: Date.now() + Math.max(0, ttlSeconds) * 1000,
    user: { ...user, roles },
  }

  if (typeof window !== "undefined") {
    window.sessionStorage.setItem(ROLE_SELECTION_STORAGE_KEY, JSON.stringify(pending))
  }

  return pending
}

export function getPendingRoleSelection(): PendingRoleSelection | null {
  if (typeof window === "undefined") return null

  const stored = window.sessionStorage.getItem(ROLE_SELECTION_STORAGE_KEY)
  if (!stored) return null

  try {
    const pending = JSON.parse(stored) as Partial<PendingRoleSelection>
    const roles = pending.user?.roles?.filter(Boolean) ?? []
    const valid = typeof pending.selectionToken === "string" &&
      pending.selectionToken.length > 0 &&
      typeof pending.expiresAt === "number" &&
      pending.expiresAt > Date.now() &&
      Boolean(pending.user) &&
      roles.length >= 2

    if (!valid) {
      clearPendingRoleSelection()
      return null
    }

    return {
      selectionToken: pending.selectionToken!,
      expiresAt: pending.expiresAt!,
      user: { ...pending.user!, roles },
    }
  } catch {
    clearPendingRoleSelection()
    return null
  }
}

export function clearPendingRoleSelection(): void {
  if (typeof window !== "undefined") {
    window.sessionStorage.removeItem(ROLE_SELECTION_STORAGE_KEY)
  }
}
