import { PROGRAM_IMAGE_FALLBACK, programImageSource } from "@/lib/program-images"
import { cn } from "@/lib/utils"

/**
 * Portada de un programa con una segunda copia borroneada detrás, que tiñe el
 * fondo con los colores de la imagen. El contenedor que la use conviene que
 * recorte el desborde del glow.
 */
export function ProgramThumbnail({
  imageUrl,
  className,
  glowClassName = "blur-2xl",
}: {
  imageUrl?: string
  /** Define el tamaño del recuadro nítido, por ejemplo `size-16`. */
  className?: string
  glowClassName?: string
}) {
  const source = programImageSource(imageUrl) ?? PROGRAM_IMAGE_FALLBACK

  return (
    <span className={cn("relative inline-flex shrink-0", className)}>
      <img
        src={source}
        alt=""
        loading="lazy"
        className="relative z-10 size-full rounded-xl object-cover"
      />
      <img
        src={source}
        alt=""
        aria-hidden="true"
        loading="lazy"
        className={cn("absolute inset-0 size-full rounded-xl object-cover", glowClassName)}
      />
    </span>
  )
}
