import { toast } from "sonner"

import { client } from "@/generated/client.gen"
import { router, queryClient } from "@/router"
import { meQueryKey } from "@/generated/@tanstack/react-query.gen"
import { clearLegacyAuthStorage } from "@/lib/auth-session"
import { clearCurrentUserAvatar } from "@/lib/avatar"

export function registerAuthInterceptor(): void {
  client.interceptors.response.use((response, request) => {
    const path = new URL(request.url).pathname
    const isSessionEntry = path.includes("/auth/session/") && !path.endsWith("/switch-role")
    if (response.status === 401 && !isSessionEntry && queryClient.getQueryData(meQueryKey())) {
      clearLegacyAuthStorage()
      clearCurrentUserAvatar()
      // The login route rechecks /auth/me and clears the cache. Clearing it here
      // would cancel the request whose 401 the route guard still needs to handle.
      toast.error("Tu sesión expiró", {
        id: "session-expired",
        description: "Iniciá sesión nuevamente para continuar.",
        action: {
          label: "Ir a iniciar sesión",
          onClick: () => router.navigate({ to: "/login", replace: true }),
        },
      })
      void router.navigate({ to: "/login", replace: true })
    }

    return response
  })
}
