import { toast } from "sonner"

import { client } from "@/generated/client.gen"
import { router } from "@/router"

export function registerAuthInterceptor(): void {
  client.interceptors.response.use((response, request) => {
    if (response.status === 401 && request.headers.get("Authorization")) {
      toast.error("Tu sesión expiró", {
        id: "session-expired",
        description: "Iniciá sesión nuevamente para continuar.",
        action: {
          label: "Ir a iniciar sesión",
          onClick: () => router.navigate({ to: "/login", replace: true }),
        },
      })
    }

    return response
  })
}
