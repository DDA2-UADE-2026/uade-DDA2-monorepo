import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import {
  sessionLoginMutation,
  sessionLogoutMutation,
  meOptions,
  meQueryKey,
  sessionSelectRoleMutation,
  sessionSwitchRoleMutation,
} from '@/generated/@tanstack/react-query.gen'
import type { CookieSessionResponse } from '@/generated/types.gen'
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
