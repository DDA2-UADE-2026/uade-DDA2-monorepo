import type { ComponentProps } from 'react'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  ChartContainer,
  ChartLegendContent,
  ChartTooltipContent,
  type ChartConfig,
} from './chart'

type TooltipProps = ComponentProps<typeof ChartTooltipContent>
const payload = (...items: object[]) => items as NonNullable<TooltipProps['payload']>
const legend = (...items: object[]) => items as NonNullable<ComponentProps<typeof ChartLegendContent>['payload']>

const config: ChartConfig = {
  applications: { label: 'Solicitudes', color: '#2563eb' },
  approved: { label: 'Aprobadas', theme: { light: '#15803d', dark: '#4ade80' } },
  rejected: { label: 'Rechazadas', color: '#dc2626' },
  month: { label: 'Período' },
}

describe('Chart components', () => {
  it('genera una identidad estable y variables CSS según los temas configurados', () => {
    const { container } = render(
      <ChartContainer id="dashboard" config={config}>
        <div>Gráfico de solicitudes</div>
      </ChartContainer>,
    )
    const chart = container.querySelector('[data-slot="chart"]')!
    expect(chart).toHaveAttribute('data-chart', 'chart-dashboard')
    const styles = container.querySelector('style')!
    expect(styles.textContent).toContain('--color-applications: #2563eb')
    expect(styles.textContent).toContain('.dark [data-chart=chart-dashboard]')
    expect(styles.textContent).toContain('--color-approved: #4ade80')
  })

  it('omite reglas CSS cuando no se configuraron colores', () => {
    const { container } = render(
      <ChartContainer config={{ applications: { label: 'Solicitudes' } }}>
        <div>Gráfico</div>
      </ChartContainer>,
    )
    expect(container.querySelector('[data-slot="chart"]')).toHaveAttribute('data-chart', expect.stringMatching(/^chart-/))
    expect(container.querySelector('style')).not.toBeInTheDocument()
  })

  it('no presenta tooltip cerrado ni tooltip sin datos', () => {
    const { rerender } = render(
      <ChartContainer config={config}><ChartTooltipContent active={false} payload={payload({ name: 'applications', value: 2 })} /></ChartContainer>,
    )
    expect(screen.queryByText('Solicitudes')).not.toBeInTheDocument()

    rerender(<ChartContainer config={config}><ChartTooltipContent active payload={[]} /></ChartContainer>)
    expect(screen.queryByText('Solicitudes')).not.toBeInTheDocument()
  })

  it('muestra la etiqueta traducida, el valor numérico formateado y el color del punto', () => {
    render(
      <ChartContainer config={config}>
        <ChartTooltipContent
          active
          label="applications"
          payload={payload({ name: 'applications', dataKey: 'applications', value: 1234, color: '#2563eb', type: 'bar' })}
        />
      </ChartContainer>,
    )
    expect(screen.getAllByText('Solicitudes')).toHaveLength(2)
    expect(screen.getByText((1234).toLocaleString())).toBeInTheDocument()
    expect(document.querySelector('[style*="--color-bg"]')).toHaveStyle({ '--color-bg': '#2563eb' })
  })

  it('usa el labelKey del registro interno y permite formatear el encabezado', () => {
    const labelFormatter = vi.fn((label) => `Mes: ${String(label)}`)
    render(
      <ChartContainer config={config}>
        <ChartTooltipContent
          active
          label="raw"
          labelKey="month"
          labelFormatter={labelFormatter}
          payload={payload({ name: 'approved', dataKey: 'approved', value: 5, payload: { month: 'november' } })}
        />
      </ChartContainer>,
    )
    expect(screen.getByText('Mes: Período')).toBeInTheDocument()
    expect(labelFormatter).toHaveBeenCalledWith('Período', expect.any(Array))
    expect(screen.getByText('Aprobadas')).toBeInTheDocument()
  })

  it('oculta encabezado e indicador cuando se solicita y filtra series none', () => {
    render(
      <ChartContainer config={config}>
        <ChartTooltipContent
          active
          hideLabel
          hideIndicator
          payload={payload(
            { name: 'approved', dataKey: 'approved', value: 2, type: 'bar' },
            { name: 'ignored', dataKey: 'rejected', value: 99, type: 'none' },
          )}
        />
      </ChartContainer>,
    )
    expect(screen.getByText('Aprobadas')).toBeInTheDocument()
    expect(screen.queryByText('Rechazadas')).not.toBeInTheDocument()
    expect(screen.queryByText('ignored')).not.toBeInTheDocument()
  })

  it.each(['dot', 'line', 'dashed'] as const)('representa la variante de indicador %s', (indicator) => {
    const { container } = render(
      <ChartContainer config={config}>
        <ChartTooltipContent active indicator={indicator} payload={payload({ name: 'rejected', value: 3, color: '#dc2626' })} />
      </ChartContainer>,
    )
    expect(screen.getAllByText('Rechazadas')).toHaveLength(2)
    expect(container.querySelector('[style*="--color-bg"]')).toBeInTheDocument()
  })

  it('oculta la leyenda vacía y traduce entradas visibles alineadas arriba', () => {
    const { rerender, container } = render(<ChartContainer config={config}><ChartLegendContent payload={[]} /></ChartContainer>)
    expect(container.querySelector('[class*="flex items-center justify-center gap-4"]')).not.toBeInTheDocument()

    rerender(
      <ChartContainer config={config}>
        <ChartLegendContent
          verticalAlign="top"
          payload={legend(
            { dataKey: 'approved', color: '#15803d', type: 'rect' },
            { dataKey: 'rejected', color: '#dc2626', type: 'none' },
          )}
        />
      </ChartContainer>,
    )
    expect(screen.getByText('Aprobadas')).toBeInTheDocument()
    expect(screen.queryByText('Rechazadas')).not.toBeInTheDocument()
    expect(container.querySelector('.pb-3')).toBeInTheDocument()
  })

  it('admite nombre alternativo e ícono o marcador de color en la leyenda', () => {
    function CheckIcon() { return <svg aria-label="Indicador personalizado" /> }
    const iconConfig: ChartConfig = { approved: { label: 'Aprobadas', icon: CheckIcon } }
    const { rerender } = render(
      <ChartContainer config={iconConfig}>
        <ChartLegendContent nameKey="kind" payload={legend({ dataKey: 'series', kind: 'approved', value: 'Approved', color: '#15803d' })} />
      </ChartContainer>,
    )
    expect(screen.getByLabelText('Indicador personalizado')).toBeInTheDocument()

    rerender(
      <ChartContainer config={iconConfig}>
        <ChartLegendContent hideIcon payload={legend({ dataKey: 'approved', value: 'Approved', color: '#15803d' })} />
      </ChartContainer>,
    )
    expect(screen.queryByLabelText('Indicador personalizado')).not.toBeInTheDocument()
    expect(screen.getByText('Aprobadas')).toBeInTheDocument()
  })
})
