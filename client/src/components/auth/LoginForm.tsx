import { useState } from "react"
import { useForm } from "@tanstack/react-form"
import { Link } from "@tanstack/react-router"
import { IconEye, IconEyeOff, IconId, IconLock, IconUser } from "@tabler/icons-react"

import { AuthCard } from "@/components/auth/AuthCard"
import { loginSchema } from "@/components/auth/schemas"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"
import { Separator } from "@/components/ui/separator"

function LoginForm() {
  const [showPassword, setShowPassword] = useState(false)

  const form = useForm({
    defaultValues: {
      username: "",
      password: "",
    },
    validators: {
      onChange: loginSchema,
    },
    onSubmit: async ({ value }) => {
      // TODO: wire up to the real auth API once it's available.
      console.log("login", value)
    },
  })

  return (
    <AuthCard
      title="Iniciar sesión"
      description="Ingresá tus credenciales para acceder a tu cuenta."
      footer={
        <>
          <Button type="submit" form="login-form" className="w-full">
            Iniciar sesión
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            ¿No tenés cuenta?{" "}
            <Link to="/register" className="font-medium text-foreground underline-offset-4 hover:underline">
              Registrate
            </Link>
          </p>
        </>
      }
    >
      <Button
        type="button"
        variant="outline"
        className="w-full"
        onClick={() => {
          // TODO: wire up to the real Ciudadanos SSO flow once it's available.
          console.log("login with Ciudadanos")
        }}
      >
        <IconId />
        Iniciar sesión con Ciudadanos
      </Button>
      <div className="my-4 flex items-center gap-3">
        <Separator className="flex-1" />
        <span className="text-xs text-muted-foreground">O CONTINUÁ CON</span>
        <Separator className="flex-1" />
      </div>
      <form
        id="login-form"
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          form.handleSubmit()
        }}
      >
        <FieldGroup>
          <form.Field
            name="username"
            children={(field) => {
              const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={isInvalid}>
                  <FieldLabel htmlFor={field.name}>Usuario</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon>
                      <IconUser />
                    </InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(e) => field.handleChange(e.target.value)}
                      aria-invalid={isInvalid}
                      placeholder="ada.lovelace"
                      autoComplete="username"
                    />
                  </InputGroup>
                  {isInvalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }}
          />
          <form.Field
            name="password"
            children={(field) => {
              const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={isInvalid}>
                  <FieldLabel htmlFor={field.name}>Contraseña</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon>
                      <IconLock />
                    </InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      type={showPassword ? "text" : "password"}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(e) => field.handleChange(e.target.value)}
                      aria-invalid={isInvalid}
                      placeholder="••••••••"
                      autoComplete="current-password"
                    />
                    <InputGroupAddon align="inline-end">
                      <InputGroupButton
                        type="button"
                        size="icon-xs"
                        aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                        onClick={() => setShowPassword((value) => !value)}
                      >
                        {showPassword ? <IconEyeOff /> : <IconEye />}
                      </InputGroupButton>
                    </InputGroupAddon>
                  </InputGroup>
                  {isInvalid && <FieldError errors={field.state.meta.errors} />}
                </Field>
              )
            }}
          />
        </FieldGroup>
      </form>
    </AuthCard>
  )
}

export { LoginForm }
