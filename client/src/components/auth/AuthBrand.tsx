import { Link } from "@tanstack/react-router"

import { cn } from "@/lib/utils"
import AppLogoIconThemed from "../branding/AppLogoIconThemed"
import AppLogoTitleThemed from "../branding/AppLogoTitleThemed"

function AuthBrand({ className }: { className?: string }) {
  return (
    <Link
      to="/"
      className={cn("flex flex-1 w-full items-center gap-2 font-heading text-sm font-semibold", className)}>
      <AppLogoIconThemed className="h-8" />
      <AppLogoTitleThemed className="h-8" />
    </Link>
  )
}

export { AuthBrand }
