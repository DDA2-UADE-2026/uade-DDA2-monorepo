import { toast } from "sonner"

import type { ErrorResponse } from "@/generated/types.gen"

export function showApiErrorToast(error: ErrorResponse) {
  const fieldErrors = error.fields
    ?.map((field) => field.message)
    .filter((message): message is string => Boolean(message))
    .join(" ")

  toast.error("No se pudo completar la operación", {
    description: error.message ?? fieldErrors ?? "Ocurrió un error inesperado.",
  })
}
