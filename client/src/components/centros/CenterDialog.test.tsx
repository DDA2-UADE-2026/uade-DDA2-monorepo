import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import { CenterDialog } from './CenterDialog'

let api: ReturnType<typeof apiHarness>
beforeEach(() => { api = apiHarness(() => Response.json({ id: 'center-1' })) })
afterEach(() => { api.restore(); vi.restoreAllMocks() })

it('crea el centro normalizando espacios y omitiendo contactos vacíos', async () => {
  const close = vi.fn(), saved = vi.fn(), user = userEvent.setup()
  api.render(<CenterDialog center={null} onOpenChange={close} onSaved={saved} />)
  await user.type(screen.getByLabelText('Nombre'), '  Centro Norte  ')
  await user.type(screen.getByLabelText('Dirección'), '  Calle 123  ')
  await user.click(screen.getByRole('button', { name: 'Crear centro' }))
  await waitFor(() => expect(close).toHaveBeenCalledWith(false))
  expect(saved).toHaveBeenCalledOnce()
  expect(api.requests[0].method).toBe('POST')
  expect(await api.requests[0].json()).toEqual({ name: 'Centro Norte', address: 'Calle 123' })
})

it('edita el identificador existente y envía los datos actualizados', async () => {
  const close = vi.fn(), user = userEvent.setup()
  api.render(<CenterDialog center={{ id: 'center-7', name: 'Centro Sur', address: 'Calle 7', phone: '123', email: 'sur@example.test' }} onOpenChange={close} />)
  expect(screen.getByLabelText('Nombre')).toHaveValue('Centro Sur')
  await user.clear(screen.getByLabelText('Dirección'))
  await user.type(screen.getByLabelText('Dirección'), 'Avenida 20')
  await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
  await waitFor(() => expect(close).toHaveBeenCalledWith(false))
  expect(new URL(api.requests[0].url).pathname).toBe('/api/admin/municipal-centers/center-7')
  expect(api.requests[0].method).toBe('PUT')
  expect(await api.requests[0].json()).toEqual({ name: 'Centro Sur', address: 'Avenida 20', phone: '123', email: 'sur@example.test' })
})

it('rechaza nombre en blanco y correo inválido antes de llamar al servidor', async () => {
  const user = userEvent.setup()
  api.render(<CenterDialog center={null} onOpenChange={vi.fn()} />)
  await user.type(screen.getByLabelText('Nombre'), '   ')
  await user.tab()
  expect(await screen.findByText('Ingresá el nombre del centro.')).toBeInTheDocument()
  await user.clear(screen.getByLabelText('Nombre'))
  await user.type(screen.getByLabelText('Nombre'), 'Centro')
  await user.type(screen.getByLabelText('Dirección'), 'Calle 3')
  await user.type(screen.getByLabelText('Correo (opcional)'), 'invalido')
  await user.tab()
  expect(await screen.findByText('Ingresá un correo válido.')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Crear centro' })).toBeDisabled()
  expect(api.requests).toHaveLength(0)
})

it('muestra errores de campos del servidor sin cerrar ni perder valores', async () => {
  api.restore()
  api = apiHarness(() => Response.json({ message: 'No se pudo registrar', fields: [{ field: 'name', message: 'El nombre ya existe' }] }, { status: 409 }))
  const close = vi.fn(), user = userEvent.setup()
  api.render(<CenterDialog center={null} onOpenChange={close} />)
  await user.type(screen.getByLabelText('Nombre'), 'Centro Norte')
  await user.type(screen.getByLabelText('Dirección'), 'Calle 3')
  await user.click(screen.getByRole('button', { name: 'Crear centro' }))
  expect(await screen.findByText('El nombre ya existe')).toBeInTheDocument()
  expect(screen.getByLabelText('Nombre')).toHaveValue('Centro Norte')
  expect(close).not.toHaveBeenCalled()
})

it('cancelar no envía cambios', async () => {
  const close = vi.fn()
  api.render(<CenterDialog center={null} onOpenChange={close} />)
  await userEvent.setup().click(screen.getByRole('button', { name: 'Cancelar' }))
  expect(close).toHaveBeenCalledWith(false)
  expect(api.requests).toHaveLength(0)
})
