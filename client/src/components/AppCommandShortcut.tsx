import { useEffect, useMemo, useState } from "react"
import { Link } from "@tanstack/react-router"
import {
  IconBriefcase,
  IconDeviceLaptop,
  IconKey,
  IconMoon,
  IconSun,
  IconUser,
} from "@tabler/icons-react"

import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import { ANALISIS_NAV, DEBUG_NAV, OPERACION_NAV, PORTAL_NAV } from "@/components/layout/sidebar-constants"
import { AUTH_NAV, commandActions, flattenNavItems, type CommandActionItem } from "@/lib/command-menu"
import { useTheme } from "@/hooks/use-theme"

const PORTAL_ITEMS = flattenNavItems(PORTAL_NAV)
const GESTION_ITEMS = flattenNavItems([...OPERACION_NAV, ...ANALISIS_NAV, ...DEBUG_NAV])

export default function AppCommandShortcut() {
  const [open, setOpen] = useState(false)
  const { setTheme } = useTheme()

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

  // Hook-dependent actions (theme, etc.) are built here; commandActions is the
  // static extension point other modules push plain actions into.
  const actions: CommandActionItem[] = useMemo(
    () => [
      { title: "Tema claro", icon: IconSun, onSelect: () => setTheme("light") },
      { title: "Tema oscuro", icon: IconMoon, onSelect: () => setTheme("dark") },
      { title: "Tema del sistema", icon: IconDeviceLaptop, onSelect: () => setTheme("system") },
      ...commandActions,
    ],
    [setTheme]
  )

  const runAction = (action: CommandActionItem) => {
    action.onSelect()
    setOpen(false)
  }

  return (
    <CommandDialog open={open} onOpenChange={setOpen}>
      <Command>
        <CommandInput placeholder="Buscar una página o acción..." />
        <CommandList>
          <CommandEmpty>No se encontraron resultados.</CommandEmpty>

          <CommandGroup heading="Acciones">
            {actions.map((action) => (
              <CommandItem
                key={action.title}
                value={action.title}
                keywords={action.keywords}
                onSelect={() => runAction(action)}
              >
                {action.icon ? <action.icon className="w-4 h-4" /> : null}
                <span>{action.title}</span>
              </CommandItem>
            ))}
          </CommandGroup>

          <CommandGroup heading="Portal ciudadano">
            {PORTAL_ITEMS.map((item) => (
              <CommandItem key={item.url} value={item.title} asChild onSelect={() => setOpen(false)}>
                <Link to={item.url} className="flex items-center gap-2">
                  <IconUser className="w-4 h-4" />
                  <span>{item.title}</span>
                </Link>
              </CommandItem>
            ))}
          </CommandGroup>

          <CommandGroup heading="Gestión municipal">
            {GESTION_ITEMS.map((item) => (
              <CommandItem key={item.url} value={item.title} asChild onSelect={() => setOpen(false)}>
                <Link to={item.url} className="flex items-center gap-2">
                  <IconBriefcase className="w-4 h-4" />
                  <span>{item.title}</span>
                </Link>
              </CommandItem>
            ))}
          </CommandGroup>

          <CommandGroup heading="Autenticación">
            {AUTH_NAV.map((item) => (
              <CommandItem key={item.url} value={item.title} asChild onSelect={() => setOpen(false)}>
                <Link to={item.url} className="flex items-center gap-2">
                  <IconKey className="w-4 h-4" />
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
