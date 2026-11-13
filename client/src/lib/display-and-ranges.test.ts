import { describe, expect, it } from 'vitest'
import { getPaginationRange } from './pagination'
import { getUserInitials } from './user-display'
import { formatProgramDate } from './program-dates'
import { coreStatusLabel, formatDateTime } from './core-events'
import { delta, formatBucket, formatBucketLong, formatCount, formatDate, formatDays, formatRate, presetRange, toIsoDate } from './indicators'

describe('navegación paginada', () => {
  it.each([
    [1, 0, []], [1, 1, [1]], [4, 7, [1, 2, 3, 4, 5, 6, 7]],
    [1, 10, [1, 2, 'ellipsis', 10]], [3, 10, [1, 2, 3, 4, 'ellipsis', 10]],
    [5, 10, [1, 'ellipsis', 4, 5, 6, 'ellipsis', 10]],
    [9, 10, [1, 'ellipsis', 8, 9, 10]], [10, 10, [1, 'ellipsis', 9, 10]],
  ])('página %s de %s conserva extremos y vecinos', (page, total, expected) => {
    expect(getPaginationRange(page as number, total as number)).toEqual(expected)
  })
})

it.each([['', '?'], ['   ', '?'], [' ana ', 'AN'], ['Ana María Pérez', 'AP'], ['élena gómez', 'ÉG'], ['A', 'A']])('iniciales de %j no exponen texto sobrante', (input, expected) => {
  expect(getUserInitials(input)).toBe(expected)
})

describe('indicadores y fechas visibles', () => {
  it('distingue ausencia, cero, miles y cantidades compactas', () => {
    expect(formatCount(undefined)).toBe('—')
    expect(formatCount(null)).toBe('—')
    expect(formatCount(0)).toBe('0')
    expect(formatCount(9999)).toBe('9.999')
    expect(formatCount(12900)).toMatch(/12,9.*(?:mil|k)/)
    expect(formatCount(-12900)).toMatch(/-12,9.*(?:mil|k)/)
    expect(formatRate(0)).toMatch(/^0\s*%$/)
    expect(formatRate(0.625)).toMatch(/^63\s*%$/)
    expect(formatRate(null)).toBe('—')
    expect(formatDays(1)).toBe('1 día')
    expect(formatDays(1.5)).toBe('1,5 días')
    expect(formatDays(undefined)).toBe('—')
  })
  it('no inventa una variación cuando no existe base de comparación', () => {
    expect(delta(12, 0)).toBeNull()
    expect(delta(0, 0)).toBeNull()
    expect(delta(20, 10)).toBe(1)
    expect(delta(5, 10)).toBe(-0.5)
    expect(delta(10, 10)).toBe(0)
  })
  it('conserva el día local y distingue semanas de meses', () => {
    expect(toIsoDate(new Date(2026, 0, 2, 23, 59))).toBe('2026-01-02')
    expect(formatDate('2024-02-29')).toBe('29 de febrero de 2024')
    expect(formatBucket('2026-11-13', 'WEEK')).toMatch(/13.*nov/)
    expect(formatBucket('2026-11-13', 'MONTH')).toMatch(/nov.*26/)
    expect(formatBucketLong('2026-11-13', 'WEEK')).toBe('Semana del 13 de noviembre de 2026')
    expect(formatBucketLong('2026-11-13', 'MONTH')).toBe('noviembre de 2026')
    expect(formatProgramDate()).toBe('A confirmar')
    expect(formatProgramDate('2024-02-29')).toMatch(/29.*feb.*2024/)
  })
  it.each([
    ['30d', '2025-12-16'], ['90d', '2025-10-17'], ['12m', '2025-01-15'], ['anio', '2026-01-01'],
  ] as const)('rango %s incluye hoy sin modificar la fecha de entrada', (preset, from) => {
    const today = new Date(2026, 0, 14, 12)
    expect(presetRange(preset, today)).toEqual({ from, to: '2026-01-14' })
    expect(today.getDate()).toBe(14)
    expect(today.getFullYear()).toBe(2026)
  })
  it('muestra estados desconocidos y fechas inválidas sin inventar datos', () => {
    expect(coreStatusLabel('PENDING_RETRY')).toBe('Reintento pendiente')
    expect(coreStatusLabel('NEW_STATE')).toBe('NEW_STATE')
    expect(coreStatusLabel()).toBe('—')
    expect(formatDateTime()).toBe('—')
    expect(formatDateTime('not-a-date')).toBe('—')
    expect(formatDateTime('2026-11-13T12:34:56')).toContain('12:34:56')
  })
})
