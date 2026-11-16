import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { UserChip } from "@/components/UserChip"
import { findAllQueryKey, findByIdQueryKey } from "@/generated/@tanstack/react-query.gen"
import { findById, update } from "@/generated/sdk.gen"
import type { UserManagementResponse } from "@/generated/types.gen"

function errorMessage(error: unknown) {
  if (typeof error === "object" && error !== null && "message" in error && typeof error.message === "string") return error.message
  return "Intentá nuevamente en unos instantes."
}

export function ActivateUserDialog({ user, onClose }: { user: UserManagementResponse & { id: number }; onClose: () => void }) {
  const queryClient = useQueryClient()
  const activate = useMutation({
    mutationFn: async () => {
      // La actualización reemplaza los roles, por eso consultamos los datos más recientes antes de enviarla.
      const { data } = await findById({ path: { id: user.id }, throwOnError: true })
      if (!data.name || !data.email || !data.roles) throw new Error("No se pudieron obtener los datos completos del usuario.")
      if (!data.active) await update({ path: { id: user.id }, body: { name: data.name, email: data.email, roles: data.roles, active: true }, throwOnError: true })
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: findAllQueryKey() }),
        queryClient.invalidateQueries({ queryKey: findByIdQueryKey({ path: { id: user.id } }) }),
      ])
      toast.success("Usuario habilitado")
      onClose()
    },
  })

  return <Dialog open onOpenChange={(open) => { if (!open && !activate.isPending) onClose() }}>
    <DialogContent>
      <DialogHeader><DialogTitle>Habilitar usuario</DialogTitle><DialogDescription>La cuenta quedará activa y conservará sus roles actuales.</DialogDescription></DialogHeader>
      <UserChip user={{ id: user.id, name: user.name, email: user.email }} secondary={user.email} />
      {activate.error && <Alert variant="destructive"><AlertTitle>No se pudo habilitar el usuario</AlertTitle><AlertDescription>{errorMessage(activate.error)}</AlertDescription></Alert>}
      <DialogFooter><Button variant="outline" disabled={activate.isPending} onClick={onClose}>Cancelar</Button><Button disabled={activate.isPending} onClick={() => activate.mutate()}>{activate.isPending ? "Habilitando…" : "Habilitar usuario"}</Button></DialogFooter>
    </DialogContent>
  </Dialog>
}
