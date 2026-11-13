import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, expect, it, vi } from 'vitest'
import { ErrorCard } from './ErrorCard'
import { NotFoundCard } from './NotFoundCard'

const history = vi.hoisted(() => ({ canGoBack: false, back: vi.fn() }))
vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(), useCanGoBack: () => history.canGoBack, useRouter: () => ({ history: { back: history.back } }),
  Link: ({ children, to, ...props }: { children?: ReactNode; to: string }) => <a {...props} href={to}>{children}</a>,
}))
beforeEach(() => { history.canGoBack = false; history.back.mockReset() })

it('permite reintentar el error y consultar el detalle técnico solo cuando se solicita', async () => {
  const reset = vi.fn()
  render(<ErrorCard error={new Error('Servicio interrumpido')} info={{ componentStack: 'ComponentStack: ApplicationPage' }} reset={reset} homeHref="/portal" homeLabel="Volver al portal" />)
  expect(screen.getByText('Servicio interrumpido')).toBeInTheDocument()
  expect(screen.queryByText('ComponentStack: ApplicationPage')).not.toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Detalles del error' }))
  expect(await screen.findByText('ComponentStack: ApplicationPage')).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
  expect(reset).toHaveBeenCalledOnce()
  expect(screen.getByRole('button', { name: 'Volver al portal' })).toHaveAttribute('href', '/portal')
})

it('prioriza una descripción orientada al usuario sobre el mensaje técnico', () => {
  render(<ErrorCard error={new Error('Internal database error')} reset={vi.fn()} title="No se pudo abrir el trámite" description="Intentá nuevamente más tarde." />)
  expect(screen.getByText('No se pudo abrir el trámite')).toBeInTheDocument()
  expect(screen.getByText('Intentá nuevamente más tarde.')).toBeInTheDocument()
  expect(screen.queryByText('Internal database error')).not.toBeInTheDocument()
})

it('ofrece volver al inicio si no existe una página previa en el historial', () => {
  render(<NotFoundCard path="/portal/inexistente" homeHref="/portal" />)
  expect(screen.getByText('/portal/inexistente')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Volver atrás' })).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Ir al inicio' })).toHaveAttribute('href', '/portal')
})

it('permite recuperar la página anterior cuando el historial está disponible', async () => {
  history.canGoBack = true
  render(<NotFoundCard />)
  await userEvent.click(screen.getByRole('button', { name: 'Volver atrás' }))
  expect(history.back).toHaveBeenCalledOnce()
})
