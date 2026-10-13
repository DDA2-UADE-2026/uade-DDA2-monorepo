import { sessionCsrf } from '@/generated/sdk.gen'
import type { Client } from '@/generated/client/types.gen'
import type { SessionCsrfResponse } from '@/generated/types.gen'

/** Install once on the app client; shared by JSON, multipart and download calls. */
export function registerCookieAuthTransport(client: Client): () => void {
  let pendingCsrf: Promise<SessionCsrfResponse> | undefined

  const interceptor = client.interceptors.request.use(async (request) => {
    if (['GET', 'HEAD', 'OPTIONS'].includes(request.method)) return request

    // Read the current token before each write: another tab, login or logout may
    // have rotated its cookie. Coalesce simultaneous reads without persisting it.
    pendingCsrf ??= sessionCsrf({ client, throwOnError: true }).then(({ data }) => data).finally(() => {
      pendingCsrf = undefined
    })
    const csrf = await pendingCsrf
    if (!csrf.headerName || !csrf.token) throw new Error('No se pudo verificar la sesión. Intentá nuevamente.')
    request.headers.set(csrf.headerName, csrf.token)
    return request
  })

  return () => client.interceptors.request.eject(interceptor)
}
