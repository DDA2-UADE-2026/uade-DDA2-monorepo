import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useTheme } from '@/hooks/use-theme'
import { ThemeProvider } from './theme-provider'

function ThemeProbe() {
  const { theme, setTheme } = useTheme()
  return (
    <div>
      <output aria-label="Tema actual">{theme}</output>
      <button onClick={() => setTheme('dark')}>Elegir oscuro</button>
      <button onClick={() => setTheme('light')}>Elegir claro</button>
      <button onClick={() => setTheme('system')}>Usar sistema</button>
    </div>
  )
}

const root = document.documentElement
let originalClasses: string

beforeEach(() => {
  originalClasses = root.className
  localStorage.clear()
})

afterEach(() => {
  root.className = originalClasses
  localStorage.clear()
  vi.restoreAllMocks()
})

describe('ThemeProvider', () => {
  it('carga el tema guardado y aplica la clase correspondiente al documento', () => {
    localStorage.setItem('preferencia', 'dark')

    render(<ThemeProvider storageKey="preferencia"><ThemeProbe /></ThemeProvider>)

    expect(screen.getByLabelText('Tema actual')).toHaveTextContent('dark')
    expect(root).toHaveClass('dark')
    expect(root).not.toHaveClass('light')
  })

  it('usa el tema predeterminado cuando no existe una preferencia persistida', () => {
    render(<ThemeProvider defaultTheme="light"><ThemeProbe /></ThemeProvider>)

    expect(screen.getByLabelText('Tema actual')).toHaveTextContent('light')
    expect(root).toHaveClass('light')
  })

  it('persiste los cambios y reemplaza la clase anterior', () => {
    render(<ThemeProvider storageKey="portal-theme"><ThemeProbe /></ThemeProvider>)

    act(() => screen.getByRole('button', { name: 'Elegir oscuro' }).click())
    expect(localStorage.getItem('portal-theme')).toBe('dark')
    expect(root).toHaveClass('dark')
    expect(root).not.toHaveClass('light')

    act(() => screen.getByRole('button', { name: 'Elegir claro' }).click())
    expect(localStorage.getItem('portal-theme')).toBe('light')
    expect(root).toHaveClass('light')
    expect(root).not.toHaveClass('dark')
  })

  it.each([
    [true, 'dark'],
    [false, 'light'],
  ] as const)('resuelve el tema del sistema (oscuro=%s)', (prefersDark, expected) => {
    vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: prefersDark }))

    render(<ThemeProvider defaultTheme="system"><ThemeProbe /></ThemeProvider>)

    expect(root).toHaveClass(expected)
    expect(root).not.toHaveClass(expected === 'dark' ? 'light' : 'dark')
  })
})
