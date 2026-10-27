import { QueryClient, QueryClientProvider, useInfiniteQuery } from "@tanstack/react-query"
import { act, renderHook, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { afterEach, describe, expect, it, vi } from "vitest"

import { adminCenterOptions } from "@/components/turnos/adminCenterOptions"
import { client } from "@/generated/client.gen"

const originalConfig = client.getConfig()

afterEach(() => {
  client.setConfig(originalConfig)
  vi.restoreAllMocks()
})

describe("selector de centros para turnos", () => {
  it("puede alcanzar centros después de la página 100 sin exceder el máximo del backend", async () => {
    const requestedPages: number[] = []
    client.setConfig({
      baseUrl: "http://localhost",
      fetch: vi.fn<typeof fetch>(async (input) => {
        const url = new URL((input as Request).url)
        const page = Number(url.searchParams.get("page"))
        requestedPages.push(page)
        expect(Number(url.searchParams.get("size"))).toBeLessThanOrEqual(100)
        expect(url.searchParams.get("active")).toBe("true")
        return Response.json({
          page,
          totalPages: 3,
          content: page === 2
            ? [{ id: "centro-101", name: "Centro 101" }]
            : Array.from({ length: 50 }, (_, index) => ({
              id: `centro-${page * 50 + index + 1}`,
              name: `Centro ${page * 50 + index + 1}`,
            })),
        })
      }),
    })
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )

    const { result, unmount } = renderHook(() => useInfiniteQuery(adminCenterOptions("")), { wrapper })
    await waitFor(() => expect(result.current.hasNextPage).toBe(true))
    await act(async () => { await result.current.fetchNextPage() })
    await act(async () => { await result.current.fetchNextPage() })
    await waitFor(() => expect(result.current.data?.pages).toHaveLength(3))

    expect(requestedPages).toEqual([0, 1, 2])
    const centers = result.current.data?.pages.flatMap((page) => page.content ?? [])
    expect(centers).toHaveLength(101)
    expect(centers?.at(-1)?.name).toBe("Centro 101")
    expect(result.current.hasNextPage).toBe(false)
    unmount()
    queryClient.clear()
  })
})
