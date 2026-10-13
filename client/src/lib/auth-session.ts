import type { QueryClient } from '@tanstack/react-query'

import { sessionRoleSelectionOptions } from '@/generated/@tanstack/react-query.gen'
import type { UserResponse } from '@/generated/types.gen'

export type PendingRoleSelection = { user: UserResponse }

export function isUnauthenticated(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'status' in error && error.status === 401
}

// Remove credentials left by the previous Bearer implementation. New sessions
// and pending role selections live exclusively in HttpOnly server cookies.
export function clearLegacyAuthStorage(): void {
  try { localStorage.removeItem('auth-token') } catch { /* Storage may be disabled. */ }
  try { sessionStorage.removeItem('role-selection') } catch { /* Storage may be disabled. */ }
}

export async function getPendingRoleSelection(queryClient: QueryClient): Promise<PendingRoleSelection | null> {
  try {
    const pending = await queryClient.fetchQuery({ ...sessionRoleSelectionOptions(), retry: false, staleTime: 0 })
    return pending.requiresRoleSelection && pending.user ? { user: pending.user } : null
  } catch (error) {
    if (isUnauthenticated(error)) return null
    throw error
  }
}
