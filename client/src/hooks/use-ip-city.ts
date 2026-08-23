import { useSyncExternalStore } from 'react';

const CLIENT_URL = import.meta.env.VITE_CLIENT_URL || 'http://localhost:8080';

let ipCity: string | null = null;
const listeners = new Set<() => void>();

function setIpCity(value: string | null) {
  ipCity = value;
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getSnapshot() {
  return ipCity;
}

fetch(`${CLIENT_URL}/cdn-cgi/trace`)
  .then((response) => setIpCity(response.headers.get('acf-ipcity')))
  .catch(() => setIpCity(null));

export function useIpCity() {
  return useSyncExternalStore(subscribe, getSnapshot);
}
