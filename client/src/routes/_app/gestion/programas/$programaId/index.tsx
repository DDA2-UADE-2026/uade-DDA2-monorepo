import { IconPhoto, IconUpload } from "@tabler/icons-react"
import { useForm } from "@tanstack/react-form"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect, useState } from "react"
import { toast } from "sonner"
import { showApiErrorToast } from "@/components/errors/showApiErrorToast"
import { DeleteConfirmationButton } from "@/components/programs/DeleteConfirmationButton"
import { ProgramImageUploadDialog } from "@/components/programs/ProgramImageUploadDialog"
import { FormField, LoadingOrError, RoutePanel, inputClass, textareaClass } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { delete2Mutation, deleteProgramImageMutation, findById2Options, findById2QueryKey, getAvailableProgramQueryKey, list1QueryKey, listAvailableProgramsQueryKey, update2Mutation } from "@/generated/@tanstack/react-query.gen"
import type { UpdateProgramRequest } from "@/generated/types.gen"
import { zUpdateProgramRequest } from "@/generated/zod.gen"
import { programImageSource } from "@/lib/program-images"

const updateProgramSchema = zUpdateProgramRequest.extend({
  name: zUpdateProgramRequest.shape.name.trim().min(1, "Ingresá el nombre del programa."),
})

export const Route = createFileRoute("/_app/gestion/programas/$programaId/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [imageDialogOpen, setImageDialogOpen] = useState(false)
  const query = useQuery(findById2Options({ path: { id: programaId } }))
  const update = useMutation({ ...update2Mutation(), onSuccess: () => { client.invalidateQueries({ queryKey: findById2QueryKey({ path: { id: programaId } }) }); client.invalidateQueries({ queryKey: list1QueryKey() }) }, onError: showApiErrorToast })
  const remove = useMutation({ ...delete2Mutation(), onSuccess: () => { client.invalidateQueries({ queryKey: list1QueryKey() }); navigate({ to: "/gestion/programas", search: { page: 1 } }) }, onError: showApiErrorToast })
  const removeImage = useMutation({
    ...deleteProgramImageMutation(),
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: findById2QueryKey({ path: { id: programaId } }) }),
        client.invalidateQueries({ queryKey: list1QueryKey() }),
        client.invalidateQueries({ queryKey: getAvailableProgramQueryKey({ path: { id: programaId } }) }),
        client.invalidateQueries({ queryKey: listAvailableProgramsQueryKey() }),
      ])
      toast.success("Imagen de portada eliminada")
    },
    onError: showApiErrorToast,
  })
  const form = useForm({
    defaultValues: { name: "" } as UpdateProgramRequest,
    validators: { onChange: updateProgramSchema },
    onSubmit: ({ value }) => update.mutate({
      path: { id: programaId },
      body: value,
    }),
  })
  useEffect(() => {
    if (query.data) form.reset({ name: query.data.name ?? "", objective: query.data.objective })
  }, [form, query.data])

  return <RoutePanel>
    <LoadingOrError pending={query.isPending} error={query.isError} retry={() => query.refetch()} />
    {query.data && <div className="grid gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Imagen de portada</CardTitle>
          <CardDescription>Se muestra en el catálogo público y en el detalle del programa.</CardDescription>
        </CardHeader>
        <CardContent>
          {query.data.imageUrl ? (
            <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
              <img
                src={programImageSource(query.data.imageUrl)}
                alt={`Portada de ${query.data.name ?? "programa"}`}
                className="aspect-[1.91/1] w-full max-w-xl rounded-xl border bg-muted object-cover"
              />
              <div className="flex flex-wrap gap-2 sm:flex-col">
                <Button type="button" variant="outline" onClick={() => setImageDialogOpen(true)}>
                  <IconPhoto />
                  Reemplazar
                </Button>
                <DeleteConfirmationButton
                  description="Se eliminará la imagen de portada de este programa."
                  disabled={removeImage.isPending}
                  onConfirm={() => removeImage.mutateAsync({ path: { programId: programaId } })}
                />
              </div>
            </div>
          ) : (
            <div className="flex min-h-48 flex-col items-center justify-center gap-3 rounded-xl border border-dashed bg-muted/30 p-6 text-center">
              <IconPhoto className="size-10 text-muted-foreground" />
              <div>
                <p className="font-medium">Este programa todavía no tiene portada</p>
                <p className="mt-1 text-sm text-muted-foreground">Podés subir una imagen JPG o PNG de hasta 10 MB.</p>
              </div>
              <Button type="button" variant="outline" onClick={() => setImageDialogOpen(true)}>
                <IconUpload />
                Subir portada
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <form className="grid gap-5" noValidate onSubmit={(event) => { event.preventDefault(); event.stopPropagation(); form.handleSubmit() }}>
      <form.Field name="name" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Nombre" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><input id={field.name} name={field.name} className={inputClass} value={field.state.value} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value)} aria-invalid={invalid} /></FormField>
      }} />
      <form.Field name="objective" children={(field) => {
        const invalid = field.state.meta.isTouched && !field.state.meta.isValid
        return <FormField label="Objetivo" htmlFor={field.name} invalid={invalid} errors={field.state.meta.errors}><textarea id={field.name} name={field.name} className={textareaClass} value={field.state.value ?? ""} onBlur={field.handleBlur} onChange={(event) => field.handleChange(event.target.value || undefined)} aria-invalid={invalid} /></FormField>
      }} />
      <div className="flex justify-between gap-2"><DeleteConfirmationButton description="Se eliminará este programa." disabled={remove.isPending} onConfirm={() => remove.mutateAsync({ path: { id: programaId } })} /><Button type="submit" disabled={update.isPending}>{update.isPending ? "Guardando…" : "Guardar cambios"}</Button></div>
      </form>

      {imageDialogOpen && (
        <ProgramImageUploadDialog
          programId={programaId}
          hasImage={Boolean(query.data.imageUrl)}
          onOpenChange={setImageDialogOpen}
        />
      )}
    </div>}
  </RoutePanel>
}
