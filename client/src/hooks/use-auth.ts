import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { loginMutation, meOptions, meQueryKey } from '@/generated/@tanstack/react-query.gen'
import { client } from '@/generated/client.gen'
import { clearStoredAuthToken, getStoredAuthToken, setStoredAuthToken } from '@/lib/auth-token'
import { clearCurrentUserAvatar, saveCurrentUserAvatar } from '@/lib/avatar'

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    ...loginMutation(),
    onSuccess: (data) => {
      if (data.token) {
        setStoredAuthToken(data.token)
        client.setConfig({ headers: { Authorization: `Bearer ${data.token}` } })
      }
      if (data.user) saveCurrentUserAvatar(data.user)
      queryClient.invalidateQueries({ queryKey: meQueryKey() })
    },
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
    clearCurrentUserAvatar()
    client.setConfig({ headers: { Authorization: null } })
    queryClient.removeQueries({ queryKey: meQueryKey() })
  }
}
