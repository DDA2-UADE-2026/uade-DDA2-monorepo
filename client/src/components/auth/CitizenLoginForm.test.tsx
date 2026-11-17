import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ComponentProps, ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { CookieSessionResponse } from '@/generated/types.gen'
import { CitizenLoginForm } from './CitizenLoginForm'

const navigateMock = vi.fn()
const mutateMock = vi.fn()

type LoginState = {
  mutate: typeof mutateMock
  isPending: boolean
  isError: boolean
  error: { code?: string; message?: string } | null
  failureCount: number
  submittedAt: number
}

const idle = (): LoginState => ({
  mutate: mutateMock,
  isPending: false,
  isError: false,
  error: null,
  failureCount: 0,
  submittedAt: 0,
})

let loginState = idle()

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-router')>(
    '@tanstack/react-router',
  )

  return {
    ...actual,
    useNavigate: () => navigateMock,
    Link: ({ to, children, ...props }: ComponentProps<'a'> & { to: string }) => (
      <a href={to} {...props}>
        {children}
      </a>
    ),
  }
})

vi.mock('@/hooks/use-auth', async () => {
  const actual = await vi.importActual<typeof import('@/hooks/use-auth')>('@/hooks/use-auth')
  return {
    ...actual,
    useM1Login: () => loginState,
  }
})

vi.mock('@/components/auth/AuthCard', () => ({
  AuthCard: ({ title, children, footer }: ComponentProps<'section'> & { title: string; footer?: ReactNode }) => (
    <section aria-label={title}>
      {children}
      {footer}
    </section>
  ),
}))

describe('CitizenLoginForm', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    mutateMock.mockReset()
    loginState = idle()
  })

  it('envía el correo sin espacios y la contraseña tal cual', async () => {
    const user = userEvent.setup()
    render(<CitizenLoginForm />)

    await user.type(screen.getByLabelText('Correo de Ciudadanos'), '  vecino@ejemplo.com ')
    await user.type(screen.getByLabelText('Contraseña de Ciudadanos'), ' clave ')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    await waitFor(() => expect(mutateMock).toHaveBeenCalled())
    expect(mutateMock).toHaveBeenCalledWith(
      { body: { email: 'vecino@ejemplo.com', password: ' clave ' } },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
  })

  it.each(['20234234559', '20-23423455-9'])('avisa que %s parece un CUIT y no lo envía', async (cuit) => {
    const user = userEvent.setup()
    render(<CitizenLoginForm />)

    await user.type(screen.getByLabelText('Correo de Ciudadanos'), cuit)
    await user.type(screen.getByLabelText('Contraseña de Ciudadanos'), 'clave')
    expect(screen.getByText(/Eso parece un CUIT/)).toBeInTheDocument()
    expect(screen.getByLabelText('Correo de Ciudadanos')).toHaveAttribute('aria-invalid', 'true')

    await user.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(mutateMock).not.toHaveBeenCalled()
  })

  it('redirige al portal ciudadano al iniciar sesión', async () => {
    const user = userEvent.setup()
    const response: CookieSessionResponse = {
      user: { username: 'vecino', activeRole: 'CIUDADANO', roles: ['CIUDADANO'] },
    }
    mutateMock.mockImplementation((_variables, options) => options.onSuccess(response))
    render(<CitizenLoginForm />)

    await user.type(screen.getByLabelText('Correo de Ciudadanos'), 'vecino@ejemplo.com')
    await user.type(screen.getByLabelText('Contraseña de Ciudadanos'), 'clave')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(navigateMock).toHaveBeenCalledWith({ to: '/portal', replace: true })
  })

  it('aclara que no guarda la contraseña y ofrece el ingreso interno', () => {
    render(<CitizenLoginForm />)

    expect(screen.getByText(/tu contraseña pasa por nuestros servidores/)).toBeInTheDocument()
    expect(screen.getByText('Desarrollo Social no guarda tu contraseña')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Usá el ingreso interno' })).toHaveAttribute('href', '/login')
  })

  it('avisa cuando Ciudadanos tarda en responder', () => {
    loginState = { ...idle(), isPending: true, submittedAt: Date.now() - 10_000 }
    render(<CitizenLoginForm />)

    expect(screen.getByText('Conectando con Ciudadanos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Verificando con Ciudadanos/ })).toBeDisabled()
  })

  it('muestra el reintento automático en curso', () => {
    loginState = { ...idle(), isPending: true, failureCount: 1, submittedAt: Date.now() - 20_000 }
    render(<CitizenLoginForm />)

    expect(screen.getByText('Ciudadanos se está iniciando')).toBeInTheDocument()
    expect(screen.getByText(/intento 2 de 3/)).toBeInTheDocument()
    expect(screen.queryByText('Conectando con Ciudadanos')).not.toBeInTheDocument()
  })

  it('ofrece reintentar cuando M1 no respondió a tiempo', () => {
    loginState = { ...idle(), isError: true, error: { code: 'M1_AUTH_TIMEOUT', message: 'M1 no respondió.' } }
    render(<CitizenLoginForm />)

    expect(screen.getByText('Ciudadanos no respondió a tiempo')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
    expect(screen.queryByText('No pudimos iniciar sesión')).not.toBeInTheDocument()
  })

  it('muestra el mensaje del servidor ante credenciales inválidas', () => {
    loginState = {
      ...idle(),
      isError: true,
      error: { code: 'M1_AUTH_INVALID_CREDENTIALS', message: 'Correo o contraseña incorrectos.' },
    }
    render(<CitizenLoginForm />)

    expect(screen.getByText('No pudimos iniciar sesión')).toBeInTheDocument()
    expect(screen.getByText('Correo o contraseña incorrectos.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })
})
