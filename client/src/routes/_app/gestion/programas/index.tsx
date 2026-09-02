import { IconAlertTriangle, IconPlus, IconRefresh, IconTarget, IconWriting } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { OutletNavRightButton, OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { create2Mutation, listOptions, listQueryKey } from "@/generated/@tanstack/react-query.gen"
import type { CreateProgramRequest } from "@/generated/types.gen"
import { zCreateProgramRequest } from "@/generated/zod.gen"

const PAGE_SIZE = 10
const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
})
const createProgramSchema = zCreateProgramRequest.extend({
  name: zCreateProgramRequest.shape.name.trim().min(1, "Ingresá el nombre del programa."),
})

export const Route = createFileRoute("/_app/gestion/programas/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [createOpen, setCreateOpen] = useState(false)
  const { data, isPending, isError, isFetching, dataUpdatedAt, refetch } = useQuery(
    listOptions({ query: { page: page - 1, size: PAGE_SIZE } }),
  )
  const programs = data?.content ?? []
  const totalItems = Number(data?.totalElements ?? 0)
  const totalPages = Math.max(1, data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)
  const setPage = (nextPage: number) => navigate({ search: { page: nextPage } })

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Programas" }]} />
        <OutletNavRightButton className="gap-1.5">
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <IconPlus />
            Nuevo programa
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-2 sm:mx-4! lg:py-4">
            {dataUpdatedAt > 0 && (
              <div className="mb-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                <span>Última actualización: {new Date(dataUpdatedAt).toLocaleTimeString("es-AR")}</span>
                <Button size="xs" variant="ghost" onClick={() => refetch()} disabled={isFetching}>
                  <IconRefresh className={isFetching ? "animate-spin" : undefined} />
                  Actualizar
                </Button>
              </div>
            )}
            {isPending ? (
              <p className="text-sm text-muted-foreground">Cargando programas…</p>
            ) : isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los programas.</p>
                <Button size="sm" variant="outline" onClick={() => refetch()}>Reintentar</Button>
              </div>
            ) : programs.length === 0 ? (
              <p className="text-sm text-muted-foreground">No hay programas registrados.</p>
            ) : (
              <Table>
                <TableHeader><TableRow><TableHead>Nombre</TableHead><TableHead>Objetivo</TableHead><TableHead>Creado</TableHead><TableHead>Actualizado</TableHead></TableRow></TableHeader>
                <TableBody>
                  {programs.map((program) => (
                    <TableRow key={program.id}>
                      <TableCell className="font-medium">
                        <Link to="/gestion/programas/$programaId" params={{ programaId: program.id ?? "" }} className="hover:underline">{program.name}</Link>
                      </TableCell>
                      <TableCell className="max-w-xl whitespace-normal text-muted-foreground">{program.objective || "—"}</TableCell>
                      <TableCell className="text-muted-foreground">{program.createdAt ? new Date(program.createdAt).toLocaleDateString("es-AR") : "—"}</TableCell>
                      <TableCell className="text-muted-foreground">{program.updatedAt ? new Date(program.updatedAt).toLocaleDateString("es-AR") : "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>
        {!isPending && !isError && totalItems > 0 && (
          <DataPagination page={currentPage} totalPages={totalPages} totalItems={totalItems} pageSize={PAGE_SIZE} onPageChange={setPage} />
        )}
      </SidebarShellContent>
      {createOpen && (
        <CreateProgramDialog onOpenChange={(open) => !open && setCreateOpen(false)} />
      )}
    </SidebarShell>
  )
}

function CreateProgramDialog({ onOpenChange }: { onOpenChange: (open: boolean) => void }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createProgram = useMutation({
    ...create2Mutation(),
    onSuccess: (program) => {
      queryClient.invalidateQueries({ queryKey: listQueryKey() })
      onOpenChange(false)
      if (program.id) {
        navigate({
          to: "/gestion/programas/$programaId",
          params: { programaId: program.id },
        })
      }
    },
  })
  const form = useForm({
    defaultValues: { name: "" } as CreateProgramRequest,
    validators: { onChange: createProgramSchema },
    onSubmit: ({ value }) => createProgram.mutate({ body: value }),
  })

  return (
    <Dialog open onOpenChange={(open) => { if (!createProgram.isPending) onOpenChange(open) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nuevo programa</DialogTitle>
          <DialogDescription>
            Definí el nombre y el objetivo del nuevo programa social.
          </DialogDescription>
        </DialogHeader>

        {createProgram.isError && (
          <Alert variant="destructive">
            <IconAlertTriangle />
            <AlertTitle>No se pudo crear el programa</AlertTitle>
            <AlertDescription>
              {createProgram.error.message ?? "Revisá los datos e intentá nuevamente."}
            </AlertDescription>
          </Alert>
        )}

        <form
          id="create-program-form"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            event.stopPropagation()
            form.handleSubmit()
          }}
        >
          <FieldGroup>
            <form.Field name="name" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Nombre</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon><IconWriting /></InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                      aria-invalid={invalid}
                      placeholder="Becas de formación laboral"
                      maxLength={200}
                      autoFocus
                    />
                  </InputGroup>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />

            <form.Field name="objective" children={(field) => {
              const invalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={invalid}>
                  <FieldLabel htmlFor={field.name}>Objetivo</FieldLabel>
                  <div className="relative">
                    <IconTarget className="absolute top-3 left-3 size-4 text-muted-foreground" />
                    <Textarea
                      id={field.name}
                      name={field.name}
                      value={field.state.value ?? ""}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value || undefined)}
                      aria-invalid={invalid}
                      placeholder="Describí el propósito del programa"
                      className="min-h-32 pl-9"
                    />
                  </div>
                  {invalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }} />
          </FieldGroup>
        </form>

        <DialogFooter>
          <DialogClose render={<Button type="button" variant="outline" disabled={createProgram.isPending} />}>
            Cancelar
          </DialogClose>
          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]} children={([canSubmit, isSubmitting]) => (
            <Button
              type="submit"
              form="create-program-form"
              disabled={!canSubmit || isSubmitting || createProgram.isPending}
            >
              {createProgram.isPending ? "Creando…" : "Crear programa"}
            </Button>
          )} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
