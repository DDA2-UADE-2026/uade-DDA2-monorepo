import type { QueryClient } from '@tanstack/react-query'
import { redirect } from '@tanstack/react-router'

import { meOptions } from '@/generated/@tanstack/react-query.gen'
import type { UserResponse } from '@/generated/types.gen'
import { clearLegacyAuthStorage, getPendingRoleSelection, isUnauthenticated, type PendingRoleSelection } from '@/lib/auth-session'
import { clearCurrentUserAvatar } from '@/lib/avatar'

export type RoleHome = '/portal' | '/gestion'
export type RoleChoice =
  | { mode: 'select'; pending: PendingRoleSelection }
  | { mode: 'switch'; user: UserResponse }

export function getRoleHome(activeRole: string): RoleHome {
  return activeRole.trim().toUpperCase() === 'CIUDADANO' ? '/portal' : '/gestion'
}

function clearInvalidSession(queryClient: QueryClient): void {
  clearLegacyAuthStorage()
  clearCurrentUserAvatar()
  queryClient.clear()
}

export async function getAuthenticatedUser(queryClient: QueryClient): Promise<UserResponse | null> {
  try {
    const session = await queryClient.fetchQuery({
      ...meOptions(),
      retry: false,
    })

    if (session.user) return session.user
  } catch (error) {
    // Connectivity and server errors must not turn into a false logout.
    if (!isUnauthenticated(error)) throw error
  }

  clearInvalidSession(queryClient)
  return null
}

export async function redirectAuthenticatedUser(queryClient: QueryClient): Promise<void> {
  const user = await getAuthenticatedUser(queryClient)

  if (user?.activeRole) {
    throw redirect({ to: getRoleHome(user.activeRole), replace: true })
  }

  if (await getPendingRoleSelection(queryClient)) {
    throw redirect({ to: '/seleccionar-rol', replace: true })
  }
}

export async function requireAuthenticatedUser(
  queryClient: QueryClient,
): Promise<{ user: UserResponse }> {
  const user = await getAuthenticatedUser(queryClient)

  if (!user) {
    throw redirect({ to: '/login', replace: true })
  }

  return { user }
}

export async function requireRoleChoice(queryClient: QueryClient): Promise<RoleChoice> {
  const user = await getAuthenticatedUser(queryClient)
  if (user?.activeRole) {
    if ((user.roles?.length ?? 0) < 2) {
      throw redirect({ to: getRoleHome(user.activeRole), replace: true })
    }

    return { mode: 'switch', user }
  }

  const pending = await getPendingRoleSelection(queryClient)
  if (pending) return { mode: 'select', pending }

  throw redirect({ to: '/login', replace: true })
}
