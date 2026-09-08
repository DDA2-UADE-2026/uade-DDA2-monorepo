import {
  IconBuildingCommunity,
  IconCheck,
  IconChevronRight,
  IconLogout,
  IconUsers,
} from "@tabler/icons-react"
import { useNavigate } from "@tanstack/react-router"

import { AuthCard } from "@/components/auth/AuthCard"
import { UserAvatar } from "@/components/UserAvatar"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { useLogout, useSelectRole, useSwitchRole } from "@/hooks/use-auth"
import { getRoleHome, type RoleChoice } from "@/lib/auth-route-guards"

type RoleSwitcherProps = {
  roleChoice: RoleChoice
}

const roleLabels: Record<string, string> = {
  ADMIN: "Administración",
  ADMINISTRATIVO: "Administrativo",
  AUDITOR: "Auditoría",
  CIUDADANO: "Portal ciudadano",
  COORDINADOR: "Coordinación",
  PROFESIONAL_CENTRO: "Profesional de centro",
  TRABAJADOR_SOCIAL: "Trabajo social",
  VIEWER: "Consulta municipal",
}

function RoleSwitcher({ roleChoice }: RoleSwitcherProps) {
  const navigate = useNavigate()
  const logout = useLogout()
  const selectRole = useSelectRole()
  const switchRole = useSwitchRole()
  const isInitialSelection = roleChoice.mode === "select"
  const user = isInitialSelection ? roleChoice.pending.user : roleChoice.user
  const roles = user.roles?.filter(Boolean) ?? []
  const activeRole = user.activeRole
  const mutation = isInitialSelection ? selectRole : switchRole
  const selectedRole = mutation.variables?.body.role

  const chooseRole = async (role: string) => {
    try {
      const data = isInitialSelection
        ? await selectRole.mutateAsync({
            body: {
              selectionToken: roleChoice.pending.selectionToken,
              role,
            },
          })
        : await switchRole.mutateAsync({ body: { role } })

      if (data.token && data.user?.activeRole) {
        navigate({ to: getRoleHome(data.user.activeRole), replace: true })
      }
    } catch {
      // TanStack Query exposes the API error below and keeps this screen active.
    }
  }

  const leaveSelector = () => {
    if (isInitialSelection) {
      logout()
      navigate({ to: "/login", replace: true })
      return
    }

    navigate({ to: getRoleHome(activeRole!), replace: true })
  }

  return (
    <AuthCard
      title="Elegí cómo continuar"
      description={isInitialSelection
        ? "Tu cuenta tiene varios roles. Elegí cuál querés usar en esta sesión."
        : "Elegí otro rol para actualizar tus permisos y el espacio de trabajo."}
      headerVisual={<UserAvatar addBlob user={user} className="size-16" fallbackClassName="text-lg" />}
      className="sm:max-w-lg"
      footer={
        <Button
          variant={isInitialSelection ? "destructive" : "outline"}
          className="w-full"
          disabled={mutation.isPending}
          onClick={leaveSelector}
        >
          {isInitialSelection && <IconLogout />}
          {isInitialSelection ? "Cancelar y volver al inicio de sesión" : "Conservar rol actual"}
        </Button>
      }
    >
      {mutation.isError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>No pudimos seleccionar el rol</AlertTitle>
          <AlertDescription>
            {mutation.error.message ?? "La selección no pudo completarse. Intentá nuevamente."}
          </AlertDescription>
        </Alert>
      )}

      <div className="flex flex-col gap-3">
        {roles.map((role) => {
          const normalizedRole = role.trim().toUpperCase()
          const isCitizen = normalizedRole === "CIUDADANO"
          const isCurrent = !isInitialSelection && role === activeRole
          const isSelecting = mutation.isPending && selectedRole === role
          const Icon = isCitizen ? IconUsers : IconBuildingCommunity

          return (
            <button
              key={role}
              type="button"
              disabled={mutation.isPending || isCurrent}
              onClick={() => chooseRole(role)}
              className="group flex items-center gap-4 rounded-2xl border border-input p-4 text-left transition-colors hover:border-primary hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/20">
                <Icon className="size-6" />
              </div>
              <div className="flex flex-1 flex-col gap-1">
                <span className="flex items-center gap-2 text-sm font-medium">
                  {roleLabels[normalizedRole] ?? formatRoleName(role)}
                  {isCurrent && <Badge variant="secondary">Actual</Badge>}
                </span>
                <span className="text-sm text-muted-foreground">
                  {isCitizen
                    ? "Trámites, programas y consultas del portal ciudadano."
                    : `Herramientas municipales habilitadas para el rol ${role}.`}
                </span>
              </div>
              {isSelecting
                ? <span className="text-xs text-muted-foreground">Ingresando…</span>
                : isCurrent
                  ? <IconCheck className="size-5 shrink-0 text-primary" />
                  : <IconChevronRight className="size-5 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />}
            </button>
          )
        })}
      </div>
    </AuthCard>
  )
}

function formatRoleName(role: string): string {
  return role
    .trim()
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}

export { RoleSwitcher }
