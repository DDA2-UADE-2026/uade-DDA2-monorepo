import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import { useState } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { DateTimePicker } from './DateTimePicker'

beforeEach(() => vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} }))
afterEach(() => vi.unstubAllGlobals())

it('conserva una hora parcial y solo emite fecha y hora completas después de elegir el día', async () => {
  const changed = vi.fn()
  function Picker() {
    const [value, setValue] = useState('')
    return <DateTimePicker value={value} onChange={next => { setValue(next); changed(next) }} />
  }
  render(<Picker />)
  fireEvent.change(screen.getByLabelText('Hora'), { target: { value: '10:30' } })
  expect(changed).toHaveBeenLastCalledWith('')
  await userEvent.click(screen.getByRole('button', { name: 'Seleccionar fecha' }))
  await userEvent.click(await screen.findByRole('button', { name: new RegExp(format(new Date(), "d 'de' MMMM 'de' yyyy", { locale: es })) }))
  expect(changed).toHaveBeenLastCalledWith(`${format(new Date(), 'yyyy-MM-dd')}T10:30`)
  expect(screen.getByLabelText('Hora')).toHaveValue('10:30')
})

it('respeta la fecha y la hora mínimas del día elegido sin convertir a UTC', async () => {
  const changed = vi.fn()
  render(<DateTimePicker value="2026-11-15T10:30" min="2026-11-15T09:00" required timeLabel="Hora de inicio" onChange={changed} />)
  expect(screen.getByLabelText('Hora de inicio')).toHaveAttribute('min', '09:00')
  expect(screen.getByLabelText('Hora de inicio')).toBeRequired()
  fireEvent.change(screen.getByLabelText('Hora de inicio'), { target: { value: '11:45' } })
  expect(changed).toHaveBeenLastCalledWith('2026-11-15T11:45')
  await userEvent.click(screen.getByRole('button', { name: '15 de noviembre de 2026' }))
  expect(await screen.findByRole('button', { name: /14 de noviembre de 2026/ })).toBeDisabled()
})

it('libera la restricción horaria cuando la fecha elegida es posterior al día mínimo', () => {
  render(<DateTimePicker value="2026-11-16T08:00" min="2026-11-15T09:00" onChange={vi.fn()} />)
  expect(screen.getByLabelText('Hora')).not.toHaveAttribute('min')
  expect(screen.getByLabelText('Hora')).toHaveValue('08:00')
})
