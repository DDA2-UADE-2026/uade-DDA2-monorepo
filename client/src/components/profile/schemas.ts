import * as z from "zod"

// Hand-written for now — swap for the schema exported by the generated API
// client once /profile is wired up (ver components/auth/schemas.ts).

const profileSchema = z.object({
  name: z.string().min(2, "El nombre debe tener al menos 2 caracteres."),
  email: z.email("Ingresá un correo electrónico válido."),
})

type ProfileValues = z.infer<typeof profileSchema>

export { profileSchema }
export type { ProfileValues }
