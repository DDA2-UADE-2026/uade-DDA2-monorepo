import { IconMoon, IconSun, IconCheck, IconDeviceLaptop } from "@tabler/icons-react"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useTheme } from "@/hooks/use-theme"

export function ThemeToggle() {
  const { setTheme, theme } = useTheme()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={<Button variant="outline" size="icon" />}>
        <IconSun className="size-[1.2rem] scale-100 rotate-0 transition-all dark:scale-0 dark:-rotate-90" />
        <IconMoon className="absolute size-[1.2rem] scale-0 rotate-90 transition-all dark:scale-100 dark:rotate-0" />
        <span className="sr-only">Cambiar tema</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem data-sel={theme === "light"} className="rounded-lg" onClick={() => setTheme("light")}>
          <IconSun className="mr-2 text-inherit" />
          <span className="w-full min-w-18">Claro</span>
          {theme === "light" && <IconCheck className="ml-2 size-4 text-primary" />}
        </DropdownMenuItem>
        <DropdownMenuItem data-sel={theme === "dark"} className="rounded-lg" onClick={() => setTheme("dark")}>
          <IconMoon className="mr-2 text-inherit" />
          <span className="w-full min-w-18">Oscuro</span>
          {theme === "dark" && <IconCheck className="ml-2 size-4 text-primary" />}
        </DropdownMenuItem>
        <DropdownMenuItem className="rounded-lg min-w-24" onClick={() => setTheme("system")}>
          <IconDeviceLaptop className="mr-2 text-inherit" />
          <span className="w-full min-w-18">Sistema</span>
          {theme === "system" && <IconCheck className="ml-2 size-4 text-primary" />}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}