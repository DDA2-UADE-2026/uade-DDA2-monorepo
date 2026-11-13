import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { get5QueryKey, list7QueryKey, listActivitiesQueryKey } from '@/generated/@tanstack/react-query.gen'
import { ActivityStatusDialog } from './ActivityStatusDialog'

let api: ReturnType<typeof apiHarness>
afterEach(() => { api?.restore() })

it.each([{ action: 'publish' as const, name: 'Publicar' }, { action: 'close' as const, name: 'Cerrar' }])('$name usa el endpoint correcto, invalida cachés y comunica el estado confirmado', async ({ action, name }) => {
  const saved = { id: 'act-1', name: 'Taller', status: action === 'publish' ? 'PUBLISHED' : 'CLOSED' }
  api = apiHarness(() => Response.json(saved))
  const cachedKeys = [list7QueryKey(), listActivitiesQueryKey(), get5QueryKey({ path: { id: 'act-1' } })]
  for (const key of cachedKeys) api.queryClient.setQueryData(key, { status: 'previous' })
  const close = vi.fn(); const done = vi.fn()
  api.render(<ActivityStatusDialog activity={saved} action={action} onOpenChange={close} onDone={done} />)
  expect(screen.getByRole('alertdialog')).toHaveTextContent('Taller')
  await userEvent.click(screen.getByRole('button', { name }))
  await waitFor(() => expect(done).toHaveBeenCalledWith(saved))
  expect(close).toHaveBeenCalledWith(false)
  expect(new URL(api.requests[0].url).pathname).toBe(`/api/admin/activities/act-1/${action}`)
  expect(api.requests[0].method).toBe('PATCH')
  for (const key of cachedKeys) expect(api.queryClient.getQueryState(key)?.isInvalidated).toBe(true)
})

it('mantiene abierto un cambio rechazado y permite reintentar', async () => {
  let fails = true
  api = apiHarness(() => fails ? Response.json({ message: 'La actividad ya cerró' }, { status: 409 }) : Response.json({ id: 'act-1' }))
  const close = vi.fn()
  api.render(<ActivityStatusDialog activity={{ id: 'act-1' }} action="publish" onOpenChange={close} />)
  expect(screen.getByRole('alertdialog')).toHaveTextContent('Sin nombre')
  await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
  expect(await screen.findByText('La actividad ya cerró')).toBeInTheDocument()
  expect(close).not.toHaveBeenCalled()
  fails = false
  await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
  await waitFor(() => expect(close).toHaveBeenCalledWith(false))
  expect(api.requests).toHaveLength(2)
})

it('no envía cambios sin identificador y permite cancelar', async () => {
  api = apiHarness(() => Response.json({}))
  const close = vi.fn()
  api.render(<ActivityStatusDialog activity={{}} action="close" onOpenChange={close} />)
  expect(screen.getByRole('button', { name: 'Cerrar' })).toBeDisabled()
  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
  expect(close).toHaveBeenCalledWith(false)
  expect(api.requests).toHaveLength(0)
})

it('impide duplicar el envío o cancelar mientras espera la confirmación', async () => {
  let finish!: (response: Response) => void
  api = apiHarness(() => new Promise<Response>(resolve => { finish = resolve }))
  const close = vi.fn()
  api.render(<ActivityStatusDialog activity={{ id: 'act-1' }} action="publish" onOpenChange={close} />)
  await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
  expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  expect(api.requests).toHaveLength(1)
  expect(close).not.toHaveBeenCalled()
  finish(Response.json({ id: 'act-1' }))
  await waitFor(() => expect(close).toHaveBeenCalledWith(false))
})
