import { infiniteQueryOptions } from "@tanstack/react-query"

import { listMunicipalCenters } from "@/generated/sdk.gen"

const CENTER_PAGE_SIZE = 50

export function adminCenterOptions(search: string) {
  return infiniteQueryOptions({
    queryKey: ["turnos-centros", search],
    initialPageParam: 0,
    queryFn: async ({ pageParam }) => {
      const { data } = await listMunicipalCenters({
        query: { active: true, page: pageParam, size: CENTER_PAGE_SIZE, ...(search ? { search } : {}) },
        throwOnError: true,
      })
      return data
    },
    getNextPageParam: (lastPage) => {
      const next = (lastPage.page ?? 0) + 1
      return next < (lastPage.totalPages ?? 0) ? next : undefined
    },
  })
}
