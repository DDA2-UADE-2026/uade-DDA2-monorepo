import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { sessionM1Login } from '@/generated/sdk.gen'
import {
  sessionLoginMutation,
  sessionLogoutMutation,
  meOptions,
  meQueryKey,
  sessionSelectRoleMutation,
  sessionSwitchRoleMutation,
} from '@/generated/@tanstack/react-query.gen'
import type {
  CookieSessionResponse,
  M1SessionLoginRequestWritable,
  SessionM1LoginError,
  SessionM1LoginResponse,
} from '@/generated/types.gen'
import { clearLegacyAuthStorage } from '@/lib/auth-session'
import { clearCurrentUserAvatar, saveCurrentUserAvatar } from '@/lib/avatar'

async function storeSession(
  data: CookieSessionResponse,
  queryClient: ReturnType<typeof useQueryClient>,
): Promise<void> {
  // Cancel requests from the previous role before clearing its private data.
  await queryClient.cancelQueries()
  queryClient.removeQueries()
  clearLegacyAuthStorage()
  clearCurrentUserAvatar()
  if (data.user?.activeRole) queryClient.setQueryData(meQueryKey(), { user: data.user })
  if (data.user) saveCurrentUserAvatar(data.user)
}

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    ...sessionLoginMutation(),
    onSuccess: (data) => storeSession(data, queryClient),
  })
}

/** M1 runs on a host that sleeps when idle: the first request wakes it and may time out. */
const M1_WAKING_CODES = new Set(['M1_AUTH_TIMEOUT', 'M1_AUTH_UNAVAILABLE', 'M1_AUTH_UPSTREAM_ERROR'])
export const M1_LOGIN_MAX_ATTEMPTS = 3

export function isM1WakingError(error: unknown): boolean {
  const code = (error as SessionM1LoginError | null)?.code
  return typeof code === 'string' && M1_WAKING_CODES.has(code)
}

export function useM1Login() {
  const queryClient = useQueryClient()

  return useMutation<SessionM1LoginResponse, SessionM1LoginError, { body: M1SessionLoginRequestWritable }>({
    mutationFn: async ({ body }) => {
      // The spec marks the CSRF header as required for this operation only.
      // registerCookieAuthTransport replaces this placeholder on every write.
      const { data } = await sessionM1Login({ body, headers: { 'X-XSRF-TOKEN': '' }, throwOnError: true })
      return data
    },
    // Only availability failures are retried; wrong credentials never are.
    // Each attempt counts toward the server's login rate limit (10 per 15 min).
    retry: (failureCount, error) => failureCount < M1_LOGIN_MAX_ATTEMPTS - 1 && isM1WakingError(error),
    retryDelay: 2000,
    onSuccess: (data) => storeSession(data, queryClient),
  })
}

export function useSelectRole() {
  const queryClient = useQueryClient()

  return useMutation({
    ...sessionSelectRoleMutation(),
    onSuccess: (data) => storeSession(data, queryClient),
  })
}

export function useSwitchRole() {
  const queryClient = useQueryClient()

  return useMutation({
    ...sessionSwitchRoleMutation(),
    onSuccess: (data) => storeSession(data, queryClient),
  })
}

export function useMe() {
  return useQuery({
    ...meOptions(),
    retry: false,
  })
}

export function useLogout() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    ...sessionLogoutMutation(),
    onSuccess: async () => {
      await queryClient.cancelQueries()
      queryClient.clear()
      clearLegacyAuthStorage()
      clearCurrentUserAvatar()
    },
    onError: () => toast.error('No pudimos cerrar la sesión. Verificá la conexión e intentá nuevamente.'),
  })
  return {
    isPending: mutation.isPending,
    logout: () => mutation.mutateAsync({}),
  }
}
