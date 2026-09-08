import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ComponentProps, ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { LoginResponse } from '@/generated/types.gen'
import { storePendingRoleSelection } from '@/lib/role-selection'
import { LoginForm } from './LoginForm'

const navigateMock = vi.fn()
const mutateMock = vi.fn()

let loginState = {
  mutate: mutateMock,
  isPending: false,
  isError: false,
  error: new Error(''),
}

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-router')>(
    '@tanstack/react-router',
  )

  return {
    ...actual,
    useNavigate: () => navigateMock,
  }
})

vi.mock('@/hooks/use-auth', () => ({
  useLogin: () => loginState,
}))

vi.mock('@/components/auth/AuthCard', () => ({
  AuthCard: ({ title, children, footer }: ComponentProps<'section'> & { title: string; footer?: ReactNode }) => (
    <section aria-label={title}>
      {children}
      {footer}
    </section>
  ),
}))

describe('LoginForm', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    navigateMock.mockReset()
    mutateMock.mockReset()
    loginState = {
      mutate: mutateMock,
      isPending: false,
      isError: false,
      error: new Error(''),
    }
  })

  it('envía usuario y contraseña al submit', async () => {
    const user = userEvent.setup()
    render(<LoginForm />)

    await user.type(screen.getByLabelText('Usuario'), 'ada.lovelace')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto')
    await user.click(screen.getByRole('button', { name: /^Iniciar sesión$/ }))

    await waitFor(() => expect(mutateMock).toHaveBeenCalled())
    expect(mutateMock).toHaveBeenCalledWith(
      { body: { username: 'ada.lovelace', password: 'secreto' } },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
  })

  it('no envía credenciales vacías', async () => {
    const user = userEvent.setup()
    render(<LoginForm />)

    await user.click(screen.getByRole('button', { name: /^Iniciar sesión$/ }))

    expect(mutateMock).not.toHaveBeenCalled()
  })

  it('permite mostrar y ocultar la contraseña', async () => {
    const user = userEvent.setup()
    render(<LoginForm />)

    const password = screen.getByLabelText('Contraseña')
    expect(password).toHaveAttribute('type', 'password')

    await user.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(password).toHaveAttribute('type', 'text')

    await user.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(password).toHaveAttribute('type', 'password')
  })

  it('redirige al inicio del rol activo cuando el login devuelve token operativo', async () => {
    const user = userEvent.setup()
    const response: LoginResponse = {
      token: 'jwt-operativo',
      user: {
        username: 'ada.lovelace',
        activeRole: 'CIUDADANO',
        roles: ['CIUDADANO'],
      },
    }

    mutateMock.mockImplementation((_variables, options) => options.onSuccess(response))
    render(<LoginForm />)

    await user.type(screen.getByLabelText('Usuario'), 'ada.lovelace')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto')
    await user.click(screen.getByRole('button', { name: /^Iniciar sesión$/ }))

    expect(navigateMock).toHaveBeenCalledWith({ to: '/portal', replace: true })
  })

  it('redirige a selección de rol cuando el login queda pendiente de rol', async () => {
    const user = userEvent.setup()
    const response: LoginResponse = {
      requiresRoleSelection: true,
      selectionToken: 'jwt-seleccion',
      selectionExpiresIn: 300,
      user: {
        username: 'ada.lovelace',
        roles: ['CIUDADANO', 'OPERADOR'],
      },
    }

    mutateMock.mockImplementation((_variables, options) => {
      storePendingRoleSelection(response)
      options.onSuccess(response)
    })
    render(<LoginForm />)

    await user.type(screen.getByLabelText('Usuario'), 'ada.lovelace')
    await user.type(screen.getByLabelText('Contraseña'), 'secreto')
    await user.click(screen.getByRole('button', { name: /^Iniciar sesión$/ }))

    expect(navigateMock).toHaveBeenCalledWith({ to: '/seleccionar-rol', replace: true })
  })

  it('muestra el error de credenciales', () => {
    loginState = {
      mutate: mutateMock,
      isPending: false,
      isError: true,
      error: new Error('Credenciales inválidas'),
    }

    render(<LoginForm />)

    expect(screen.getByText('No pudimos iniciar sesión')).toBeInTheDocument()
    expect(screen.getByText('Credenciales inválidas')).toBeInTheDocument()
  })
})
