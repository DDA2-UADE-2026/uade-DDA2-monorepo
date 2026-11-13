import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it } from 'vitest'
import { ChartCard, StatTile } from './IndicatorUi'

it('ofrece una tabla accesible con los mismos datos y permite volver al gráfico', async () => {
  render(<ChartCard title="Solicitudes por mes" description="Último trimestre" table={{ columns: ['Mes', 'Cantidad'], rows: [['Enero', 12], ['Febrero', 20]] }} footer={<p>Fuente: solicitudes</p>}><div role="img" aria-label="Gráfico mensual" /></ChartCard>)
  expect(screen.getByRole('img', { name: 'Gráfico mensual' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Ver tabla' })).toHaveAttribute('aria-pressed', 'false')
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  const table = screen.getByRole('table')
  expect(within(table).getAllByRole('columnheader').map(cell => cell.textContent)).toEqual(['Mes', 'Cantidad'])
  expect(within(table).getByRole('row', { name: 'Enero 12' })).toBeInTheDocument()
  expect(within(table).getByRole('row', { name: 'Febrero 20' })).toBeInTheDocument()
  expect(screen.queryByRole('img')).not.toBeInTheDocument()
  expect(screen.getByText('Fuente: solicitudes')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Ver gráfico' })).toHaveAttribute('aria-pressed', 'true')
  await userEvent.click(screen.getByRole('button', { name: 'Ver gráfico' }))
  expect(screen.getByRole('img', { name: 'Gráfico mensual' })).toBeInTheDocument()
})

it('explica la ausencia de registros cuando se consulta la tabla de un período vacío', async () => {
  render(<ChartCard title="Visitas" table={{ columns: ['Fecha'], rows: [] }}><div role="img" aria-label="Visitas" /></ChartCard>)
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getByText('Sin datos para el período elegido.')).toBeInTheDocument()
  expect(screen.queryByRole('table')).not.toBeInTheDocument()
})

it.each([
  { change: .125, upIsGood: true, text: '+13%', color: 'text-emerald-700' },
  { change: .125, upIsGood: false, text: '+13%', color: 'text-red-700' },
  { change: -.2, upIsGood: true, text: '-20%', color: 'text-red-700' },
  { change: -.2, upIsGood: false, text: '-20%', color: 'text-emerald-700' },
  { change: 0, upIsGood: true, text: '0%', color: 'text-muted-foreground' },
])('muestra la variación $text con el sentido favorable configurado: $upIsGood', ({ change, upIsGood, text, color }) => {
  render(<StatTile label="Tiempo medio" value="12 días" hint="Desde la presentación" change={change} upIsGood={upIsGood} />)
  expect(screen.getByText('Tiempo medio')).toBeInTheDocument()
  expect(screen.getByText('12 días')).toBeInTheDocument()
  expect(screen.getByText(`${text} vs. período anterior`)).toHaveClass(color)
  expect(screen.getByText('Desde la presentación')).toBeInTheDocument()
})

it('no inventa una variación cuando no hay período de comparación', () => {
  render(<StatTile label="Total" value="0" change={null} />)
  expect(screen.queryByText(/vs. período anterior/)).not.toBeInTheDocument()
})
