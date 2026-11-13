import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useIpLocation } from './use-ip-city'

let queryClient: QueryClient

function renderLocation() {
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return renderHook(() => useIpLocation(), {
    wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
  })
}

afterEach(() => {
  queryClient?.clear()
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

describe('useIpLocation', () => {
  it('resuelve ciudad, país, coordenadas y zona horaria desde Cloudflare', async () => {
    vi.stubEnv('DEV', false)
    vi.stubEnv('VITE_CLIENT_URL', 'https://portal.example.test')
    const fetch = vi.fn().mockResolvedValue(new Response('', {
      headers: {
        'acf-ipcity': 'La Plata',
        'acf-ipcontinent': 'SA',
        'acf-ipcountry': 'AR',
        'acf-iplatitude': '-34.9215',
        'acf-iplongitude': '-57.9545',
        'acf-region': 'Buenos Aires',
        'acf-timezone': 'America/Argentina/Buenos_Aires',
      },
    }))
    vi.stubGlobal('fetch', fetch)
    const { result } = renderLocation()

    await waitFor(() => expect(result.current.isResolved).toBe(true))
    expect(result.current.city).toBe('La Plata')
    expect(result.current.latitude).toBe(-34.9215)
    expect(result.current.longitude).toBe(-57.9545)
    expect(result.current.timezone).toBe('America/Argentina/Buenos_Aires')
    expect(fetch).toHaveBeenCalledWith(expect.stringMatching(/\/cdn-cgi\/trace$/))
  })

  it('usa valores geográficos de respaldo ante coordenadas ausentes o inválidas', async () => {
    vi.stubEnv('DEV', false)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', {
      headers: { 'acf-ipcity': 'Rosario', 'acf-iplatitude': 'n/a', 'acf-iplongitude': 'NaN' },
    })))
    const { result } = renderLocation()

    await waitFor(() => expect(result.current.isResolved).toBe(true))
    expect(result.current.city).toBe('Rosario')
    expect(result.current.country).toBe('AR')
    expect(result.current.latitude).toBe(-34.6037)
    expect(result.current.longitude).toBe(-58.3816)
  })

  it.each(['http-error', 'network-error'] as const)('responde con Buenos Aires cuando falla la consulta (%s)', async (failure) => {
    vi.stubEnv('DEV', false)
    vi.stubEnv('VITE_CLIENT_URL', 'https://portal.example.test')
    vi.stubGlobal('fetch', failure === 'http-error'
      ? vi.fn().mockResolvedValue(new Response('', { status: 503 }))
      : vi.fn().mockRejectedValue(new Error('red no disponible')))
    const { result } = renderLocation()

    await waitFor(() => expect(result.current.isResolved).toBe(true))
    expect(result.current.city).toBe('Buenos Aires')
    expect(result.current.country).toBe('AR')
    expect(result.current.isResolved).toBe(true)
  })

  it('usa la ubicación de desarrollo de forma inmediata sin consultar la red', () => {
    vi.stubEnv('DEV', true)
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)
    const { result } = renderLocation()

    expect(result.current.city).toBe('Buenos Aires')
    expect(result.current.isResolved).toBe(true)
    expect(fetch).not.toHaveBeenCalled()
  })
})
