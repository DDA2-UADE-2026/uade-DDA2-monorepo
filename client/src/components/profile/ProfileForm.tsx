import { useForm } from "@tanstack/react-form"
import { IconBuilding, IconMail, IconShieldCheck, IconUser } from "@tabler/icons-react"

import { profileSchema } from "@/components/profile/schemas"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"

// Mock hasta que haya sesión real — mismos datos que SidebarUser.tsx.
const MOCK_USER = {
  name: "Usuario",
  email: "usuario@municipio.gob.ar",
  initials: "US",
  role: "Trabajador/a social",
  municipio: "Municipalidad UADE",
}

function ProfileForm() {
  const form = useForm({
    defaultValues: {
      name: MOCK_USER.name,
      email: MOCK_USER.email,
    },
    validators: {
      onChange: profileSchema,
    },
    onSubmit: async ({ value }) => {
      // TODO: wire up to the real profile API once it's available.
      console.log("update profile", value)
    },
  })

  return (
    <Card className="relative w-full sm:max-w-md">
      <CardHeader className="flex flex-col items-center gap-4 text-center">
        <Avatar className="size-16">
          <AvatarFallback className="text-lg">{MOCK_USER.initials}</AvatarFallback>
        </Avatar>
        <div className="flex flex-col gap-1.5">
          <CardTitle className="text-xl">Mi perfil</CardTitle>
          <CardDescription>Información de tu cuenta.</CardDescription>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-col gap-2 rounded-2xl border border-border bg-muted/30 p-3.5 text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <IconShieldCheck className="size-4 shrink-0" />
            {MOCK_USER.role}
          </div>
          <div className="flex items-center gap-2">
            <IconBuilding className="size-4 shrink-0" />
            {MOCK_USER.municipio}
          </div>
        </div>

        <form
          id="profile-form"
          onSubmit={(e) => {
            e.preventDefault()
            e.stopPropagation()
            form.handleSubmit()
          }}
        >
          <FieldGroup>
            <form.Field
              name="name"
              children={(field) => {
                const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>Nombre completo</FieldLabel>
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
                        autoComplete="name"
                      />
                    </InputGroup>
                    {isInvalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                )
              }}
            />
            <form.Field
              name="email"
              children={(field) => {
                const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
                return (
                  <Field data-invalid={isInvalid}>
                    <FieldLabel htmlFor={field.name}>Correo electrónico</FieldLabel>
                    <InputGroup>
                      <InputGroupAddon>
                        <IconMail />
                      </InputGroupAddon>
                      <InputGroupInput
                        id={field.name}
                        name={field.name}
                        type="email"
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                        aria-invalid={isInvalid}
                        autoComplete="email"
                      />
                    </InputGroup>
                    {isInvalid && <FieldError errors={field.state.meta.errors} />}
                  </Field>
                )
              }}
            />
          </FieldGroup>
        </form>
      </CardContent>
      <CardFooter>
        <Button type="submit" form="profile-form" className="w-full">
          Guardar cambios
        </Button>
      </CardFooter>
    </Card>
  )
}

export { ProfileForm }
