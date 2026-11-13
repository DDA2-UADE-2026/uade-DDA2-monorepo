import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ProgramDatePicker } from './ProgramDatePicker'

beforeEach(() => vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} }))
afterEach(() => vi.unstubAllGlobals())

it('emite una fecha local sin conversión a UTC y cierra el calendario después de elegirla', async () => {
  const onChange = vi.fn()
  function Picker() {
    const [date, setDate] = useState('2026-11-15')
    return <ProgramDatePicker value={date} onChange={next => { setDate(next); onChange(next) }} />
  }
  render(<Picker />)
  await userEvent.click(screen.getByRole('button', { name: '15 de noviembre de 2026' }))
  await userEvent.click(await screen.findByRole('button', { name: /16 de noviembre de 2026/ }))
  expect(onChange).toHaveBeenCalledWith('2026-11-16')
  expect(screen.getByRole('button', { name: '16 de noviembre de 2026' })).toHaveAttribute('aria-expanded', 'false')
})

it('no permite seleccionar fechas fuera del límite o de un rango ocupado', async () => {
  const onChange = vi.fn()
  render(<ProgramDatePicker value="2026-11-15" onChange={onChange} disabled={{ before: new Date(2026, 10, 15) }} occupied={[{ from: new Date(2026, 10, 18), to: new Date(2026, 10, 20) }]} occupiedLabel="Ya asignado" />)
  await userEvent.click(screen.getByRole('button', { name: '15 de noviembre de 2026' }))
  expect(await screen.findByRole('button', { name: /14 de noviembre de 2026/ })).toBeDisabled()
  const occupied = screen.getByRole('button', { name: /19 de noviembre de 2026/ })
  expect(occupied).toBeDisabled()
  expect(screen.getByText('Ya asignado')).toBeInTheDocument()
  await userEvent.click(occupied)
  expect(onChange).not.toHaveBeenCalled()
})

it('respeta solo lectura y muestra el texto de selección cuando aún no hay fecha', async () => {
  const onChange = vi.fn()
  render(<ProgramDatePicker value="" onChange={onChange} readOnly placeholder="Elegir fecha de inicio" />)
  const trigger = screen.getByRole('button', { name: 'Elegir fecha de inicio' })
  expect(trigger).toBeDisabled()
  await userEvent.click(trigger)
  expect(onChange).not.toHaveBeenCalled()
  expect(screen.queryByRole('grid')).not.toBeInTheDocument()
})
