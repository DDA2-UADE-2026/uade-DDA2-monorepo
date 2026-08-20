import React from "react"
import { CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { Link } from "@tanstack/react-router"
import { Icon3dCubeSphere, IconKey as KeyRound, IconSquare as Square, IconUser as User } from "@tabler/icons-react"

const navigationItemsCustom = [
  {
    title: "Onboarding",
    url: "/auth/onboarding",
    icon: KeyRound, 
  },
  {
    title: "Crear Cuenta",
    url: "/auth/signup",
    icon: KeyRound,
  },
  {
    title: "Iniciar Sesión",
    url: "/auth/login",
    icon: KeyRound,
  },
  {
    title: "Perfil",
    url: "/dashboard/profile",
    icon: User,
  },
  {
    title: "Base Path",
    url: "/",
    icon: Square
  }
]

export function CommandMenu() {
  const [open, setOpen] = React.useState(false)

  React.useEffect(() => {
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
      <CommandInput placeholder="Escribe o busca un comando" />
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

        <CommandGroup heading="Links de navegación">
            <CommandItem asChild>
              <Link to="/" className="flex items-center gap-2">
                <Icon3dCubeSphere className="w-4 h-4" />
                <span>test</span>
              </Link>
            </CommandItem>
        </CommandGroup>
      </CommandList>
    </CommandDialog>
  )
}