import { queryOptions, useQuery } from '@tanstack/react-query';

const CLIENT_URL = import.meta.env.VITE_CLIENT_URL;
const REFETCH_INTERVAL_MS = 60 * 60 * 1000;

async function fetchIpCity(): Promise<string | null> {
  const response = await fetch(`${CLIENT_URL}/cdn-cgi/trace`);
  return response.headers.get('acf-ipcity');
}

const ipCityQueryOptions = queryOptions({
  queryKey: ['ip-city'],
  queryFn: fetchIpCity,
  staleTime: REFETCH_INTERVAL_MS,
  refetchInterval: REFETCH_INTERVAL_MS,
  retry: 3,
});

export function useIpCity() {
  const { data } = useQuery(ipCityQueryOptions);

  return data ?? null;
}
