import { Link } from "@tanstack/react-router"
import { IconCube } from "@tabler/icons-react"

import { cn } from "@/lib/utils"

// Placeholder brand until the real product name/logo is ready.
const BRAND_NAME = "Acme Inc."

function AuthBrand({ className }: { className?: string }) {
  return (
    <Link to="/" className={cn("flex items-center gap-2 font-heading text-sm font-semibold", className)}>
      <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground">
        <IconCube className="size-5" />
      </span>
      {BRAND_NAME}
    </Link>
  )
}

export { AuthBrand, BRAND_NAME }
