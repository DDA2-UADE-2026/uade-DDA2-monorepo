import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import { TimeRangeDialog } from './TimeRangeDialog'

const defaults = { title: 'Nueva franja', description: 'Horario de atención', initial: { dayOfWeek: 'MONDAY' as const, startTime: '09:00', endTime: '12:00' }, pending: false, error: null, submitLabel: 'Guardar', onOpenChange: vi.fn() }

it('sincroniza el día seleccionado con la lista enviada para una franja individual', async () => {
  const onSubmit = vi.fn()
  render(<TimeRangeDialog {...defaults} onSubmit={onSubmit} />)
  await userEvent.click(screen.getByRole('combobox'))
  await userEvent.click(await screen.findByRole('option', { name: 'Miércoles' }))
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ dayOfWeek: 'WEDNESDAY', days: ['WEDNESDAY'], startTime: '09:00', endTime: '12:00' }))
})

it('envía los días elegidos y toma el primero como día principal al crear varias franjas', async () => {
  const onSubmit = vi.fn()
  render(<TimeRangeDialog {...defaults} allowMultipleDays onSubmit={onSubmit} />)
  await userEvent.click(screen.getByRole('checkbox', { name: /Lunes/ }))
  await userEvent.click(screen.getByRole('checkbox', { name: /Martes/ }))
  await userEvent.click(screen.getByRole('checkbox', { name: /Jueves/ }))
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ dayOfWeek: 'TUESDAY', days: ['TUESDAY', 'THURSDAY'], startTime: '09:00', endTime: '12:00' }))
})

it('impide guardar sin días y habilita el formulario al corregirlos', async () => {
  const onSubmit = vi.fn()
  render(<TimeRangeDialog {...defaults} allowMultipleDays onSubmit={onSubmit} />)
  await userEvent.click(screen.getByRole('checkbox', { name: /Lunes/ }))
  expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled()
  expect(onSubmit).not.toHaveBeenCalled()
  await userEvent.click(screen.getByRole('checkbox', { name: /Domingo/ }))
  expect(screen.getByRole('button', { name: 'Guardar' })).toBeEnabled()
})

it.each(['08:00', '09:00'])('rechaza fin %s anterior o igual al inicio y permite corregirlo', async end => {
  const onSubmit = vi.fn()
  render(<TimeRangeDialog {...defaults} onSubmit={onSubmit} />)
  fireEvent.change(screen.getByLabelText('Fin'), { target: { value: end } })
  fireEvent.blur(screen.getByLabelText('Fin'))
  expect(await screen.findByText('El inicio debe ser anterior al fin.')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '13:00' } })
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ endTime: '13:00' })))
})

it('bloquea envío y cierre durante el guardado y presenta errores específicos del backend', async () => {
  const onSubmit = vi.fn(); const onOpenChange = vi.fn()
  render(<TimeRangeDialog {...defaults} pending onSubmit={onSubmit} onOpenChange={onOpenChange} error={{ message: 'Franja superpuesta', fields: [{ field: 'startTime', message: 'Se superpone con la atención existente' }] }} />)
  expect(screen.getByText('Franja superpuesta')).toBeInTheDocument()
  expect(screen.getByText('Se superpone con la atención existente')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  await userEvent.keyboard('{Escape}')
  expect(onOpenChange).not.toHaveBeenCalled()
  expect(onSubmit).not.toHaveBeenCalled()
})
