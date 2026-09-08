import { useState } from "react"
import { useForm } from "@tanstack/react-form"
import { useNavigate } from "@tanstack/react-router"
import {
  IconAlertTriangle,
  IconEye,
  IconEyeOff,
  IconId,
  IconLock,
  IconLogin2,
  IconUser,
} from "@tabler/icons-react"

import { AuthCard } from "@/components/auth/AuthCard"
import { useLogin } from "@/hooks/use-auth"
import { zLoginRequestWritable } from "@/generated/zod.gen"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"
import { Separator } from "@/components/ui/separator"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { getRoleHome } from "@/lib/auth-route-guards"
import { getPendingRoleSelection } from "@/lib/role-selection"

function LoginForm() {
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const login = useLogin()

  const form = useForm({
    defaultValues: {
      username: "",
      password: "",
    },
    validators: {
      onChange: zLoginRequestWritable,
    },
    onSubmit: ({ value }) => {
      login.mutate(
        { body: value },
        {
          onSuccess: (data) => {
            if (data.token && data.user?.activeRole) {
              navigate({ to: getRoleHome(data.user.activeRole), replace: true })
              return
            }

            if (getPendingRoleSelection()) {
              navigate({ to: "/seleccionar-rol", replace: true })
            }
          },
        },
      )
    },
  })

  return (
    <AuthCard
      title="Iniciar sesión"
      description="Ingresá tus credenciales para acceder a tu cuenta."
      icon={IconLogin2}
      footer={
        <Button type="submit" form="login-form" className="w-full" disabled={login.isPending}>
          {login.isPending ? "Ingresando…" : "Iniciar sesión"}
        </Button>
      }
    >
      <Tooltip>
        <TooltipTrigger
          render={
            <Button
              type="button"
              variant="outline"
              className="w-full aria-disabled:opacity-50"
              aria-disabled="true"
            >
              <IconId />
              Iniciar sesión con Ciudadanos
            </Button>
          }
        />
        <TooltipContent>
          {/* TODO: remove once the real Ciudadanos SSO flow is available. */}
          Esta función no está disponible
        </TooltipContent>
      </Tooltip>
      <div className="my-4 flex items-center gap-3">
        <Separator className="flex-1" />
        <span className="text-xs text-muted-foreground">O CONTINUÁ CON</span>
        <Separator className="flex-1" />
      </div>
      {login.isError && (
        <Alert variant="destructive" className="mb-4">
          <IconAlertTriangle />
          <AlertTitle>No pudimos iniciar sesión</AlertTitle>
          <AlertDescription>
            {login.error.message ?? "Revisá tu usuario y contraseña e intentá de nuevo."}
          </AlertDescription>
        </Alert>
      )}
      <form
        id="login-form"
        noValidate
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
