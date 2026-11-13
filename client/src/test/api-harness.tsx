import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactNode } from 'react'
import { vi } from 'vitest'
import { client } from '@/generated/client.gen'

export function apiHarness(handler: (request: Request) => Response | Promise<Response>) {
  const previous = client.getConfig()
  const requests: Request[] = []
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  client.setConfig({ baseUrl: 'http://localhost', fetch: vi.fn<typeof fetch>(async input => {
    const request = input as Request
    requests.push(request.clone())
    return handler(request)
  }) })
  return {
    requests, queryClient,
    render: (ui: ReactNode) => render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>),
    restore: () => { queryClient.clear(); client.setConfig(previous) },
  }
}
