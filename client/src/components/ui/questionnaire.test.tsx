import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import {
  Questionnaire,
  QuestionnaireActions,
  QuestionnaireChoice,
  QuestionnaireChoiceDescription,
  QuestionnaireChoices,
  QuestionnaireDescription,
  QuestionnaireError,
  QuestionnaireInput,
  QuestionnaireItem,
  QuestionnaireNext,
  QuestionnairePrevious,
  QuestionnaireProgress,
  QuestionnaireSkip,
  QuestionnaireSubmit,
  QuestionnaireTitle,
} from './questionnaire'

function FormFixture({ onComplete = vi.fn(), onFirstStatus = vi.fn(), onSecondStatus }: {
  onComplete?: (data: FormData) => void
  onFirstStatus?: (status: 'unanswered' | 'answered' | 'skipped') => void
  onSecondStatus?: (status: 'unanswered' | 'answered' | 'skipped') => void
}) {
  return (
    <Questionnaire
      aria-label="Solicitud de actividad"
      items={[
        { name: 'participa', required: true, choices: [{ value: 'si' }, { value: 'no', disabled: true }] },
        { name: 'comentario' },
      ]}
      onSubmit={(event) => {
        event.preventDefault()
        onComplete(new FormData(event.currentTarget))
      }}
    >
      <QuestionnaireProgress />
      <QuestionnaireItem name="participa" required onStatusChange={onFirstStatus}>
        <QuestionnaireTitle>¿Querés participar?</QuestionnaireTitle>
        <QuestionnaireDescription>Elegí una respuesta para continuar.</QuestionnaireDescription>
        <QuestionnaireChoices>
          <QuestionnaireChoice value="si">Sí<QuestionnaireChoiceDescription>Quiero anotarme</QuestionnaireChoiceDescription></QuestionnaireChoice>
          <QuestionnaireChoice value="no" disabled>No</QuestionnaireChoice>
        </QuestionnaireChoices>
        <QuestionnaireError>Esta respuesta es obligatoria.</QuestionnaireError>
      </QuestionnaireItem>
      <QuestionnaireItem name="comentario" onStatusChange={onSecondStatus}>
        <QuestionnaireTitle>Comentario adicional</QuestionnaireTitle>
        <QuestionnaireInput aria-label="Comentario" type="text" />
      </QuestionnaireItem>
      <QuestionnaireActions>
        <QuestionnairePrevious type="button">Anterior</QuestionnairePrevious>
        <QuestionnaireSkip type="button">Saltar</QuestionnaireSkip>
        <QuestionnaireNext type="button">Continuar</QuestionnaireNext>
        <QuestionnaireSubmit type="submit">Enviar</QuestionnaireSubmit>
      </QuestionnaireActions>
    </Questionnaire>
  )
}

describe('Questionnaire', () => {
  it('inicia en el primer paso, informa el progreso y permite avanzar luego de responder', async () => {
    const onFirstStatus = vi.fn()
    render(<FormFixture onFirstStatus={onFirstStatus} />)

    expect(screen.getByRole('group', { name: '¿Querés participar?' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Anterior' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled()
    await userEvent.click(screen.getByRole('radio', { name: /Sí/ }))

    await waitFor(() => expect(onFirstStatus).toHaveBeenLastCalledWith('answered'))
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByRole('group', { name: 'Comentario adicional' })).toBeInTheDocument()
    expect(screen.getByLabelText('Comentario')).toBeInTheDocument()
  })

  it('permite volver al paso anterior y conserva la respuesta elegida', async () => {
    render(<FormFixture />)
    await userEvent.click(screen.getByRole('radio', { name: /Sí/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    await userEvent.type(screen.getByLabelText('Comentario'), 'Prefiero el turno mañana')
    await userEvent.click(screen.getByRole('button', { name: 'Anterior' }))

    expect(screen.getByRole('radio', { name: /Sí/ })).toBeChecked()
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByLabelText('Comentario')).toHaveValue('Prefiero el turno mañana')
  })

  it('ofrece saltar el paso opcional y enviar solo las respuestas contestadas', async () => {
    const onComplete = vi.fn()
    const onFirstStatus = vi.fn()
    render(<FormFixture onComplete={onComplete} onFirstStatus={onFirstStatus} />)
    await userEvent.click(screen.getByRole('radio', { name: /Sí/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Saltar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))

    await waitFor(() => expect(onComplete).toHaveBeenCalled())
    const data = onComplete.mock.calls.at(-1)![0]
    expect(data.get('participa')).toBe('si')
    expect(data.has('comentario')).toBe(false)
  })

  it('acepta texto opcional al enviar y excluye las opciones deshabilitadas', async () => {
    const onComplete = vi.fn()
    render(<FormFixture onComplete={onComplete} />)
    const disabledChoice = screen.getByRole('radio', { name: 'No' })
    expect(disabledChoice).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: /Sí/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    await userEvent.type(screen.getByLabelText('Comentario'), 'Sin comentarios')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))

    await waitFor(() => expect(onComplete).toHaveBeenCalledOnce())
    const data = onComplete.mock.calls[0][0]
    expect(data.get('participa')).toBe('si')
    expect(data.get('comentario')).toBe('Sin comentarios')
    expect(data.get('participa')).not.toBe('no')
  })

  it('permite omitir el paso opcional y comunica el estado de omisión', async () => {
    const onSecondStatus = vi.fn()
    render(<FormFixture onSecondStatus={onSecondStatus} />)
    await userEvent.click(screen.getByRole('radio', { name: /Sí/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Saltar' }))
    await waitFor(() => expect(onSecondStatus).toHaveBeenLastCalledWith('skipped'))
    expect(screen.getByRole('group', { name: 'Comentario adicional' })).toBeInTheDocument()
  })
})
