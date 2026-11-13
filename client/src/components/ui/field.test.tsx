import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSeparator,
  FieldSet,
  FieldTitle,
} from './field'

describe('Field', () => {
  it.each(['vertical', 'horizontal', 'responsive'] as const)('aplica a orientação %s ao grupo do campo', (orientation) => {
    render(<Field orientation={orientation} aria-label="Datos personales"><FieldTitle>Nombre</FieldTitle></Field>)
    expect(screen.getByRole('group', { name: 'Datos personales' })).toHaveAttribute('data-orientation', orientation)
  })

  it('asocia la etiqueta al control y presenta descripción contextual', () => {
    render(
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="email">Correo electrónico</FieldLabel>
          <FieldContent>
            <input id="email" type="email" />
            <FieldDescription>No compartimos tu dirección.</FieldDescription>
          </FieldContent>
        </Field>
      </FieldGroup>,
    )
    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('type', 'email')
    expect(screen.getByText('No compartimos tu dirección.')).toBeInTheDocument()
  })

  it('muestra un mensaje de error singular y omite el contenedor vacío', () => {
    const { rerender } = render(<FieldError errors={[{ message: 'Dato inválido' }]} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Dato inválido')
    rerender(<FieldError />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('elimina errores duplicados y enumera mensajes distintos', () => {
    render(<FieldError errors={[
      { message: 'Es obligatorio' },
      { message: 'Es obligatorio' },
      undefined,
      { message: 'Debe tener formato válido' },
      {},
    ]} />)
    const alert = screen.getByRole('alert')
    expect(alert.querySelectorAll('li')).toHaveLength(2)
    expect(alert).toHaveTextContent('Es obligatorio')
    expect(alert).toHaveTextContent('Debe tener formato válido')
  })

  it('da prioridad al contenido explícito frente a errores de validación', () => {
    render(<FieldError errors={[{ message: 'Error de schema' }]}><span>Mensaje de ayuda</span></FieldError>)
    expect(screen.getByRole('alert')).toHaveTextContent('Mensaje de ayuda')
    expect(screen.queryByText('Error de schema')).not.toBeInTheDocument()
  })

  it('distingue separadores con y sin texto y permite títulos de campo', () => {
    const { container } = render(
      <FieldSet>
        <FieldLegend variant="label">Preferencias</FieldLegend>
        <FieldSeparator>o continuar</FieldSeparator>
        <FieldSeparator />
        <FieldTitle>Notificaciones</FieldTitle>
      </FieldSet>,
    )
    expect(screen.getByText('Preferencias')).toHaveAttribute('data-variant', 'label')
    expect(screen.getByText('o continuar')).toBeInTheDocument()
    expect(container.querySelectorAll('[data-slot="field-separator-content"]')).toHaveLength(1)
    expect(screen.getByText('Notificaciones')).toHaveAttribute('data-slot', 'field-label')
  })
})
