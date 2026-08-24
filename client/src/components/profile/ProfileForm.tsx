import { Link } from '@tanstack/react-router'
import { IconAt, IconMail, IconShieldCheck, IconUser } from '@tabler/icons-react'

import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { InputGroup, InputGroupAddon, InputGroupInput } from '@/components/ui/input-group'
import { Skeleton } from '@/components/ui/skeleton'
import type { UserResponse } from '@/generated/types.gen'
import { useMe } from '@/hooks/use-auth'
import { cn } from '@/lib/utils'
import { getUserInitials } from '@/lib/user-display'

export type ProfileDisplayType = 'portal' | 'gestion'

function ProfileSkeleton({ displayType }: { displayType: ProfileDisplayType }) {
  return (
    <Card
      className={cn('relative w-full', displayType === 'gestion' ? 'sm:max-w-xl' : 'sm:max-w-md')}
      aria-label="Cargando perfil"
    >
      <CardHeader className="flex flex-col items-center gap-4">
        <Skeleton className="size-16 rounded-full" />
        <div className="flex w-full flex-col items-center gap-2">
          <Skeleton className="h-6 w-28" />
          <Skeleton className="h-4 w-44" />
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <Skeleton className="h-20 w-full rounded-2xl" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </CardContent>
    </Card>
  )
}

function UnavailableProfile() {
  return (
    <Card className="relative w-full sm:max-w-md">
      <CardHeader className="items-center text-center">
        <span className="mb-2 flex size-12 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
          <IconShieldCheck className="size-6" />
        </span>
        <CardTitle>No pudimos cargar tu perfil</CardTitle>
        <CardDescription>
          Tu sesión puede haber vencido. Volvé a iniciar sesión para continuar.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button className="w-full" render={<Link to="/login" />}>
          Ir al inicio de sesión
        </Button>
      </CardContent>
    </Card>
  )
}

function AccessList({ title, values, emptyLabel }: { title: string; values: string[]; emptyLabel: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{title}</p>
      <ul className="mt-2 flex flex-wrap gap-2" aria-label={title}>
        {values.length ? (
          values.map((value) => (
            <li
              key={value}
              className="rounded-lg border border-border bg-background px-2.5 py-1 font-mono text-[11px] text-foreground"
            >
              {value}
            </li>
          ))
        ) : (
          <li className="text-sm text-muted-foreground">{emptyLabel}</li>
        )}
      </ul>
    </div>
  )
}

function AuthenticatedProfile({
  user,
  displayType,
}: {
  user: UserResponse
  displayType: ProfileDisplayType
}) {
  const displayName = user.name || user.username || 'Usuario'
  const email = user.email ?? ''
  const username = user.username ? `@${user.username}` : 'Sin nombre de usuario'
  const userRoles = user.roles ?? []
  const roles = userRoles.length ? userRoles.join(' · ') : 'Sin roles asignados'
  const permissions = user.permissions ?? []

  return (
    <Card className={cn('relative w-full', displayType === 'gestion' ? 'sm:max-w-xl' : 'sm:max-w-md')}>
      <CardHeader className="flex flex-col items-center gap-4 text-center">
        <Avatar className="size-16">
          <AvatarFallback className="text-lg">{getUserInitials(displayName)}</AvatarFallback>
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
            <span>{roles}</span>
          </div>
          <div className="flex items-center gap-2">
            <IconAt className="size-4 shrink-0" />
            <span>{username}</span>
          </div>
        </div>

        <FieldGroup>
          <Field>
            <FieldLabel htmlFor="profile-name">Nombre completo</FieldLabel>
            <InputGroup>
              <InputGroupAddon>
                <IconUser />
              </InputGroupAddon>
              <InputGroupInput
                id="profile-name"
                name="name"
                value={displayName}
                readOnly
                aria-readonly="true"
                autoComplete="name"
              />
            </InputGroup>
          </Field>
          <Field>
            <FieldLabel htmlFor="profile-email">Correo electrónico</FieldLabel>
            <InputGroup>
              <InputGroupAddon>
                <IconMail />
              </InputGroupAddon>
              <InputGroupInput
                id="profile-email"
                name="email"
                type="email"
                value={email}
                readOnly
                aria-readonly="true"
                autoComplete="email"
              />
            </InputGroup>
          </Field>
        </FieldGroup>

        {displayType === 'gestion' && (
          <div className="grid gap-4 rounded-2xl border border-border bg-muted/20 p-4 sm:grid-cols-2">
            <AccessList title="Roles" values={userRoles} emptyLabel="Sin roles asignados" />
            <AccessList title="Permisos" values={permissions} emptyLabel="Sin permisos asignados" />
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function ProfileForm({ displayType }: { displayType: ProfileDisplayType }) {
  const { data, isError, isLoading } = useMe()

  if (isLoading) return <ProfileSkeleton displayType={displayType} />
  if (isError || !data?.user) return <UnavailableProfile />

  return <AuthenticatedProfile user={data.user} displayType={displayType} />
}

export { ProfileForm }
