import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiHarness } from '@/test/api-harness'
import type { ProgramListItemResponse } from '@/generated/types.gen'
import { ProgramPickerDialog } from './ProgramPickerDialog'

let api: ReturnType<typeof apiHarness>
let failFirstPage: boolean
let selected: Set<string>
const toggled = vi.fn()
const program = (id: string, name: string, objective?: string): ProgramListItemResponse => ({ id, name, objective })

beforeEach(() => {
  failFirstPage = false
  selected = new Set(['program-1'])
  toggled.mockReset()
  api = apiHarness((request) => {
    const url = new URL(request.url)
    if (url.pathname !== '/api/admin/programs') throw new Error(`Unexpected request ${request.method} ${url.pathname}`)
    const page = Number(url.searchParams.get('page') ?? 0)
    if (page === 0 && failFirstPage) {
      return Response.json({ status: 503, message: 'Programas no disponibles' }, { status: 503 })
    }
    const content = page === 0
      ? [program('program-1', 'Apoyo alimentario', 'Acceso a alimentos'), program('program-2', 'Salud comunitaria'), program('excluded', 'Programa excluido'), program('', 'Sin identificador')]
      : [program('program-2', 'Acompañamiento'), program('program-3', 'Salud comunitaria')]
    return Response.json({ content, totalElements: 12, totalPages: 2, number: page, size: 10 })
  })
})

afterEach(() => api.restore())

function renderPicker(overrides: Partial<React.ComponentProps<typeof ProgramPickerDialog>> = {}) {
  api.render(
    <ProgramPickerDialog
      onOpenChange={vi.fn()}
      selectedIds={selected}
      excludeIds={new Set(['excluded'])}
      onToggle={toggled}
      {...overrides}
    />,
  )
}

describe('ProgramPickerDialog', () => {
  it('filtra IDs excluidos y faltantes y refleja la selección y el objetivo', async () => {
    renderPicker()

    await waitFor(() => expect(api.requests.length).toBeGreaterThan(0))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('Apoyo alimentario')).toBeInTheDocument()
    expect(within(dialog).getByText('Acceso a alimentos')).toBeInTheDocument()
    expect(within(dialog).queryByText('Programa excluido')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('Sin identificador')).not.toBeInTheDocument()
    expect(within(dialog).getByRole('checkbox', { name: 'Marcar Apoyo alimentario como incompatible' })).toBeChecked()
  })

  it('impide modificar el checkbox del programa que está pendiente', async () => {
    renderPicker({ pendingId: 'program-1' })

    const dialog = await screen.findByRole('dialog')
    const selectedCheckbox = await within(dialog).findByRole('checkbox', { name: 'Marcar Apoyo alimentario como incompatible' })
    expect(selectedCheckbox).toHaveAttribute('aria-disabled', 'true')
  })

  it('envía al callback el programa y el nuevo valor al marcar y desmarcar', async () => {
    renderPicker()

    const dialog = await screen.findByRole('dialog')
    const selectedCheckbox = await within(dialog).findByRole('checkbox', { name: 'Marcar Apoyo alimentario como incompatible' })
    await userEvent.click(selectedCheckbox)
    expect(toggled).toHaveBeenCalledWith(expect.objectContaining({ id: 'program-1', name: 'Apoyo alimentario' }), false)

    const unselectedCheckbox = within(dialog).getByRole('checkbox', { name: 'Marcar Salud comunitaria como incompatible' })
    await userEvent.click(unselectedCheckbox)
    expect(toggled).toHaveBeenCalledWith(expect.objectContaining({ id: 'program-2', name: 'Salud comunitaria' }), true)
  })

  it('muestra el estado vacío cuando todos los programas están excluidos', async () => {
    renderPicker({ excludeIds: new Set(['program-1', 'program-2', 'excluded']) })

    expect(await screen.findByText('No hay programas disponibles.')).toBeInTheDocument()
  })

  it('recupera un error de carga al reintentar', async () => {
    failFirstPage = true
    renderPicker()
    expect(await screen.findByText('No se pudieron cargar los datos.')).toBeInTheDocument()

    failFirstPage = false
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('Apoyo alimentario')).toBeInTheDocument()
    expect(api.requests.filter((request) => new URL(request.url).pathname === '/api/admin/programs')).toHaveLength(2)
  })

  it('cambia la página y solicita la segunda página al paginador', async () => {
    renderPicker()

    expect(await screen.findByText('Apoyo alimentario')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Siguiente'))
    expect(await screen.findByText('Acompañamiento')).toBeInTheDocument()
    await waitFor(() => expect(api.requests.some((request) => new URL(request.url).searchParams.get('page') === '1')).toBe(true))
  })
})
