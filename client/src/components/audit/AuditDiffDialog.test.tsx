import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it } from 'vitest'
import { AuditDiffDialog } from './AuditDiffDialog'

async function open(oldValues?: string, newValues?: string) {
  render(<AuditDiffDialog log={{ entityId: 'app-42', oldValues, newValues }} />)
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Ver diff' }))
  return screen.getByRole('dialog')
}
it('identifica valores cambiados, agregados y eliminados conservando el contexto sin cambios', async () => {
  const dialog = await open(JSON.stringify({ name: 'Antes', stable: 'Igual', removed: 10, nested: { count: 1 } }), JSON.stringify({ name: 'Después', stable: 'Igual', added: true, nested: { count: 2 } }))
  expect(dialog).toHaveTextContent('app-42')
  expect(screen.getByText('name: "Antes",')).toHaveClass('line-through')
  expect(screen.getByText('name: "Después",')).not.toHaveClass('line-through')
  expect(screen.getByText('removed: 10,')).toHaveClass('line-through')
  expect(screen.getByText('added: true,')).not.toHaveClass('line-through')
  expect(screen.getByText('stable: "Igual",')).toHaveClass('text-muted-foreground')
  expect(screen.getByText('count: 1,')).toHaveClass('line-through')
  expect(screen.getByText('count: 2,')).not.toHaveClass('line-through')
})
it.each([{ before: undefined, after: undefined }, { before: '{"a":1}', after: '{"a":1}' }])('informa ausencia de diferencias sin inventar cambios: %j', async ({ before, after }) => {
  expect(await open(before, after)).toHaveTextContent('No hay diferencias entre los valores registrados.')
})
it('tolera registros históricos que no contienen JSON válido', async () => {
  const dialog = await open('Texto anterior', 'Texto posterior')
  expect(dialog).toHaveTextContent('"Texto anterior"')
  expect(dialog).toHaveTextContent('"Texto posterior"')
  expect(screen.getByText('"Texto anterior"')).toHaveClass('line-through')
})
it('presenta la sustitución completa de arrays sin perder sus elementos', async () => {
  const dialog = await open('{"roles":["ADMIN","USER"]}', '{"roles":["USER","AUDITOR"]}')
  expect(dialog).toHaveTextContent('"ADMIN"')
  expect(dialog).toHaveTextContent('"AUDITOR"')
  expect(dialog.querySelectorAll('pre .line-through').length).toBeGreaterThan(0)
})
it('presenta altas y bajas de una entidad completa', async () => {
  expect(await open(undefined, '{"created":true}')).toHaveTextContent('"created": true')
  expect(screen.getByRole('dialog').querySelector('.line-through')).toBeNull()
})
