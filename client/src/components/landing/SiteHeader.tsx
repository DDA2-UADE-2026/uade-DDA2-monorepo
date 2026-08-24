import { Link } from "@tanstack/react-router"
import { IconArrowRight } from "@tabler/icons-react"
import AppLogoTitleThemed from "@/components/branding/AppLogoTitleThemed"
import { ThemeToggle } from "@/components/ThemeToggle"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { useMe } from "@/hooks/use-auth"
import { getUserInitials } from "@/lib/user-display"
import AppLogoIconThemed from "../branding/AppLogoIconThemed"

function SiteHeader() {
  const { data, isLoading } = useMe()
  const user = data?.user
  const displayName = user?.name || user?.username || "Usuario"

  return (
    <header className="fixed overflow-hidden inset-x-3 top-3 z-50 mx-auto max-w-6xl rounded-full border border-border bg-background/70 shadow-sm backdrop-blur-lg sm:inset-x-6 supports-backdrop-filter:bg-background/60">
      <div className="flex h-14 items-center justify-between pr-3 pl-3 sm:pr-3 sm:pl-4">
        <Link
          to="/"
          className={"relative flex items-center gap-2 sm:gap-3 font-heading text-sm font-semibold"}>
          <AppLogoIconThemed className="h-8 z-60" />
          <AppLogoTitleThemed className="h-8 z-60" />
          <span className="z-55 absolute -left-10 -top-6 h-12 dark:bg-blue-700 bg-blue-500/80 blur-2xl bottom-0 w-20"></span>
        </Link>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          {isLoading ? (
            <Button disabled className="pl-1.5">
              <Avatar size="sm">
                <AvatarFallback className="animate-pulse bg-primary-foreground/15" />
              </Avatar>
              Verificando…
            </Button>
          ) : user ? (
            <Button className="pl-1.5" render={<Link to="/portal" />}>
              <Avatar size="sm" aria-label={displayName}>
                <AvatarFallback className="bg-primary-foreground/15 text-primary-foreground">
                  {getUserInitials(displayName)}
                </AvatarFallback>
              </Avatar>
              Ir al portal
              <IconArrowRight />
            </Button>
          ) : (
            <Button render={<Link to="/login" />}>Iniciar sesión</Button>
          )}
        </div>
      </div>
    </header>
  )
}

export { SiteHeader }
