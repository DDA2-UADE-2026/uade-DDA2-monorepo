export const PROGRAM_IMAGE_ACCEPT = ".jpg,.jpeg,.png,image/jpeg,image/png"
export const MAX_PROGRAM_IMAGE_BYTES = 10 * 1024 * 1024

export function validateProgramImageFile(file: Pick<File, "name" | "size" | "type">): string | undefined {
  if (file.size === 0) return "La imagen está vacía. Seleccioná otra."
  if (file.size > MAX_PROGRAM_IMAGE_BYTES) return "La imagen supera los 10 MB permitidos."

  const fileName = file.name.trim()
  if (!fileName || fileName.length > 255) return "La imagen debe tener un nombre de archivo válido."

  const extension = fileName.split(".").pop()?.toLowerCase()
  const expectedType = extension === "png"
    ? "image/png"
    : extension === "jpg" || extension === "jpeg"
      ? "image/jpeg"
      : undefined

  if (!expectedType || file.type.toLowerCase() !== expectedType) {
    return "Seleccioná una imagen JPG o PNG con el formato correcto."
  }
}

export function programImageSource(imageUrl?: string): string | undefined {
  if (!imageUrl) return undefined
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl

  const serverUrl = import.meta.env.VITE_SERVER_URL?.replace(/\/+$/, "") ?? ""
  const imagePath = imageUrl.startsWith("/") ? imageUrl : `/${imageUrl}`
  return `${serverUrl}${imagePath}`
}
