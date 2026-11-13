import { act, renderHook } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { useOnlineStatus } from './use-online-status'
import { useIsMobile } from './use-mobile'

const originalWidth = Object.getOwnPropertyDescriptor(window, 'innerWidth')!
afterEach(() => { vi.restoreAllMocks(); Object.defineProperty(window, 'innerWidth', originalWidth) })

it('refleja desconexión y reconexión y retira los listeners al desmontar', () => {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true)
  const remove = vi.spyOn(window, 'removeEventListener')
  const view = renderHook(() => useOnlineStatus())
  expect(view.result.current).toBe(true)
  act(() => window.dispatchEvent(new Event('offline')))
  expect(view.result.current).toBe(false)
  act(() => window.dispatchEvent(new Event('online')))
  expect(view.result.current).toBe(true)
  view.unmount()
  expect(remove).toHaveBeenCalledWith('online', expect.any(Function))
  expect(remove).toHaveBeenCalledWith('offline', expect.any(Function))
})

it('detecta el límite móvil de 768 px y responde a cambios de media query', () => {
  let callback: (() => void) | undefined
  const remove = vi.fn()
  vi.spyOn(window, 'matchMedia').mockImplementation(query => ({
    media: query, matches: true, onchange: null, addListener: vi.fn(), removeListener: vi.fn(), dispatchEvent: vi.fn(),
    addEventListener: vi.fn((_event, listener) => { callback = listener as () => void }), removeEventListener: remove,
  }))
  Object.defineProperty(window, 'innerWidth', { configurable: true, writable: true, value: 767 })
  const view = renderHook(() => useIsMobile())
  expect(view.result.current).toBe(true)
  window.innerWidth = 768
  act(() => callback?.())
  expect(view.result.current).toBe(false)
  view.unmount()
  expect(remove).toHaveBeenCalledWith('change', expect.any(Function))
})
