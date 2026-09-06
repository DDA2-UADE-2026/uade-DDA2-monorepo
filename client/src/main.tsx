import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from '@tanstack/react-router'
import { QueryClientProvider } from '@tanstack/react-query'
import { z } from 'zod'
import { es } from 'zod/locales'
import { ThemeProvider } from '@/providers/theme-provider'
import { TooltipProvider } from "@/components/ui/tooltip"
import { registerAuthInterceptor } from '@/lib/auth-interceptor'
import { queryClient, router } from './router'
import './index.css'

registerAuthInterceptor()

z.config({
  ...es(),
  customError: (issue) =>
    issue.input === undefined || issue.input === ""
      ? "Este campo es obligatorio."
      : undefined,
})

console.log("Current environment: ", import.meta.env.MODE)
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
