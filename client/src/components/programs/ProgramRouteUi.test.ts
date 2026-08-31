import { describe, expect, it } from 'vitest'
import { parseLocalDate } from './ProgramRouteUi'

describe('parseLocalDate', () => {
  it('convierte una fecha en formato YYYY-MM-DD', () => {
    const result = parseLocalDate('2026-08-31')

    expect(result?.getFullYear()).toBe(2026)
    expect(result?.getMonth()).toBe(7)
    expect(result?.getDate()).toBe(31)
  })
})