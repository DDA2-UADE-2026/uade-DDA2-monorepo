import { useQuery } from '@tanstack/react-query';

import { healthOptions } from '@/generated/@tanstack/react-query.gen';

const REFETCH_INTERVAL_MS = 2.5 * 60 * 1000;

export function useBackendHealth() {
  return useQuery({
    ...healthOptions(),
    refetchInterval: REFETCH_INTERVAL_MS,
    retry: 3,
  });
}
