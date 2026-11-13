import { act, render, screen } from "@testing-library/react"
import { afterEach, beforeEach, expect, it, vi } from "vitest"

const mapHarness = vi.hoisted(() => ({
  options: null as Record<string, unknown> | null,
  load: null as (() => void) | null,
  flight: [] as Record<string, unknown>[],
  jumps: [] as Record<string, unknown>[],
  locations: [] as number[][],
  bearings: [] as number[],
  mapRemovals: 0,
  markerRemovals: 0,
  offEvents: [] as string[],
}))

vi.mock("maplibre-gl", () => ({
  setWorkerUrl: vi.fn(),
  Map: class {
    constructor(options: Record<string, unknown>) { mapHarness.options = options }
    once(event: string, callback: () => void) { if (event === "load") mapHarness.load = callback; return this }
    off(event: string) { mapHarness.offEvents.push(event); return this }
    flyTo(options: Record<string, unknown>) { mapHarness.flight.push(options); return this }
    jumpTo(options: Record<string, unknown>) { mapHarness.jumps.push(options); return this }
    getBearing() { return 12 }
    setBearing(value: number) { mapHarness.bearings.push(value); return this }
    remove() { mapHarness.mapRemovals++ }
  },
  Marker: class {
    constructor() {}
    setLngLat(coordinates: number[]) { mapHarness.locations.push(coordinates); return this }
    addTo() { return this }
    remove() { mapHarness.markerRemovals++ }
  },
}))

vi.mock("maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url", () => ({ default: "/worker.js" }))

import { ArgentinaLocationMap } from "./ArgentinaLocationMap"

const location = { city: "Córdoba", region: "Córdoba", country: "AR", continent: "SA", timezone: "America/Argentina/Cordoba", latitude: -31.42, longitude: -64.18, isResolved: true }
let originalHidden: PropertyDescriptor | undefined

beforeEach(() => {
  vi.useFakeTimers()
  mapHarness.options = null
  mapHarness.load = null
  mapHarness.flight = []
  mapHarness.jumps = []
  mapHarness.locations = []
  mapHarness.bearings = []
  mapHarness.mapRemovals = 0
  mapHarness.markerRemovals = 0
  mapHarness.offEvents = []
  vi.spyOn(window, "matchMedia").mockReturnValue({ matches: true, media: "(prefers-reduced-motion: reduce)", onchange: null, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn() })
})

afterEach(() => {
  if (originalHidden) Object.defineProperty(document, "hidden", originalHidden)
  else Reflect.deleteProperty(document, "hidden")
  originalHidden = undefined
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

it("crea el mapa al resolver la ubicación, respeta movimiento reducido y limpia recursos", async () => {
  const view = render(<ArgentinaLocationMap location={{ ...location, isResolved: false }} />)
  expect(mapHarness.options).toBeNull()

  view.rerender(<ArgentinaLocationMap location={location} className="map-test" />)
  expect(mapHarness.options).toMatchObject({ style: "/geo/fabrizio-custom-darkmatter-tiles.json", interactive: false, center: [-64, -38], zoom: 3.15 })
  expect(mapHarness.locations).toEqual([[-64.18, -31.42]])
  const locationLabel = screen.getByText("Córdoba, Córdoba").parentElement?.parentElement
  expect(locationLabel).toHaveClass("opacity-0")

  await act(async () => {
    mapHarness.load?.()
    await vi.advanceTimersByTimeAsync(150)
  })
  expect(mapHarness.flight).toHaveLength(1)
  expect(mapHarness.flight[0]).toMatchObject({ center: [-64.18, -31.42], zoom: 14.75, duration: 900, pitch: 0, bearing: 0 })
  await act(async () => { await vi.advanceTimersByTimeAsync(900) })
  expect(mapHarness.jumps).toEqual([{ center: [-64.18, -31.42], zoom: 14.75, bearing: 12, pitch: 0 }])
  expect(locationLabel).toHaveClass("opacity-100")

  view.unmount()
  expect(mapHarness.offEvents).toContain("load")
  expect(mapHarness.markerRemovals).toBe(1)
  expect(mapHarness.mapRemovals).toBe(1)
})

it("inicia la órbita sin movimiento reducido y la pausa/reanuda al ocultar la pestaña", async () => {
  originalHidden = Object.getOwnPropertyDescriptor(document, "hidden")
  const frames: ((time: number) => void)[] = []
  vi.stubGlobal("requestAnimationFrame", (callback: (time: number) => void) => { frames.push(callback); return frames.length })
  vi.stubGlobal("cancelAnimationFrame", vi.fn())
  vi.spyOn(window, "matchMedia").mockReturnValue({ matches: false, media: "(prefers-reduced-motion: reduce)", onchange: null, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn() })
  const view = render(<ArgentinaLocationMap location={location} />)

  await act(async () => {
    mapHarness.load?.()
    await vi.advanceTimersByTimeAsync(1100)
  })
  expect(mapHarness.flight[0]).toMatchObject({ duration: 7000, pitch: 45, bearing: -25 })
  await act(async () => { await vi.advanceTimersByTimeAsync(7000) })
  expect(frames).toHaveLength(1)
  act(() => frames[0](performance.now() + 1000))
  expect(mapHarness.bearings).toEqual([18])

  Object.defineProperty(document, "hidden", { configurable: true, value: true })
  act(() => document.dispatchEvent(new Event("visibilitychange")))
  expect(vi.mocked(cancelAnimationFrame)).toHaveBeenCalledWith(2)
  Object.defineProperty(document, "hidden", { configurable: true, value: false })
  act(() => document.dispatchEvent(new Event("visibilitychange")))
  expect(frames).toHaveLength(3)

  view.unmount()
})
