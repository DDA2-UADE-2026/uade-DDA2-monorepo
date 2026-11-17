import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { UserResponse } from '@/generated/types.gen'
import { CitizenSessionCard } from './CitizenSessionCard'

const citizenId = '4cda1b10-394b-42b8-94f1-1093de89a3fb'
let me: { user: UserResponse } | undefined

vi.mock('@/hooks/use-auth', () => ({
  useMe: () => ({ data: me }),
}))

describe('CitizenSessionCard', () => {
  beforeEach(() => {
    me = undefined
  })

  it('muestra el citizen ID y el enlace a Ciudadanos en sesiones M1', () => {
    me = { user: { name: 'Vecino', authSource: 'M1', externalCitizenId: citizenId } }
    render(<CitizenSessionCard />)

    expect(screen.getByRole('region', { name: 'Sesión de Ciudadanos' })).toBeInTheDocument()
    expect(screen.getByText(citizenId)).toHaveAttribute('title', citizenId)
    const link = screen.getByRole('button', { name: /Ir a Ciudadanos/ })
    expect(link).toHaveAttribute('href', 'https://modulo1-ciudadanos-grupo8.onrender.com/ingresar')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('copia el citizen ID', async () => {
    const user = userEvent.setup()
    me = { user: { authSource: 'M1', externalCitizenId: citizenId } }
    render(<CitizenSessionCard />)

    await user.click(screen.getByRole('button', { name: 'Copiar citizen ID' }))

    await expect(navigator.clipboard.readText()).resolves.toBe(citizenId)
    expect(screen.getByRole('button', { name: 'Citizen ID copiado' })).toBeInTheDocument()
  })

  it('no aparece en sesiones locales aunque la cuenta tenga citizen ID', () => {
    me = { user: { authSource: 'LOCAL', externalCitizenId: citizenId } }
    const { container } = render(<CitizenSessionCard />)

    expect(container).toBeEmptyDOMElement()
  })

  it('no aparece mientras no hay usuario', () => {
    const { container } = render(<CitizenSessionCard />)

    expect(container).toBeEmptyDOMElement()
  })
})
