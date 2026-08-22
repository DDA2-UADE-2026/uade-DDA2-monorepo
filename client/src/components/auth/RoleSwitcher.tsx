import { useState } from "react"
import { Link, useNavigate } from "@tanstack/react-router"
import { IconBuildingCommunity, IconCircleCheck, IconLogout, IconUsers, IconUserCog } from "@tabler/icons-react"

import { AuthCard } from "@/components/auth/AuthCard"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

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

type RoleId = (typeof ROLES)[number]["id"]

function RoleSwitcher() {
  const navigate = useNavigate()
  const [selected, setSelected] = useState<RoleId | null>(null)

  return (
    <AuthCard
      title="Elegí cómo continuar"
      description="Tu cuenta tiene acceso a más de un rol."
      icon={IconUserCog}
      className="sm:max-w-lg"
      footer={
        <>
          <Button
            className="w-full"
            disabled={!selected}
            onClick={() => {
              const role = ROLES.find((r) => r.id === selected)
              if (role) navigate({ to: role.to })
            }}
          >
            Continuar
          </Button>
          <Button variant="ghost" className="w-full" render={<Link to="/login" />}>
            <IconLogout />
            Cerrar sesión y cambiar de cuenta
          </Button>
        </>
      }
    >
      <div role="radiogroup" aria-label="Rol" className="grid gap-3 sm:grid-cols-2">
        {ROLES.map((role) => {
          const isSelected = selected === role.id
          return (
            <label
              key={role.id}
              data-selected={isSelected}
              className={cn(
                "group flex cursor-pointer flex-col gap-3 rounded-2xl border p-4 transition-colors",
                isSelected ? "border-primary bg-primary/5" : "border-input hover:bg-muted/50"
              )}
            >
              <input
                type="radio"
                name="role"
                value={role.id}
                checked={isSelected}
                onChange={() => setSelected(role.id)}
                className="sr-only"
              />
              {/* Placeholder art — swap for a real illustration/photo per role later. */}
              <div className="flex h-20 items-center justify-center rounded-xl bg-primary/10">
                <role.icon className="size-8 text-primary" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="flex items-center gap-2 text-sm font-medium">
                  {role.title}
                  {isSelected && <IconCircleCheck className="size-4 text-primary" />}
                </span>
                <span className="text-sm text-muted-foreground">{role.description}</span>
              </div>
            </label>
          )
        })}
      </div>
    </AuthCard>
  )
}

export { RoleSwitcher }
