import { Link } from "@tanstack/react-router"
import { IconCube } from "@tabler/icons-react"

import { BRAND_NAME } from "@/components/auth/AuchBrand"
import { ThemeToggle } from "@/components/ThemeToggle"
import { Button } from "@/components/ui/button"

function SiteHeader() {
  return (
    <header className="fixed inset-x-3 top-3 z-50 mx-auto max-w-6xl rounded-full border border-border/50 bg-background/70 shadow-sm backdrop-blur-lg sm:inset-x-6 supports-backdrop-filter:bg-background/60">
      <div className="flex h-14 items-center justify-between pr-3 pl-3 sm:pr-3 sm:pl-5">
        <Link to="/" className="flex items-center gap-2 font-heading text-sm font-semibold">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <IconCube className="size-4.5" />
          </span>
          {BRAND_NAME}
        </Link>

        <div className="flex items-center gap-2">
          <ThemeToggle />
          <Button render={<Link to="/login" />}>Iniciar sesión</Button>
        </div>
      </div>
    </header>
  )
}

export { SiteHeader }
