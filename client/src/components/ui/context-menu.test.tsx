import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { render } from '@testing-library/react'
import {
  ContextMenu,
  ContextMenuCheckboxItem,
  ContextMenuContent,
  ContextMenuGroup,
  ContextMenuItem,
  ContextMenuLabel,
  ContextMenuRadioGroup,
  ContextMenuRadioItem,
  ContextMenuSeparator,
  ContextMenuShortcut,
  ContextMenuSub,
  ContextMenuSubContent,
  ContextMenuSubTrigger,
  ContextMenuTrigger,
} from './context-menu'

function MenuFixture({ onEdit = vi.fn() }: { onEdit?: () => void }) {
  const [checked, setChecked] = useState(false)
  const [view, setView] = useState('cards')
  return (
    <ContextMenu>
      <ContextMenuTrigger><div data-testid="target">Abrir acciones</div></ContextMenuTrigger>
      <ContextMenuContent>
        <ContextMenuGroup>
          <ContextMenuLabel inset>Programa</ContextMenuLabel>
          <ContextMenuItem inset onClick={onEdit}>Editar<ContextMenuShortcut>Ctrl+E</ContextMenuShortcut></ContextMenuItem>
          <ContextMenuItem variant="destructive">Eliminar</ContextMenuItem>
          <ContextMenuSeparator />
          <ContextMenuCheckboxItem checked={checked} onCheckedChange={(value) => setChecked(value)}>
            Fijar arriba
          </ContextMenuCheckboxItem>
          <ContextMenuRadioGroup value={view} onValueChange={(value) => setView(value)}>
            <ContextMenuRadioItem value="cards">Tarjetas</ContextMenuRadioItem>
            <ContextMenuRadioItem value="table">Tabla</ContextMenuRadioItem>
          </ContextMenuRadioGroup>
          <ContextMenuSub>
            <ContextMenuSubTrigger>Más acciones</ContextMenuSubTrigger>
            <ContextMenuSubContent>
              <ContextMenuItem>Duplicar</ContextMenuItem>
            </ContextMenuSubContent>
          </ContextMenuSub>
        </ContextMenuGroup>
      </ContextMenuContent>
    </ContextMenu>
  )
}

async function openMenu() {
  const trigger = screen.getByTestId('target')
  fireEvent.contextMenu(trigger, { clientX: 24, clientY: 30 })
  return screen.findByRole('menu')
}

describe('ContextMenu', () => {
  it('abre con el menú contextual y presenta las acciones agrupadas', async () => {
    render(<MenuFixture />)
    const menu = await openMenu()

    expect(within(menu).getByText('Programa')).toBeInTheDocument()
    expect(within(menu).getByRole('menuitem', { name: /Editar/ })).toBeInTheDocument()
    expect(within(menu).getByRole('menuitem', { name: 'Eliminar' })).toHaveAttribute('data-variant', 'destructive')
    expect(within(menu).getByText('Ctrl+E')).toBeInTheDocument()
    expect(menu.querySelector('[data-slot="context-menu-separator"]')).toBeInTheDocument()
  })

  it('ejecuta la acción elegida y cierra el menú', async () => {
    const onEdit = vi.fn()
    render(<MenuFixture onEdit={onEdit} />)
    const menu = await openMenu()
    await userEvent.click(within(menu).getByRole('menuitem', { name: /Editar/ }))

    expect(onEdit).toHaveBeenCalledOnce()
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('presenta opciones checkbox y radio con el rol y el estado accesible', async () => {
    render(<MenuFixture />)
    const menu = await openMenu()

    const checkbox = within(menu).getByRole('menuitemcheckbox', { name: 'Fijar arriba' })
    expect(checkbox).toHaveAttribute('aria-checked', 'false')
    await userEvent.click(checkbox)
    expect(within(await screen.findByRole('menu')).getByRole('menuitemcheckbox', { name: 'Fijar arriba' })).toHaveAttribute('aria-checked', 'true')
  })

  it('abre un submenú y permite activar una acción anidada', async () => {
    render(<MenuFixture />)
    const menu = await openMenu()
    await userEvent.hover(within(menu).getByRole('menuitem', { name: 'Más acciones' }))

    const duplicate = await screen.findByRole('menuitem', { name: 'Duplicar' })
    expect(duplicate).toBeInTheDocument()
    await userEvent.click(duplicate)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })
})
