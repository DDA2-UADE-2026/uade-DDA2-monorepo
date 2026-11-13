import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import {
  Combobox,
  ComboboxChips,
  ComboboxChip,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
  ComboboxValue,
} from './combobox'

const options = ['Alimentación', 'Acompañamiento', 'Vivienda']

function SingleSelect({ onValueChange = vi.fn(), disabled = false }: { onValueChange?: (value: string | null) => void; disabled?: boolean }) {
  return (
    <Combobox items={options} onValueChange={onValueChange}>
      <ComboboxInput aria-label="Buscar programa" showClear disabled={disabled} />
      <ComboboxContent>
        <ComboboxEmpty>Sin resultados</ComboboxEmpty>
        <ComboboxList>
          {(item: string) => <ComboboxItem key={item} value={item}>{item}</ComboboxItem>}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  )
}

describe('Combobox', () => {
  it('filtra opciones al escribir y comunica la opción seleccionada', async () => {
    const onValueChange = vi.fn()
    renderSingle(onValueChange)
    const input = screen.getByRole('combobox', { name: 'Buscar programa' })
    await userEvent.type(input, 'vivi')

    expect(await screen.findByRole('option', { name: 'Vivienda' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Alimentación' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('option', { name: 'Vivienda' }))
    expect(onValueChange).toHaveBeenCalledWith('Vivienda', expect.anything())
  })

  it('muestra el mensaje vacío y permite limpiar el valor seleccionado', async () => {
    const onValueChange = vi.fn()
    renderSingle(onValueChange)
    const input = screen.getByRole('combobox', { name: 'Buscar programa' })
    await userEvent.type(input, 'sin coincidencia')
    expect(await screen.findByText('Sin resultados')).toBeInTheDocument()

    await userEvent.clear(input)
    await userEvent.type(input, 'Alim')
    await userEvent.click(await screen.findByRole('option', { name: 'Alimentación' }))
    await waitFor(() => expect(onValueChange).toHaveBeenCalledWith(null, expect.anything()))
  })

  it('mantiene el campo deshabilitado e impide desplegar opciones', async () => {
    renderSingle(vi.fn(), true)
    const input = screen.getByRole('combobox', { name: 'Buscar programa' })
    expect(input).toBeDisabled()
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('expone el valor presentado y los chips seleccionados', () => {
    render(
      <Combobox multiple value={['Vivienda', 'Alimentación']} items={options}>
        <ComboboxValue />
        <ComboboxChips>
          <ComboboxChip>Vivienda</ComboboxChip>
          <ComboboxChip showRemove={false}>Alimentación</ComboboxChip>
          <ComboboxInput aria-label="Agregar programa" showTrigger={false} />
        </ComboboxChips>
        <ComboboxContent><ComboboxList>{(item: string) => <ComboboxItem key={item} value={item}>{item}</ComboboxItem>}</ComboboxList></ComboboxContent>
      </Combobox>,
    )

    expect(screen.getByText('Vivienda')).toBeInTheDocument()
    expect(screen.getByText('Alimentación')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Agregar programa' })).toBeInTheDocument()
  })
})

function renderSingle(onValueChange: (value: string | null) => void, disabled = false) {
  return render(<SingleSelect onValueChange={onValueChange} disabled={disabled} />)
}
