import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  loginMutation,
  meOptions,
  meQueryKey,
  selectRoleMutation,
  switchRoleMutation,
} from '@/generated/@tanstack/react-query.gen'
import { client } from '@/generated/client.gen'
import type { LoginResponse } from '@/generated/types.gen'
import { clearStoredAuthToken, getStoredAuthToken, setStoredAuthToken } from '@/lib/auth-token'
import { clearCurrentUserAvatar, saveCurrentUserAvatar } from '@/lib/avatar'
import { clearPendingRoleSelection, storePendingRoleSelection } from '@/lib/role-selection'

function storeOperationalSession(
  data: LoginResponse,
  queryClient: ReturnType<typeof useQueryClient>,
  clearRoleScopedQueries = false,
): void {
  if (!data.token || !data.user?.activeRole) return

  if (clearRoleScopedQueries) queryClient.removeQueries()
  clearPendingRoleSelection()
  setStoredAuthToken(data.token)
  client.setConfig({ headers: { Authorization: `Bearer ${data.token}` } })
  queryClient.setQueryData(meQueryKey(), { user: data.user })
  saveCurrentUserAvatar(data.user)
}

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    ...loginMutation(),
    onSuccess: (data) => {
      clearStoredAuthToken()
      clearPendingRoleSelection()
      client.setConfig({ headers: { Authorization: null } })
      queryClient.removeQueries()

      if (data.token) storeOperationalSession(data, queryClient)
      else storePendingRoleSelection(data)

      if (data.user) saveCurrentUserAvatar(data.user)
    },
  })
}

export function useSelectRole() {
  const queryClient = useQueryClient()

  return useMutation({
    ...selectRoleMutation({ headers: { Authorization: null } }),
    onSuccess: (data) => storeOperationalSession(data, queryClient, true),
  })
}

export function useSwitchRole() {
  const queryClient = useQueryClient()

  return useMutation({
    ...switchRoleMutation(),
    onSuccess: (data) => storeOperationalSession(data, queryClient, true),
  })
}

export function useMe() {
  return useQuery({
    ...meOptions(),
    enabled: !!getStoredAuthToken(),
    retry: false,
  })
}

export function useLogout() {
  const queryClient = useQueryClient()

  return () => {
    clearStoredAuthToken()
    clearPendingRoleSelection()
    clearCurrentUserAvatar()
    client.setConfig({ headers: { Authorization: null } })
    queryClient.clear()
  }
}
