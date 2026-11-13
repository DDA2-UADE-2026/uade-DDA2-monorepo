import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from './drawer'

function DrawerFixture({ modal = true, direction = 'down' as const, showSwipeHandle = false, snapPoints }: {
  modal?: boolean
  direction?: 'up' | 'down' | 'left' | 'right'
  showSwipeHandle?: boolean
  snapPoints?: (number | string)[]
}) {
  return (
    <Drawer modal={modal} swipeDirection={direction} showSwipeHandle={showSwipeHandle} snapPoints={snapPoints}>
      <DrawerTrigger>Abrir detalle</DrawerTrigger>
      <DrawerContent>
        <DrawerHeader>
          <DrawerTitle>Detalle de la actividad</DrawerTitle>
          <DrawerDescription>Información del encuentro barrial.</DrawerDescription>
        </DrawerHeader>
        <div>Contenido del panel</div>
        <DrawerFooter><DrawerClose>Cerrar panel</DrawerClose></DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

describe('Drawer', () => {
  it('abre el panel modal y lo cierra con Escape', async () => {
    render(<DrawerFixture />)
    const trigger = screen.getByRole('button', { name: 'Abrir detalle' })
    await userEvent.click(trigger)

    expect(await screen.findByRole('dialog', { name: 'Detalle de la actividad' })).toBeInTheDocument()
    expect(screen.getByText('Información del encuentro barrial.')).toBeInTheDocument()
    expect(document.querySelector('[data-slot="drawer-overlay"]')).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Detalle de la actividad' })).not.toBeInTheDocument()
  })

  it('cierra explícitamente y devuelve el foco al disparador', async () => {
    render(<DrawerFixture />)
    const trigger = screen.getByRole('button', { name: 'Abrir detalle' })
    await userEvent.click(trigger)
    await userEvent.click(await screen.findByRole('button', { name: 'Cerrar panel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  it('respeta el modo no modal y la dirección horizontal', async () => {
    render(<DrawerFixture modal={false} direction="left" />)
    await userEvent.click(screen.getByRole('button', { name: 'Abrir detalle' }))
    const panel = await screen.findByRole('dialog', { name: 'Detalle de la actividad' })

    expect(panel.closest('[data-slot="drawer-popup"]')).toHaveAttribute('data-swipe-axis', 'x')
    expect(panel.closest('[data-slot="drawer-viewport"]')).toHaveAttribute('data-modal', 'false')
    expect(document.querySelector('[data-slot="drawer-overlay"]')).not.toBeInTheDocument()
  })

  it('incluye la manija y marca el uso de puntos de ajuste', async () => {
    render(<DrawerFixture showSwipeHandle snapPoints={['160px', 1]} />)
    await userEvent.click(screen.getByRole('button', { name: 'Abrir detalle' }))
    const panel = await screen.findByRole('dialog', { name: 'Detalle de la actividad' })
    const popup = panel.closest('[data-slot="drawer-popup"]')

    expect(popup).toHaveAttribute('data-swipe-axis', 'y')
    expect(popup).toHaveAttribute('data-snap-points')
    expect(popup?.querySelector('[data-slot="drawer-swipe-handle"]')).toHaveAttribute('aria-hidden', 'true')
    expect(document.querySelector('[data-slot="drawer-overlay"]')).toHaveAttribute('data-snap-points')
  })
})
