import { queryOptions, useQuery } from '@tanstack/react-query';

const CLIENT_URL = import.meta.env.VITE_CLIENT_URL;
const REFETCH_INTERVAL_MS = 60 * 60 * 1000;

export interface IpLocation {
  city: string;
  continent: string;
  country: string;
  latitude: number;
  longitude: number;
  region: string;
  timezone: string;
}

const FALLBACK_IP_LOCATION: IpLocation = {
  city: 'Buenos Aires',
  continent: 'SA',
  country: 'AR',
  latitude: -34.6037,
  longitude: -58.3816,
  region: 'Buenos Aires',
  timezone: 'America/Argentina/Buenos_Aires',
};

async function fetchIpLocation(): Promise<IpLocation> {
  try {
    const response = await fetch(`${CLIENT_URL}/cdn-cgi/trace`);
    if (!response.ok) return FALLBACK_IP_LOCATION;

    const headers = response.headers;
    const latitude = Number(headers.get('acf-iplatitude'));
    const longitude = Number(headers.get('acf-iplongitude'));

    return {
      city: headers.get('acf-ipcity') ?? FALLBACK_IP_LOCATION.city,
      continent: headers.get('acf-ipcontinent') ?? FALLBACK_IP_LOCATION.continent,
      country: headers.get('acf-ipcountry') ?? FALLBACK_IP_LOCATION.country,
      latitude: Number.isFinite(latitude) ? latitude : FALLBACK_IP_LOCATION.latitude,
      longitude: Number.isFinite(longitude) ? longitude : FALLBACK_IP_LOCATION.longitude,
      region: headers.get('acf-region') ?? FALLBACK_IP_LOCATION.region,
      timezone: headers.get('acf-timezone') ?? FALLBACK_IP_LOCATION.timezone,
    };
  } catch {
    return FALLBACK_IP_LOCATION;
  }
}

const ipLocationQueryOptions = queryOptions({
  queryKey: ['ip-location'],
  queryFn: fetchIpLocation,
  staleTime: REFETCH_INTERVAL_MS,
  refetchInterval: REFETCH_INTERVAL_MS,
  retry: 3,
});

export function useIpLocation() {
  const { data } = useQuery(ipLocationQueryOptions);

  return data ?? FALLBACK_IP_LOCATION;
}
