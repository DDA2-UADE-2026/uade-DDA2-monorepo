import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { DeadLettersChart, HourlyVolumeChart, ReceivedByStatusChart, TopPublishedTypesChart } from './CoreEventCharts'

beforeEach(() => vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} }))
afterEach(() => vi.unstubAllGlobals())

it('calcula aceptados por hora sin generar valores negativos ante datos inconsistentes', async () => {
  render(<HourlyVolumeChart data={{ windowHours: 48, volumeByHour: [{ hour: '2026-11-13T12:00:00Z', total: 10, rejected: 3 }, { hour: 'sin-fecha', total: 2, rejected: 4 }] }} />)
  expect(screen.getByText(/últimas 48 h/)).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  const rows = screen.getAllByRole('row')
  expect(within(rows[1]).getAllByRole('cell').slice(1).map(cell => cell.textContent)).toEqual(['10', '7', '3'])
  expect(within(rows[2]).getAllByRole('cell').map(cell => cell.textContent)).toEqual(['sin-fecha', '2', '0', '4'])
})

it('ordena estados recibidos por cantidad y traduce sus etiquetas', async () => {
  render(<ReceivedByStatusChart data={{ received: { byStatus: { DEAD: 2, DELIVERED: 10, PENDING_RETRY: 5 } } }} />)
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getAllByRole('row').slice(1).map(row => row.textContent)).toEqual(['Entregado10', 'Reintento pendiente5', 'Fallido (DLQ)2'])
})

it('relaciona tipos publicados y motivos de mensajes fallidos con sus cantidades', async () => {
  const view = render(<TopPublishedTypesChart data={{ published: { topTypes: [{ eventType: 'solicitud.aprobada', total: 15 }] } }} />)
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getByRole('row', { name: 'solicitud.aprobada 15' })).toBeInTheDocument()
  view.rerender(<DeadLettersChart data={{ deadLetters: { open: 7, byReason: { INVALID_SCHEMA: 2, RETRY_EXHAUSTED: 5 } } }} />)
  expect(screen.getByText('7 abiertas que agotaron sus reintentos.')).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getAllByRole('row').slice(1).map(row => row.textContent)).toEqual(['RETRY_EXHAUSTED5', 'INVALID_SCHEMA2'])
})

it.each([
  { Component: HourlyVolumeChart, message: 'Sin eventos en la ventana elegida.' },
  { Component: ReceivedByStatusChart, message: 'Todavía no recibimos eventos.' },
  { Component: TopPublishedTypesChart, message: 'Todavía no publicamos eventos.' },
  { Component: DeadLettersChart, message: 'No hay dead letters abiertas.' },
])('explica la ausencia de datos: $message', ({ Component, message }) => {
  render(<Component data={{}} />)
  expect(screen.getByText(message)).toBeInTheDocument()
})
