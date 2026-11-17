import { useEffect, useState } from "react"
import { useForm } from "@tanstack/react-form"
import { Link, useNavigate } from "@tanstack/react-router"
import {
  IconAlertTriangle,
  IconAt,
  IconClockHour4,
  IconEye,
  IconEyeOff,
  IconId,
  IconInfoCircle,
  IconLoader2,
  IconLock,
  IconShieldCheck,
} from "@tabler/icons-react"

import { AuthCard } from "@/components/auth/AuthCard"
import { isM1WakingError, M1_LOGIN_MAX_ATTEMPTS, useM1Login } from "@/hooks/use-auth"
import { zM1SessionLoginRequestWritable } from "@/generated/zod.gen"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"
import { getRoleHome } from "@/lib/auth-route-guards"

/** CUIT/CUIL: 11 digits, optionally written as XX-XXXXXXXX-X. */
const CUIT_PATTERN = /^\d{2}[-\s]?\d{8}[-\s]?\d$/
const SLOW_AFTER_SECONDS = 4

function looksLikeCuit(value: string): boolean {
  return CUIT_PATTERN.test(value.trim())
}

/** Current time, refreshed every second while `active`. */
function useTickingNow(active: boolean): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (!active) return
    const id = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(id)
  }, [active])
  return now
}

function CitizenLoginForm() {
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const login = useM1Login()
  const now = useTickingNow(login.isPending)
  const elapsed = login.isPending ? (now - login.submittedAt) / 1000 : 0

  const form = useForm({
    defaultValues: {
      email: "",
      password: "",
    },
    validators: {
      onChange: zM1SessionLoginRequestWritable,
    },
    onSubmit: ({ value }) => {
      login.mutate(
        { body: { email: value.email.trim(), password: value.password } },
        {
          onSuccess: (data) => {
            if (data.user?.activeRole) {
              void navigate({ to: getRoleHome(data.user.activeRole), replace: true })
            }
          },
        },
      )
    },
  })

  const retrying = login.isPending && login.failureCount > 0
  const slow = login.isPending && !retrying && elapsed >= SLOW_AFTER_SECONDS
  const wakingFailed = login.isError && isM1WakingError(login.error)

  return (
    <AuthCard
      title="Ingresar con Ciudadanos"
      description="Usá el correo y la contraseña de tu cuenta en el portal Ciudadanos de la Municipalidad."
      icon={IconId}
      backTo="/login"
      footer={
        <>
          <Button type="submit" form="citizen-login-form" className="w-full" disabled={login.isPending}>
            {login.isPending ? (
              <>
                <IconLoader2 className="animate-spin" aria-hidden="true" />
                Verificando con Ciudadanos…
              </>
            ) : (
              "Ingresar"
            )}
          </Button>
          <p className="text-xs text-muted-foreground">
            Ingresá con el rol <span className="font-medium text-foreground">Ciudadano</span>.
          </p>
          <p className="text-xs text-muted-foreground -mt-3">
            ¿Sos personal municipal?{" "}
            <Link to="/login" className="font-medium text-foreground underline underline-offset-4">
              Usá el ingreso interno
            </Link>
            .
          </p>
          <TrustNote />
        </>
      }
    >
      <div role="status" aria-live="polite">
        {slow && (
          <Alert className="mb-4">
            <IconClockHour4 />
            <AlertTitle>Conectando con Ciudadanos</AlertTitle>
            <AlertDescription>
              Si el servicio estaba inactivo, puede tardar hasta un minuto en responder.
            </AlertDescription>
          </Alert>
        )}
        {retrying && (
          <Alert className="mb-4">
            <IconLoader2 className="animate-spin" />
            <AlertTitle>Ciudadanos se está iniciando</AlertTitle>
            <AlertDescription>
              Reintentamos automáticamente (intento {login.failureCount + 1} de {M1_LOGIN_MAX_ATTEMPTS}).
            </AlertDescription>
          </Alert>
        )}
      </div>
      {wakingFailed && (
        <Alert className="mb-4">
          <IconClockHour4 />
          <AlertTitle>Ciudadanos no respondió a tiempo</AlertTitle>
          <AlertDescription>
            <p>El servicio puede estar terminando de iniciarse. Esperá unos segundos y volvé a intentar.</p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => void form.handleSubmit()}
            >
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {login.isError && !wakingFailed && (
        <Alert variant="destructive" className="mb-4">
          <IconAlertTriangle />
          <AlertTitle>No pudimos iniciar sesión</AlertTitle>
          <AlertDescription>
            {login.error?.message ?? "Revisá tu correo y contraseña e intentá de nuevo."}
          </AlertDescription>
        </Alert>
      )}
      <form
        id="citizen-login-form"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        <FieldGroup>
          <form.Field
            name="email"
            children={(field) => {
              const isCuit = looksLikeCuit(field.state.value)
              const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
              return (
                <Field data-invalid={isInvalid || isCuit}>
                  <FieldLabel htmlFor={field.name}>Correo de Ciudadanos</FieldLabel>
                  <InputGroup>
                    <InputGroupAddon>
                      <IconAt />
                    </InputGroupAddon>
                    <InputGroupInput
                      id={field.name}
                      name={field.name}
                      type="email"
                      inputMode="email"
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(e) => field.handleChange(e.target.value)}
                      aria-invalid={isInvalid || isCuit}
                      aria-describedby={`${field.name}-hint`}
                      placeholder="nombre@ejemplo.com"
                      autoComplete="username"
                    />
                  </InputGroup>
                  {isCuit ? (
                    <p id={`${field.name}-hint`} className="flex items-start gap-1.5 text-sm text-destructive">
                      <IconInfoCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                      Eso parece un CUIT. Para este ingreso usá el correo que registraste en Ciudadanos.
                    </p>
                  ) : (
                    <>
                      <FieldDescription id={`${field.name}-hint`}>
                        Es el correo de tu cuenta, no tu CUIT ni tu DNI.
                      </FieldDescription>
                      {isInvalid && <FieldError errors={field.state.meta.errors} />}
                    </>
                  )}
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
                  <FieldLabel htmlFor={field.name}>Contraseña de Ciudadanos</FieldLabel>
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

function TrustNote() {
  return (
    <div className="flex w-full items-start gap-3 rounded-xl border bg-muted/40 p-3 text-left text-sm text-muted-foreground">
      <IconShieldCheck className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
      <div className="flex flex-col gap-1">
        <p>
          Por decisiones externas, tu contraseña pasa por nuestros servidores. <span className="font-medium text-foreground">Desarrollo Social no guarda tu contraseña</span>.
        </p>
      </div>
    </div>
  )
}

export { CitizenLoginForm }
