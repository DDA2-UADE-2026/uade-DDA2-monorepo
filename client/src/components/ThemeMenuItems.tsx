import { IconDeviceLaptop, IconMoon, IconSun } from '@tabler/icons-react'

import type { Theme } from '@/context/theme-context'
import {
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
} from '@/components/ui/dropdown-menu'
import { useTheme } from '@/hooks/use-theme'

const THEME_OPTIONS = [
  { value: 'light', label: 'Claro', icon: IconSun },
  { value: 'dark', label: 'Oscuro', icon: IconMoon },
  { value: 'system', label: 'Sistema', icon: IconDeviceLaptop },
] as const satisfies ReadonlyArray<{
  value: Theme
  label: string
  icon: typeof IconSun
}>

export function ThemeMenuItems() {
  const { setTheme, theme } = useTheme()

  return (
    <DropdownMenuRadioGroup value={theme} onValueChange={(value) => setTheme(value as Theme)}>
      {THEME_OPTIONS.map(({ value, label, icon: ThemeIcon }) => (
        <DropdownMenuRadioItem key={value} value={value}>
          <ThemeIcon />
          <span className="w-full min-w-18">{label}</span>
        </DropdownMenuRadioItem>
      ))}
    </DropdownMenuRadioGroup>
  )
}
