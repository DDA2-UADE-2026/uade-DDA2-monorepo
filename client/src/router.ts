import { createRouter } from '@tanstack/react-router'
import { QueryClient } from '@tanstack/react-query'

import { routeTree } from './routeTree.gen'

export const queryClient = new QueryClient()

export const router = createRouter({
  routeTree,
  context: { queryClient },
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
