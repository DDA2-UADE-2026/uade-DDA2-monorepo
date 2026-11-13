import { screen, waitFor, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { ProgramTicker } from './ProgramTicker'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, to, params, ...props }: { children?: ReactNode; to: string; params?: { programaId: string } }) => <a {...props} href={params ? to.replace('$programaId', params.programaId) : to}>{children}</a>,
}))
let api: ReturnType<typeof apiHarness>
afterEach(() => api.restore())

it('consulta programas disponibles y mantiene las copias de animación fuera de la navegación accesible', async () => {
  api = apiHarness(() => Response.json({ content: [{ id: 'p1', name: 'Alimentos', objective: 'Mejorar la nutrición', availableEditions: 1, nextEditionStartDate: '2026-11-15' }] }))
  api.render(<ProgramTicker />)
  const region = await screen.findByRole('region', { name: 'Programas disponibles' })
  await waitFor(() => expect(screen.queryByLabelText('Cargando programas')).not.toBeInTheDocument())
  const lists = region.querySelectorAll('ul')
  expect(lists).toHaveLength(2)
  expect(lists[1]).toHaveAttribute('aria-hidden', 'true')
  for (const link of lists[1].querySelectorAll('a')) expect(link).toHaveAttribute('tabindex', '-1')
  expect(within(region).getByRole('button', { name: 'Ver todos' })).toHaveAttribute('href', '/portal/programas')
  expect(within(lists[0]).getAllByRole('link', { name: /Alimentos/ })[0]).toHaveAttribute('href', '/portal/programas/p1')
  expect(lists[0]).toHaveTextContent('Mejorar la nutrición')
  expect(lists[0]).toHaveTextContent('1 edición')
  const query = new URL(api.requests[0].url)
  expect(query.pathname).toBe('/api/programs')
  expect(query.searchParams.get('page')).toBe('0')
  expect(query.searchParams.get('size')).toBe('12')
})

it('usa textos informativos cuando faltan nombre, objetivo, fecha y ediciones', async () => {
  api = apiHarness(() => Response.json({ content: [{}] }))
  api.render(<ProgramTicker />)
  await waitFor(() => expect(screen.queryByLabelText('Cargando programas')).not.toBeInTheDocument())
  const list = screen.getByRole('list')
  expect(list).toHaveTextContent('Programa sin nombre')
  expect(list).toHaveTextContent('El objetivo de este programa se informará próximamente.')
  expect(list).toHaveTextContent('Fecha a confirmar')
  expect(list).toHaveTextContent(/\(0 /)
})

it.each(['empty', 'error'])('oculta la sección si no puede ofrecer programas: %s', async condition => {
  api = apiHarness(() => condition === 'error' ? Response.json({ message: 'Unavailable' }, { status: 503 }) : Response.json({ content: [] }))
  const view = api.render(<ProgramTicker />)
  await waitFor(() => expect(view.container).toBeEmptyDOMElement())
  expect(screen.queryByRole('region')).not.toBeInTheDocument()
})
