import { useNavigate } from "@tanstack/react-router"
import { IconBuildingCommunity, IconChevronRight, IconLogout, IconUsers } from "@tabler/icons-react"

import { AuthCard } from "@/components/auth/AuthCard"
import { UserAvatar } from "@/components/UserAvatar"
import { Button } from "@/components/ui/button"
import { useLogout, useMe } from "@/hooks/use-auth"

// Fake roles for now — a real user could come back from the API with one
// role (skip this screen) or several (show only the ones they actually have).
const ROLES = [
  {
    id: "portal",
    title: "Portal ciudadano",
    description: "Trámites, reclamos y consultas para vecinos.",
    icon: IconUsers,
    to: "/portal",
  },
  {
    id: "gestion",
    title: "Gestión municipal",
    description: "Herramientas internas de gestión y auditoría.",
    icon: IconBuildingCommunity,
    to: "/gestion",
  },
] as const

function RoleSwitcher() {
  const navigate = useNavigate()
  const logout = useLogout()
  const { data } = useMe()
  const user = data?.user ?? { name: "Usuario" }

  return (
    <AuthCard
      title="Elegí cómo continuar"
      description="Tu cuenta tiene acceso a más de un rol."
      headerVisual={<UserAvatar addBlob user={user} className="size-16" fallbackClassName="text-lg" />}
      className="sm:max-w-lg"
      footer={
        <Button
          variant="destructive"
          className="w-full"
          onClick={() => {
            logout()
            navigate({ to: "/login" })
          }}
        >
          <IconLogout />
          Cerrar sesión y cambiar de cuenta
        </Button>
      }
    >
      <div className="flex flex-col gap-3">
        {ROLES.map((role) => (
          <button
            key={role.id}
            type="button"
            onClick={() => navigate({ to: role.to })}
            className="group flex items-center gap-4 rounded-2xl border border-input p-4 text-left transition-colors hover:border-primary hover:bg-primary/5"
          >
            <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/20">
              <role.icon className="size-6" />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <span className="text-sm font-medium">{role.title}</span>
              <span className="text-sm text-muted-foreground">{role.description}</span>
            </div>
            <IconChevronRight className="size-5 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
          </button>
        ))}
      </div>
    </AuthCard>
  )
}

export { RoleSwitcher }
