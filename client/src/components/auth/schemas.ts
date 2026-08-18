import * as z from "zod"

// Hand-written for now — mirrors the backend's LoginRequest/CreateUserRequest
// shape (server/src/main/kotlin/.../feature/auth/dto/request). Swap these for
// the schemas exported by the generated API client once /auth is wired up.

const loginSchema = z.object({
  username: z.string().min(1, "Ingresá tu usuario."),
  password: z.string().min(1, "Ingresá tu contraseña."),
})

const registerSchema = z
  .object({
    name: z.string().min(2, "El nombre debe tener al menos 2 caracteres."),
    username: z.string().min(3, "El usuario debe tener al menos 3 caracteres."),
    email: z.email("Ingresá un correo electrónico válido."),
    password: z.string().min(8, "La contraseña debe tener al menos 8 caracteres."),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Las contraseñas no coinciden.",
    path: ["confirmPassword"],
  })

type LoginValues = z.infer<typeof loginSchema>
type RegisterValues = z.infer<typeof registerSchema>

export { loginSchema, registerSchema }
export type { LoginValues, RegisterValues }
