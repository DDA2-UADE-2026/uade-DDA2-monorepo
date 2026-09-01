import { IconAlertTriangle, IconTarget, IconWriting } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"

import { OutletNavContent, OutletNavRightButton, OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Textarea } from "@/components/ui/textarea"
import { create2Mutation, listQueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateProgramRequest } from "@/generated/types.gen"
import { zCreateProgramRequest } from "@/generated/zod.gen"

const createProgramSchema = zCreateProgramRequest.extend({
  name: zCreateProgramRequest.shape.name.trim().min(1, "Ingresá el nombre del programa."),
})

export const Route = createFileRoute("/_app/gestion/programas/nuevo")({
  component: RouteComponent,
})

function RouteComponent() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createProgram = useMutation({
    ...create2Mutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listQueryKey() })
      navigate({ to: "/gestion/programas", search: { page: 1 } })
    },
  })
  const form = useForm({
    defaultValues: { name: "" } as CreateProgramRequest,
    validators: { onChange: createProgramSchema },
    onSubmit: ({ value }) => createProgram.mutate({
      body: value,
    }),
  })

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavContent>Nuevo programa</OutletNavContent>
        <OutletNavRightButton className="gap-1.5">
          <Button type="submit" form="create-program-form" size="sm" disabled={createProgram.isPending}>
            {createProgram.isPending ? "Creando…" : "Crear programa"}
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-2xl px-4 py-4 lg:px-6 lg:py-6">
            {createProgram.isError && (
              <Alert variant="destructive" className="mb-6"><IconAlertTriangle /><AlertTitle>No se pudo crear el programa</AlertTitle><AlertDescription>Revisá los datos e intentá de nuevo.</AlertDescription></Alert>
            )}
            <form id="create-program-form" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}>
              <FieldGroup>
                <form.Field name="name" children={(field) => {
                  const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
                  return <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>Nombre</FieldLabel>
                    <InputGroup><InputGroupAddon><IconWriting /></InputGroupAddon><InputGroupInput id={field.name} name={field.name} value={field.state.value} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={isInvalid} placeholder="Becas de formación laboral" maxLength={200} autoFocus /></InputGroup>
                    {isInvalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                }} />
                <form.Field name="objective" children={(field) => {
                  const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
                  return <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>Objetivo</FieldLabel>
                    <div className="relative"><IconTarget className="absolute left-3 top-3 size-4 text-muted-foreground" /><Textarea id={field.name} name={field.name} value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value || undefined)} aria-invalid={isInvalid} placeholder="Describí el propósito del programa" className="min-h-32 pl-9" /></div>
                    {isInvalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                }} />
              </FieldGroup>
            </form>
          </div>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
