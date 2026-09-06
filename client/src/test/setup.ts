import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'
import { z } from 'zod'
import { es } from 'zod/locales'

afterEach(() => {
  cleanup()
})

z.config({
  ...es(),
  customError: (issue) =>
    issue.input === undefined || issue.input === ""
      ? "Este campo es obligatorio."
      : undefined,
})
