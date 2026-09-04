import { useState } from "react"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"

type DeleteConfirmationButtonProps = {
  description: string
  disabled?: boolean
  onConfirm: () => Promise<unknown>
  size?: "default" | "sm"
}

export function DeleteConfirmationButton({
  description,
  disabled = false,
  onConfirm,
  size = "default",
}: DeleteConfirmationButtonProps) {
  const [open, setOpen] = useState(false)
  const [isConfirming, setIsConfirming] = useState(false)

  const handleConfirm = async () => {
    setIsConfirming(true)
    try {
      await onConfirm()
      setOpen(false)
    } catch {
      // The mutation owns error feedback. Keep the confirmation open for retry.
    } finally {
      setIsConfirming(false)
    }
  }

  return (
    <AlertDialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!isConfirming) setOpen(nextOpen)
      }}
    >
      <AlertDialogTrigger
        disabled={disabled || isConfirming}
        render={<Button type="button" size={size} variant={size === "sm" ? "ghost" : "destructive"} />}
      >
        Eliminar
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Confirmás la eliminación?</AlertDialogTitle>
          <AlertDialogDescription>{description} Esta acción no se puede deshacer.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isConfirming}>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            type="button"
            variant="destructive"
            disabled={isConfirming}
            onClick={handleConfirm}
          >
            {isConfirming ? "Eliminando…" : "Eliminar"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
