import type { CreateClientConfig } from '@/generated/client.gen'

export const createClientConfig: CreateClientConfig = (config) => ({
  ...config,
  baseUrl: import.meta.env.VITE_SERVER_URL || 'http://localhost:8080',
})
