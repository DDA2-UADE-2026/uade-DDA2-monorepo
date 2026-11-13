import { act, render } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

const router = vi.hoisted(() => ({ status: 'idle' as 'idle' | 'pending' }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useRouterState: ({ select }: { select: (state: { status: string }) => boolean }) => select(router),
  }
})

import { AppLoadingBar } from './AppLoadingBar'

afterEach(() => {
  router.status = 'idle'
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('AppLoadingBar', () => {
  it('aparece al comenzar la navegación, avanza y se oculta al terminar', () => {
    vi.useFakeTimers()
    vi.spyOn(Math, 'random').mockReturnValue(0.9)
    const view = render(<AppLoadingBar />)
    expect(view.container.firstChild).toBeNull()

    router.status = 'pending'
    view.rerender(<AppLoadingBar />)
    const progress = view.container.querySelector('[style]') as HTMLElement
    expect(progress).toHaveStyle({ transform: 'scaleX(0.1)' })

    act(() => vi.advanceTimersByTime(4500))
    expect(progress).toHaveStyle({ transform: 'scaleX(0.9)' })

    router.status = 'idle'
    view.rerender(<AppLoadingBar />)
    expect(progress).toHaveStyle({ transform: 'scaleX(1)' })
    act(() => vi.advanceTimersByTime(300))
    expect(view.container.firstChild).toBeNull()
  })

  it('finaliza la animación de avance al terminar una navegación', () => {
    vi.useFakeTimers()
    const view = render(<AppLoadingBar />)
    router.status = 'pending'
    view.rerender(<AppLoadingBar />)
    expect(view.container.querySelector('[style]')).toBeInTheDocument()

    router.status = 'idle'
    view.rerender(<AppLoadingBar />)
    act(() => vi.advanceTimersByTime(300))
    expect(view.container.firstChild).toBeNull()
  })
})
