import type { CreateClientConfig } from '@/generated/client.gen'
import { getStoredAuthToken } from '@/lib/auth-token'

export const createClientConfig: CreateClientConfig = (config) => {
  const token = getStoredAuthToken()

  return {
    ...config,
    baseUrl: import.meta.env.VITE_SERVER_URL,
    headers: token ? { ...config?.headers, Authorization: `Bearer ${token}` } : config?.headers,
  }
}
