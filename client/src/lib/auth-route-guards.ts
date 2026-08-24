import type { QueryClient } from '@tanstack/react-query'
import { redirect } from '@tanstack/react-router'

import { meOptions, meQueryKey } from '@/generated/@tanstack/react-query.gen'
import { client } from '@/generated/client.gen'
import type { UserResponse } from '@/generated/types.gen'
import { clearStoredAuthToken, getStoredAuthToken } from '@/lib/auth-token'

function clearInvalidSession(queryClient: QueryClient): void {
  clearStoredAuthToken()
  client.setConfig({ headers: { Authorization: null } })
  queryClient.removeQueries({ queryKey: meQueryKey() })
}

async function getAuthenticatedUser(queryClient: QueryClient): Promise<UserResponse | null> {
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

  if (user) {
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
