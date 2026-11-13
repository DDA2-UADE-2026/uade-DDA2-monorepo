import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ErrorResponse } from '@/generated/types.gen'
import {
  formatInterventionDate,
  formatInterventionDateTime,
  InterventionError,
  InterventionHeading,
  InterventionLoading,
  InterventionProgress,
  InterventionProgressSummary,
  InterventionStatusBadge,
  todayLocal,
} from './InterventionUi'

describe('InterventionUi', () => {
  it.each([
    ['PENDING', 'Pendiente'],
    ['IN_PROGRESS', 'En curso'],
    ['COMPLETED', 'Completada'],
  ] as const)('presenta el estado %s con su etiqueta', (status, label) => {
    render(<InterventionStatusBadge status={status} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })

  it('explica un estado ausente sin inventar una resolución', () => {
    render(<InterventionStatusBadge />)
    expect(screen.getByText('Sin estado')).toBeInTheDocument()
  })

  it('presenta título, descripción y una acción opcional', () => {
    render(<InterventionHeading title="Plan de acompañamiento" description="Seguimiento del caso" action={<button>Editar</button>} />)
    expect(screen.getByRole('heading', { name: 'Plan de acompañamiento' })).toBeInTheDocument()
    expect(screen.getByText('Seguimiento del caso')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Editar' })).toBeInTheDocument()
  })

  it('admite un encabezado sin descripción ni acción', () => {
    const { container } = render(<InterventionHeading title="Intervención" />)
    expect(screen.getByRole('heading', { name: 'Intervención' })).toBeInTheDocument()
    expect(container.querySelector('p')).toBeNull()
  })

  it('muestra el error, sus campos y permite reintentar', () => {
    const retry = vi.fn()
    const error: ErrorResponse = {
      status: 422,
      message: 'No se pudo guardar el plan',
      fields: [{ field: 'fecha', message: 'La fecha es obligatoria' }],
    }
    render(<InterventionError error={error} title="Error del plan" retry={retry} />)
    expect(screen.getByText('Error del plan')).toBeInTheDocument()
    expect(screen.getByText('No se pudo guardar el plan')).toBeInTheDocument()
    expect(screen.getByText('La fecha es obligatoria')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(retry).toHaveBeenCalledOnce()
  })

  it('usa un mensaje genérico cuando el servidor no devuelve detalle', () => {
    render(<InterventionError error={{ status: 500 }} title="Error del plan" />)
    expect(screen.getByText('Intentá nuevamente en unos instantes.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('permite cambiar la etiqueta del estado de carga', () => {
    render(<InterventionLoading label="Cargando acciones" />)
    expect(screen.getByRole('status', { name: 'Cargando acciones' })).toBeInTheDocument()
    expect(screen.getByText('Cargando acciones…')).toBeInTheDocument()
  })

  it('cuenta solo las acciones completadas y presenta el porcentaje', () => {
    render(<InterventionProgress statuses={['COMPLETED', 'PENDING', 'COMPLETED', undefined]} />)
    expect(screen.getByText('Progreso')).toBeInTheDocument()
    expect(screen.getByText('2 de 4 acciones')).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '50')
  })

  it.each([
    [-2, 4, 0],
    [8, 4, 100],
    [0, 0, 0],
    [1, 3, 33],
  ])('limita el progreso a un total válido (%s de %s)', (completed, total, percent) => {
    render(<InterventionProgressSummary completed={completed} total={total} compact />)
    expect(screen.getByText(`${percent}%`)).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', String(percent))
  })

  it('formatea fechas, acepta entradas no analizables y usa guion para valores vacíos', () => {
    expect(formatInterventionDate('2026-11-13')).not.toBe('—')
    expect(formatInterventionDate('no-es-fecha')).toBe('no-es-fecha')
    expect(formatInterventionDate(null)).toBe('—')
    expect(formatInterventionDateTime('2026-11-13T12:30:00Z')).not.toBe('—')
    expect(formatInterventionDateTime('')).toBe('—')
    expect(todayLocal()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
