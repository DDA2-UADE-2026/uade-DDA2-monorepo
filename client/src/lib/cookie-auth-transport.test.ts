import { describe, expect, it, vi } from 'vitest'
import { createClient } from '@/generated/client'
import { registerCookieAuthTransport } from './cookie-auth-transport'

describe('transporte cookie y CSRF', () => {
  it('no pide token para lecturas y no agrega Authorization Bearer', async () => {
    const requests: Request[] = []
    const client = createClient({ baseUrl: 'http://localhost', credentials: 'include', fetch: vi.fn<typeof fetch>(async input => {
      requests.push(input as Request)
      return new Response(null, { status: 204 })
    }) })
    const dispose = registerCookieAuthTransport(client)
    await client.get({ url: '/read' })
    await client.head({ url: '/head' })
    await client.options({ url: '/options' })
    expect(requests.map(r => r.method)).toEqual(['GET', 'HEAD', 'OPTIONS'])
    expect(requests.every(r => !r.headers.has('Authorization') && r.credentials === 'include')).toBe(true)
    dispose()
  })

  it('comparte la lectura CSRF concurrente y obtiene un token nuevo en la siguiente escritura', async () => {
    let csrfReads = 0
    const writes: Request[] = []
    const client = createClient({ baseUrl: 'http://localhost', fetch: vi.fn<typeof fetch>(async input => {
      const r = input as Request
      if (new URL(r.url).pathname === '/auth/session/csrf') return Response.json({ headerName: 'X-CSRF', token: `token-${++csrfReads}` })
      writes.push(r)
      return Response.json({ saved: true })
    }) })
    const dispose = registerCookieAuthTransport(client)
    await Promise.all([client.post({ url: '/one' }), client.patch({ url: '/two' }), client.delete({ url: '/three' })])
    expect(csrfReads).toBe(1)
    expect(writes.map(r => r.headers.get('X-CSRF'))).toEqual(['token-1', 'token-1', 'token-1'])
    await client.put({ url: '/next' })
    expect(csrfReads).toBe(2)
    expect(writes[3].headers.get('X-CSRF')).toBe('token-2')
    dispose()
    await client.post({ url: '/after-dispose' })
    expect(csrfReads).toBe(2)
    expect(writes[4].headers.has('X-CSRF')).toBe(false)
  })

  it.each([{ headerName: 'X-CSRF' }, { token: 'value' }, {}])('rechaza un token incompleto %j antes de enviar datos', async token => {
    const fetch = vi.fn<typeof globalThis.fetch>(async () => Response.json(token))
    const client = createClient({ baseUrl: 'http://localhost', fetch })
    const dispose = registerCookieAuthTransport(client)
    await expect(client.post({ url: '/protected', throwOnError: true })).rejects.toThrow('No se pudo verificar la sesión')
    expect(fetch).toHaveBeenCalledOnce()
    dispose()
  })

  it('propaga el error CSRF y permite recuperar la sesión sin conservar la promesa fallida', async () => {
    let failed = true
    const writes: Request[] = []
    const client = createClient({ baseUrl: 'http://localhost', fetch: vi.fn<typeof fetch>(async input => {
      const r = input as Request
      if (new URL(r.url).pathname === '/auth/session/csrf') return failed
        ? Response.json({ message: 'Sesión no disponible' }, { status: 503 })
        : Response.json({ headerName: 'X-CSRF', token: 'restored' })
      writes.push(r)
      return Response.json({ saved: true })
    }) })
    const dispose = registerCookieAuthTransport(client)
    await expect(client.post({ url: '/protected', throwOnError: true })).rejects.toMatchObject({ message: 'Sesión no disponible' })
    expect(writes).toHaveLength(0)
    failed = false
    await client.post({ url: '/protected', throwOnError: true })
    expect(writes).toHaveLength(1)
    expect(writes[0].headers.get('X-CSRF')).toBe('restored')
    dispose()
  })
})
