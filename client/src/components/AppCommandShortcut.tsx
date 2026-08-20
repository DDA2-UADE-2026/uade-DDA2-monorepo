import { useEffect, useState } from "react"
import { Command, CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { Link } from "@tanstack/react-router"
import { Icon3dCubeSphere, IconKey, IconUser } from "@tabler/icons-react"

const navigationItemsCustom = [
  {
    title: "Onboarding",
    url: "/auth/onboarding",
    icon: IconKey,
  },
  {
    title: "Crear Cuenta",
    url: "/auth/signup",
    icon: IconKey,
  },
  {
    title: "Iniciar Sesión",
    url: "/auth/login",
    icon: IconKey,
  },
  {
    title: "Perfil",
    url: "/dashboard/profile",
    icon: IconUser,
  },
  {
    title: "Base Path",
    url: "/",
    icon: Icon3dCubeSphere,
  }
]

export default function AppCommandShortcut() {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
        e.preventDefault()
        setOpen((open) => !open)
      }
    }
    document.addEventListener("keydown", down)
    return () => document.removeEventListener("keydown", down)
  }, [])

  return (
    <CommandDialog open={open} onOpenChange={setOpen}>
      <Command>
        <CommandInput placeholder="Type a command or search..." />
        <CommandList>
          <CommandEmpty>No results found.</CommandEmpty>
          <CommandGroup heading="Comandos de aplicación">
            {navigationItemsCustom.map((item) => (
              <CommandItem key={item.title} asChild>
                <Link to={item.url} className="flex items-center gap-2">
                  <item.icon className="w-4 h-4" />
                  <span>{item.title}</span>
                </Link>
              </CommandItem>
            ))}
          </CommandGroup>
        </CommandList>
      </Command>
    </CommandDialog>
  )
}