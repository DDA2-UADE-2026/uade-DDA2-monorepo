import { describe, expect, it } from "vitest"

import { dayIndex, dayLabel, shortTime } from "./timeRanges"

describe("dayLabel", () => {
  it("traduce los días a español", () => {
    expect(dayLabel("MONDAY")).toBe("Lunes")
    expect(dayLabel("SUNDAY")).toBe("Domingo")
  })

  it("tolera valores desconocidos o ausentes", () => {
    expect(dayLabel("FUNDAY")).toBe("FUNDAY")
    expect(dayLabel(undefined)).toBe("—")
  })
})

describe("shortTime", () => {
  it("recorta a HH:MM", () => {
    expect(shortTime("09:30:00")).toBe("09:30")
  })

  it("tolera valores ausentes", () => {
    expect(shortTime(undefined)).toBe("—")
  })
})

describe("dayIndex", () => {
  it("ordena de lunes a domingo", () => {
    expect(dayIndex("MONDAY")).toBeLessThan(dayIndex("TUESDAY"))
    expect(dayIndex("SUNDAY")).toBe(6)
  })

  it("manda lo desconocido al final", () => {
    expect(dayIndex("FUNDAY")).toBe(7)
  })
})
