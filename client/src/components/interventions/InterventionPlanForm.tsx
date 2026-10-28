import { IconAlertTriangle, IconPlus, IconTrash } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"

import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Separator } from "@/components/ui/separator"
import { Textarea } from "@/components/ui/textarea"
import { create2Mutation, get4QueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateInterventionPlanRequest, InterventionActionRequest, InterventionPlanResponse } from "@/generated/types.gen"

type DraftAction = InterventionActionRequest & { key: string }
type DraftObjective = { key: string; description: string; actions: DraftAction[] }

const key = () => crypto.randomUUID()
const blankAction = (): DraftAction => ({ key: key(), description: "", startDate: "", dueDate: "" })
const blankObjective = (): DraftObjective => ({ key: key(), description: "", actions: [blankAction()] })

export function InterventionPlanForm({ applicationId, onCreated }: { applicationId: string; onCreated?: (plan: InterventionPlanResponse) => void }) {
  const queryClient = useQueryClient()
  const [objectives, setObjectives] = useState<DraftObjective[]>([blankObjective()])
  const [validationError, setValidationError] = useState<string | null>(null)
  const create = useMutation({
    ...create2Mutation(),
    onSuccess: (plan) => {
      queryClient.setQueryData(get4QueryKey({ path: { id: applicationId } }), plan)
      queryClient.invalidateQueries({ queryKey: get4QueryKey({ path: { id: applicationId } }) })
      onCreated?.(plan)
    },
  })

  const updateObjective = (objectiveKey: string, update: (objective: DraftObjective) => DraftObjective) =>
    setObjectives((current) => current.map((objective) => objective.key === objectiveKey ? update(objective) : objective))

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const body: CreateInterventionPlanRequest = {
      objectives: objectives.map((objective) => ({
        description: objective.description.trim(),
        actions: objective.actions.map(({ description, startDate, dueDate }) => ({ description: description.trim(), startDate, dueDate })),
      })),
    }
    const invalidObjective = body.objectives.find((objective) => !objective.description || !objective.actions.length)
    const invalidAction = body.objectives.flatMap((objective) => objective.actions).find((action) => !action.description || !action.startDate || !action.dueDate || action.startDate > action.dueDate)
    if (invalidObjective || invalidAction) {
      setValidationError("Completá las descripciones y fechas. La fecha de fin no puede ser anterior al inicio.")
      return
    }
    setValidationError(null)
    create.mutate({ path: { id: applicationId }, body })
  }

  return <Card>
    <CardHeader>
      <CardTitle>Crear plan de intervención</CardTitle>
      <CardDescription>Definí los objetivos acordados y sus acciones. La estructura y las fechas quedarán fijas después de crear el plan.</CardDescription>
    </CardHeader>
    <CardContent>
      {(validationError || create.error) && <Alert variant="destructive" className="mb-5">
        <IconAlertTriangle />
        <AlertTitle>No se pudo crear el plan</AlertTitle>
        <AlertDescription>{validationError ?? create.error?.message ?? "Revisá los datos e intentá nuevamente."}</AlertDescription>
      </Alert>}
      <form onSubmit={submit} className="space-y-5">
        {objectives.map((objective, objectiveIndex) => <Card key={objective.key} size="sm" className="bg-muted/20">
          <CardHeader>
            <div className="flex items-start justify-between gap-3">
              <div><CardTitle>Objetivo {objectiveIndex + 1}</CardTitle><CardDescription>Resultado general que se busca alcanzar.</CardDescription></div>
              {objectives.length > 1 && <Button type="button" size="icon-sm" variant="ghost" aria-label={`Eliminar objetivo ${objectiveIndex + 1}`} onClick={() => setObjectives((current) => current.filter((item) => item.key !== objective.key))}><IconTrash /></Button>}
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            <Field>
              <FieldLabel htmlFor={`objective-${objective.key}`}>Descripción del objetivo</FieldLabel>
              <Textarea id={`objective-${objective.key}`} maxLength={1000} value={objective.description} onChange={(event) => updateObjective(objective.key, (item) => ({ ...item, description: event.target.value }))} placeholder="Ej. Fortalecer la autonomía del ciudadano." />
            </Field>
            <Separator />
            <div className="space-y-4">
              {objective.actions.map((action, actionIndex) => <div key={action.key} className="rounded-xl border bg-background p-4">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <p className="font-medium">Acción {actionIndex + 1}</p>
                  {objective.actions.length > 1 && <Button type="button" size="icon-sm" variant="ghost" aria-label={`Eliminar acción ${actionIndex + 1}`} onClick={() => updateObjective(objective.key, (item) => ({ ...item, actions: item.actions.filter((entry) => entry.key !== action.key) }))}><IconTrash /></Button>}
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field className="sm:col-span-2">
                    <FieldLabel htmlFor={`action-${action.key}`}>Descripción</FieldLabel>
                    <Textarea id={`action-${action.key}`} maxLength={1000} value={action.description} onChange={(event) => updateObjective(objective.key, (item) => ({ ...item, actions: item.actions.map((entry) => entry.key === action.key ? { ...entry, description: event.target.value } : entry) }))} placeholder="Ej. Acompañar el acceso a recursos municipales." />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor={`start-${action.key}`}>Inicio</FieldLabel>
                    <ProgramDatePicker
                      id={`start-${action.key}`}
                      value={action.startDate}
                      onChange={(startDate) => updateObjective(objective.key, (item) => ({ ...item, actions: item.actions.map((entry) => entry.key === action.key ? { ...entry, startDate } : entry) }))}
                      placeholder="Seleccionar inicio"
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor={`due-${action.key}`}>Fin previsto</FieldLabel>
                    <ProgramDatePicker
                      id={`due-${action.key}`}
                      value={action.dueDate}
                      onChange={(dueDate) => updateObjective(objective.key, (item) => ({ ...item, actions: item.actions.map((entry) => entry.key === action.key ? { ...entry, dueDate } : entry) }))}
                      placeholder="Seleccionar fin previsto"
                      disabled={action.startDate ? { before: parseLocalDate(action.startDate)! } : undefined}
                    />
                  </Field>
                </div>
              </div>)}
              <Button type="button" variant="outline" size="sm" onClick={() => updateObjective(objective.key, (item) => ({ ...item, actions: [...item.actions, blankAction()] }))}><IconPlus />Agregar acción</Button>
            </div>
          </CardContent>
        </Card>)}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div><Button type="button" variant="outline" onClick={() => setObjectives((current) => [...current, blankObjective()])}><IconPlus />Agregar objetivo</Button><FieldDescription className="mt-2">Debe haber al menos un objetivo y una acción por objetivo.</FieldDescription></div>
          <Button type="submit" disabled={create.isPending}>{create.isPending ? "Creando plan…" : "Crear plan"}</Button>
        </div>
      </form>
    </CardContent>
  </Card>
}
