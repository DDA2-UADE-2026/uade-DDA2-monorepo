import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { expect, it, vi } from 'vitest'
import { AttentionPanel, CapacityMeters, EvaluationHeatmap } from './IndicatorPanels'

vi.mock('@tanstack/react-router', async importOriginal => ({
  ...await importOriginal<typeof import('@tanstack/react-router')>(),
  Link: ({ children, params }: { children?: ReactNode; params: { solicitudId: string } }) => <a href={`/gestion/casos/${params.solicitudId}/evaluacion`}>{children}</a>,
}))

it('expone ocupación y alertas de cupos en texto además del color', () => {
  render(<CapacityMeters data={{ capacity: [
    { editionId: 'e1', programName: 'A', editionName: 'Uno', status: 'ACTIVE', approved: 10, maxCapacity: 10, waitlisted: 3 },
    { editionId: 'e2', programName: 'B', editionName: 'Dos', status: 'SUSPENDED', approved: 8, maxCapacity: 10 },
    { editionId: 'e3', programName: 'C', editionName: 'Tres', status: 'ACTIVE', approved: 2, maxCapacity: 10 },
  ] }} />)
  expect(screen.getByRole('meter', { name: 'Ocupación de A Uno' })).toHaveAttribute('aria-valuetext', '10 de 10 aprobadas')
  expect(screen.getByRole('meter', { name: 'Ocupación de B Dos' })).toHaveAttribute('aria-valuenow', '8')
  expect(screen.getByText('Cupo completo')).toBeInTheDocument()
  expect(screen.getByText('Casi completo')).toBeInTheDocument()
  expect(screen.getByText('3 en lista de espera')).toBeInTheDocument()
  expect(screen.getByText('Suspendida')).toBeInTheDocument()
})

it('limita las ediciones visibles y permite expandirlas y volver a contraerlas', async () => {
  render(<CapacityMeters data={{ capacity: Array.from({ length: 10 }, (_, index) => ({ editionId: `e-${index}`, programName: `Programa ${index}`, editionName: 'Anual', approved: index, maxCapacity: 20 })) }} />)
  expect(screen.getAllByRole('meter')).toHaveLength(8)
  await userEvent.click(screen.getByRole('button', { name: 'Ver las 10 ediciones' }))
  expect(screen.getAllByRole('meter')).toHaveLength(10)
  await userEvent.click(screen.getByRole('button', { name: 'Ver menos' }))
  expect(screen.getAllByRole('meter')).toHaveLength(8)
})

it('calcula la ocupación en la tabla y usa ausencia de porcentaje cuando no hay cupo definido', async () => {
  render(<CapacityMeters data={{ capacity: [{ editionId: 'e1', programName: 'Alimentos', editionName: 'Anual', status: 'ACTIVE', approved: 8, maxCapacity: 10 }, { editionId: 'e2', programName: 'Sin cupo' }] }} />)
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  const rows = screen.getAllByRole('row')
  expect(within(rows[1]).getAllByRole('cell').map(cell => cell.textContent)).toEqual(['Alimentos', 'Anual', 'Activa', '8', '10', '80%', '0'])
  expect(within(rows[2]).getAllByRole('cell').map(cell => cell.textContent)).toEqual(['Sin cupo', '', '', '0', '0', '—', '0'])
})

it('explica un tablero sin cupos ni evaluaciones', () => {
  render(<><CapacityMeters data={{}} /><EvaluationHeatmap data={{}} /></>)
  expect(screen.getByText('No hay ediciones publicadas.')).toBeInTheDocument()
  expect(screen.getByText('No se registraron evaluaciones en el período.')).toBeInTheDocument()
})

it('mantiene los 16 cruces accesibles del mapa de evaluación y representa ausencias como cero', async () => {
  render(<EvaluationHeatmap data={{ evaluationMatrix: [{ vulnerability: 'CRITICAL', priority: 'URGENT', count: 7 }, { vulnerability: 'LOW', priority: 'LOW', count: 2 }] }} />)
  expect(screen.getAllByRole('cell')).toHaveLength(16)
  expect(screen.getByRole('cell', { name: 'Vulnerabilidad crítica, prioridad urgente: 7 evaluaciones' })).toHaveTextContent('7')
  expect(screen.getByRole('cell', { name: 'Vulnerabilidad media, prioridad alta: 0 evaluaciones' })).toHaveTextContent('0')
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getByRole('row', { name: 'Crítica 0 0 0 7' })).toBeInTheDocument()
  expect(screen.getByRole('row', { name: 'Baja 2 0 0 0' })).toBeInTheDocument()
})

it('relaciona alertas de demora con la solicitud que debe evaluar el profesional', async () => {
  render(<AttentionPanel data={{ attention: { unassigned: 3, awaitingEvaluation: 1, staleAfterDays: 20, oldestAwaitingEvaluation: [{ applicationId: 'app-42', applicationNumber: 42, programName: 'Alimentos', status: 'IN_EVALUATION', submittedAt: '2026-10-01T12:00:00Z', daysWaiting: 35 }] } }} />)
  expect(screen.getByText('Sin evaluar hace más de 20 días')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: /N.º 42/ })).toHaveAttribute('href', '/gestion/casos/app-42/evaluacion')
  expect(screen.getByText(/35 días/)).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', { name: 'Ver tabla' }))
  expect(screen.getByRole('row', { name: /N.º 42 Alimentos.*35/ })).toBeInTheDocument()
})
