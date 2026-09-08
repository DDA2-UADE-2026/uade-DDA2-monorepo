import type { QueryClient } from '@tanstack/react-query'
import { redirect } from '@tanstack/react-router'

import { meOptions } from '@/generated/@tanstack/react-query.gen'
import { client } from '@/generated/client.gen'
import type { UserResponse } from '@/generated/types.gen'
import { clearStoredAuthToken, getStoredAuthToken } from '@/lib/auth-token'
import { clearCurrentUserAvatar } from '@/lib/avatar'
import { getPendingRoleSelection, type PendingRoleSelection } from '@/lib/role-selection'

export type RoleHome = '/portal' | '/gestion'
export type RoleChoice =
  | { mode: 'select'; pending: PendingRoleSelection }
  | { mode: 'switch'; user: UserResponse }

export function getRoleHome(activeRole: string): RoleHome {
  return activeRole.trim().toUpperCase() === 'CIUDADANO' ? '/portal' : '/gestion'
}

function clearInvalidSession(queryClient: QueryClient): void {
  clearStoredAuthToken()
  clearCurrentUserAvatar()
  client.setConfig({ headers: { Authorization: null } })
  queryClient.clear()
}

export async function getAuthenticatedUser(queryClient: QueryClient): Promise<UserResponse | null> {
  if (!getStoredAuthToken()) return null

  try {
    const session = await queryClient.fetchQuery({
      ...meOptions(),
      retry: false,
    })

    if (session.user) return session.user
  } catch {
    // Invalid and expired sessions are handled identically below.
  }

  clearInvalidSession(queryClient)
  return null
}

export async function redirectAuthenticatedUser(queryClient: QueryClient): Promise<void> {
  const user = await getAuthenticatedUser(queryClient)

  if (user?.activeRole) {
    throw redirect({ to: getRoleHome(user.activeRole), replace: true })
  }

  if (getPendingRoleSelection()) {
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

  const pending = getPendingRoleSelection()
  if (pending) return { mode: 'select', pending }

  throw redirect({ to: '/login', replace: true })
}
