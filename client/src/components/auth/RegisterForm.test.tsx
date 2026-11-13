import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { RegisterForm } from './RegisterForm'

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ children, to, ...props }: React.ComponentProps<'a'> & { to: string }) => <a href={to} {...props}>{children}</a>,
  }
})

describe('RegisterForm', () => {
  it('explica que el registro está temporalmente deshabilitado', () => {
    render(<RegisterForm />)

    expect(screen.getByText('Creá tu cuenta')).toBeInTheDocument()
    expect(screen.getByText('El registro está deshabilitado')).toBeInTheDocument()
    expect(screen.getByText(/no se pueden crear cuentas nuevas/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Crear cuenta' })).toBeDisabled()
  })

  it('mantiene los campos personales y las acciones inactivos mientras no hay registro público', () => {
    render(<RegisterForm />)

    const fields = [
      'Nombre completo',
      'Usuario',
      'Correo electrónico',
      'Contraseña',
      'Confirmar contraseña',
    ]
    for (const name of fields) expect(screen.getByLabelText(name)).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Mostrar contraseña' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Ocultar contraseña' })).not.toBeInTheDocument()
  })

  it('ofrece un enlace directo a iniciar sesión', () => {
    render(<RegisterForm />)

    const login = screen.getByRole('link', { name: 'Iniciá sesión' })
    expect(login).toHaveAttribute('href', '/login')
    expect(within(login).getByText('Iniciá sesión')).toBeInTheDocument()
  })
})
