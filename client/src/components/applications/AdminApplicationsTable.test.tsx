import { render, screen, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { expect, it, vi } from 'vitest'
import { AdminApplicationsTable } from './AdminApplicationsTable'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, to, params, ...props }: { children?: ReactNode; to: string; params?: Record<string, string> }) => <a {...props} href={Object.entries(params ?? {}).reduce((url, [key, value]) => url.replace(`$${key}`, value), to)}>{children}</a>,
}))
const application = { id: 'app-42', applicationNumber: 42, userName: 'Ana Prueba', userEmail: 'ana@example.test', programId: 'p1', programName: 'Alimentos', programEditionId: 'e1', programEditionName: 'Anual', status: 'APPROVED' as const, submittedAt: '2026-11-13T12:00:00Z' }

it('relaciona número, titular, programa y edición con sus destinos y ofrece una acción identificable', () => {
  render(<AdminApplicationsTable applications={[application]} />)
  expect(screen.getByRole('link', { name: '42' })).toHaveAttribute('href', '/gestion/solicitudes/app-42')
  expect(screen.getByRole('link', { name: 'Ana Prueba' })).toHaveAttribute('href', '/gestion/solicitudes/app-42')
  expect(screen.getByRole('link', { name: 'Alimentos' })).toHaveAttribute('href', '/gestion/programas/p1')
  expect(screen.getByRole('link', { name: 'Anual' })).toHaveAttribute('href', '/gestion/programas/p1/convocatorias/e1')
  expect(screen.getByRole('button', { name: 'Ver la solicitud de Ana Prueba' })).toHaveAttribute('href', '/gestion/solicitudes/app-42')
  expect(screen.getByText('Aprobada')).toBeInTheDocument()
})

it('muestra solo la edición cuando el programa ya está implícito en la pantalla', () => {
  render(<AdminApplicationsTable applications={[application]} hideProgram />)
  expect(screen.getByRole('columnheader', { name: 'Edición' })).toBeInTheDocument()
  expect(screen.queryByRole('link', { name: 'Alimentos' })).not.toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Anual' })).toBeInTheDocument()
})

it('tolera datos incompletos sin ofrecer enlaces a solicitudes inexistentes', () => {
  render(<AdminApplicationsTable applications={[{}]} />)
  expect(screen.getByText('Titular sin nombre')).toBeInTheDocument()
  expect(screen.getByText('Sin estado')).toBeInTheDocument()
  expect(screen.queryByRole('link')).not.toBeInTheDocument()
  expect(screen.queryByRole('button')).not.toBeInTheDocument()
})

it('distingue rechazos, casos en trámite y una tabla sin registros', () => {
  const view = render(<AdminApplicationsTable applications={[{ id: 'reject', status: 'REJECTED' }, { id: 'pending', status: 'SUBMITTED' }]} />)
  expect(screen.getByText('Rechazada')).toBeInTheDocument()
  expect(screen.getByText('Presentada', { selector: '[data-slot="badge"]' })).toBeInTheDocument()
  view.rerender(<AdminApplicationsTable applications={[]} />)
  expect(within(screen.getByRole('table')).getAllByRole('row')).toHaveLength(1)
})
