import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import type { Indicators } from '@/lib/indicators'
import { ApplicationsTrendChart, AppointmentsTrendChart, OutcomesByProgramChart, ResolutionsDonut, StatusDistributionChart, VisitsTrendChart, WorkloadChart } from './IndicatorCharts'

beforeEach(() => vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} }))
afterEach(() => vi.unstubAllGlobals())
const cases = [
  { title: 'Solicitudes', Component: ApplicationsTrendChart, data: { bucket: 'MONTH', applicationsTrend: [{ periodStart: '2026-11-01', submitted: 20, resolved: 12 }] }, cells: ['noviembre de 2026', '20', '12'] },
  { title: 'Estados', Component: StatusDistributionChart, data: { statusDistribution: [{ status: 'APPROVED', count: 7 }, { count: 99 }] }, cells: ['Aprobada', '7'] },
  { title: 'Resultados por programa', Component: OutcomesByProgramChart, data: { programs: [{ programName: 'Alimentos', submitted: 10, inProgress: 2, waitlisted: 1, approved: 6, rejected: 1 }] }, cells: ['Alimentos', '10', '2', '1', '6', '1', '60%'] },
  { title: 'Resoluciones', Component: ResolutionsDonut, data: { resolutions: { approved: 6, waitlisted: 2, rejected: 2 } }, cells: ['Aprobadas', '6', '60%'] },
  { title: 'Visitas', Component: VisitsTrendChart, data: { bucket: 'MONTH', visitsTrend: [{ periodStart: '2026-11-01', realized: 8, notRealized: 1, cancelled: 2, scheduled: 3 }] }, cells: ['noviembre de 2026', '8', '1', '2', '3'] },
  { title: 'Carga de trabajo', Component: WorkloadChart, data: { workload: [{ name: 'Ana', activeCases: 5, scheduledVisits: 2 }, {}] }, cells: ['Ana', '5', '2'] },
  { title: 'Turnos', Component: AppointmentsTrendChart, data: { bucket: 'MONTH', appointments: { trend: [{ periodStart: '2026-11-01', confirmed: 10, cancelled: 2 }] } }, cells: ['noviembre de 2026', '10', '2'] },
] satisfies Array<{ title: string; Component: typeof ApplicationsTrendChart; data: Indicators; cells: string[] }>

it.each(cases)('$title conserva fechas, cantidades y porcentajes al cambiar a tabla', async ({ Component, data, cells }) => {
  render(<Component data={data} />)
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  const table = screen.getByRole('table')
  const first = within(table).getAllByRole('row')[1]
  expect(within(first).getAllByRole('cell').map(cell => cell.textContent)).toEqual(cells)
  if (Component === StatusDistributionChart) expect(within(table).getAllByRole('row')).toHaveLength(2)
  if (Component === WorkloadChart) expect(within(table).getByRole('row', { name: 'Sin nombre 0 0' })).toBeInTheDocument()
})

it.each(cases)('$title informa ausencia de datos sin romper la alternativa de tabla', async ({ Component }) => {
  render(<Component data={{}} />)
  expect(screen.getByText(/No se resolvieron|No hay casos activos|Sin datos para/)).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  // Resoluciones conserva las tres categorías y sus ceros aunque no existan decisiones.
  if (Component === ResolutionsDonut) expect(screen.getByRole('row', { name: 'Aprobadas 0 —' })).toBeInTheDocument()
  else expect(screen.queryByRole('table')).not.toBeInTheDocument()
})

it('mantiene en la tabla todos los programas aunque el gráfico muestre solo los primeros ocho', async () => {
  render(<OutcomesByProgramChart data={{ programs: Array.from({ length: 10 }, (_, index) => ({ programName: `Programa ${index}`, submitted: 2, approved: 1 })) }} />)
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getAllByRole('row')).toHaveLength(11)
  expect(screen.getByRole('row', { name: 'Programa 9 2 0 0 1 0 50%' })).toBeInTheDocument()
})
