import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider, createRouter } from '@tanstack/react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@/providers/theme-provider'
import { TooltipProvider } from "@/components/ui/tooltip"
import './index.css'

const queryClient = new QueryClient()

// Importar el árbol de rutas generado
import { routeTree } from './routeTree.gen'
const router = createRouter({
  routeTree,
  context: { queryClient },
})
// Registrar el router en el módulo '@tanstack/react-router'
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

console.log("Using server URL: ", import.meta.env.VITE_SERVER_URL)
console.log("Using client URL: ", import.meta.env.VITE_CLIENT_URL)

const rootElement = document.getElementById('root')!
if (!rootElement.innerHTML) {
  const root = createRoot(rootElement)
  root.render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
          <TooltipProvider>
            <RouterProvider router={router} />
          </TooltipProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </StrictMode>,
  )
}
