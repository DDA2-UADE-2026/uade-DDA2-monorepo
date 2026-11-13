import { act, render, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, expect, it, vi } from "vitest"

const raysHarness = vi.hoisted(() => ({
  observerCallback: null as ((entries: { isIntersecting: boolean }[]) => void) | null,
  observerDisconnects: 0,
  observed: null as Element | null,
  rendererOptions: null as Record<string, unknown> | null,
  canvas: null as HTMLCanvasElement | null,
  setSizes: [] as number[][],
  renders: 0,
  uniforms: null as Record<string, { value: number | number[] }> | null,
  frame: null as ((time: number) => void) | null,
  cancelledFrames: [] as number[],
  loseContexts: 0,
  throwOnRender: false,
}))

vi.mock("ogl", () => ({
  Renderer: class {
    dpr = 1
    gl = {
      canvas: document.createElement("canvas"),
      getExtension: () => ({ loseContext: () => { raysHarness.loseContexts++ } }),
    }
    constructor(options: Record<string, unknown>) { raysHarness.rendererOptions = options; raysHarness.canvas = this.gl.canvas }
    setSize(width: number, height: number) { raysHarness.setSizes.push([width, height]) }
    render() { raysHarness.renders++; if (raysHarness.throwOnRender) throw new Error("WebGL lost") }
  },
  Triangle: class { constructor(gl: unknown) { void gl } },
  Program: class { constructor(gl: unknown, options: { uniforms: Record<string, { value: number | number[] }> }) { void gl; raysHarness.uniforms = options.uniforms } },
  Mesh: class { constructor(gl: unknown, options: unknown) { void gl; void options } },
}))

import SideRaysBackground from "./SideRaysBackground"

beforeEach(() => {
  raysHarness.observerCallback = null
  raysHarness.observerDisconnects = 0
  raysHarness.observed = null
  raysHarness.rendererOptions = null
  raysHarness.canvas = null
  raysHarness.setSizes = []
  raysHarness.renders = 0
  raysHarness.uniforms = null
  raysHarness.frame = null
  raysHarness.cancelledFrames = []
  raysHarness.loseContexts = 0
  raysHarness.throwOnRender = false
  vi.stubGlobal("IntersectionObserver", class {
    constructor(callback: (entries: { isIntersecting: boolean }[]) => void) { raysHarness.observerCallback = callback }
    observe(element: Element) { raysHarness.observed = element }
    disconnect() { raysHarness.observerDisconnects++ }
  })
  vi.stubGlobal("requestAnimationFrame", (callback: (time: number) => void) => { raysHarness.frame = callback; return 11 })
  vi.stubGlobal("cancelAnimationFrame", (id: number) => { raysHarness.cancelledFrames.push(id) })
  Object.defineProperty(window, "devicePixelRatio", { configurable: true, value: 3 })
})

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks() })

it("espera visibilidad, inicializa WebGL con las opciones y adapta tamaño y shader", async () => {
  const view = render(<SideRaysBackground className="ray-test" origin="bottom-left" rayColor1="#ff0000" speed={4} />)
  expect(raysHarness.observed).toBe(view.container.firstChild)
  expect(raysHarness.rendererOptions).toBeNull()

  act(() => raysHarness.observerCallback?.([{ isIntersecting: true }]))
  await waitFor(() => expect(raysHarness.canvas?.parentElement).toBe(view.container.firstChild))
  expect(raysHarness.rendererOptions).toEqual({ dpr: 2, alpha: true })
  expect(raysHarness.setSizes).toEqual([[0, 0]])
  expect(raysHarness.uniforms?.iFlipX.value).toBe(1)
  expect(raysHarness.uniforms?.iFlipY.value).toBe(1)
  expect(raysHarness.uniforms?.iRayColor1.value).toEqual([1, 0, 0])
  expect(raysHarness.uniforms?.iSpeed.value).toBe(4)

  act(() => raysHarness.frame?.(2500))
  expect(raysHarness.renders).toBe(1)
  expect(raysHarness.uniforms?.iTime.value).toBe(2.5)
  window.dispatchEvent(new Event("resize"))
  expect(raysHarness.setSizes).toHaveLength(2)

  view.unmount()
  expect(raysHarness.observerDisconnects).toBe(1)
  expect(raysHarness.cancelledFrames).toContain(11)
  expect(raysHarness.loseContexts).toBe(1)
  expect(raysHarness.canvas?.isConnected).toBe(false)
})

it("detiene el ciclo si WebGL lanza al renderizar y cubre colores inválidos", async () => {
  raysHarness.throwOnRender = true
  const view = render(<SideRaysBackground origin="top-left" rayColor2="not-a-color" />)
  act(() => raysHarness.observerCallback?.([{ isIntersecting: true }]))
  await waitFor(() => expect(raysHarness.uniforms).not.toBeNull())
  expect(raysHarness.uniforms?.iFlipX.value).toBe(1)
  expect(raysHarness.uniforms?.iFlipY.value).toBe(0)
  expect(raysHarness.uniforms?.iRayColor2.value).toEqual([1, 1, 1])

  act(() => raysHarness.frame?.(1000))
  expect(raysHarness.renders).toBe(1)
  expect(raysHarness.frame).not.toBeNull()
  view.unmount()
  expect(raysHarness.observerDisconnects).toBe(1)
})

it.each([
  ["top-right", 0, 0],
  ["bottom-right", 0, 1],
] as const)("orienta los rayos desde %s", async (origin, flipX, flipY) => {
  const view = render(<SideRaysBackground origin={origin} />)
  act(() => raysHarness.observerCallback?.([{ isIntersecting: true }]))
  await waitFor(() => expect(raysHarness.uniforms).not.toBeNull())

  expect(raysHarness.uniforms?.iFlipX.value).toBe(flipX)
  expect(raysHarness.uniforms?.iFlipY.value).toBe(flipY)
  view.unmount()
})
