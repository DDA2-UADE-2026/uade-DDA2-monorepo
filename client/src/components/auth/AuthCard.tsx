import type { ReactNode } from "react"
import type { Icon } from "@tabler/icons-react"

import { AuthBrand } from "@/components/auth/AuthBrand"
import { ThemeToggle } from "@/components/ThemeToggle"
import { cn } from "@/lib/utils"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

interface AuthCardProps {
  title: string
  description?: string
  icon?: Icon
  headerVisual?: ReactNode
  children: ReactNode
  footer?: ReactNode
  className?: string
}

function AuthCard({ title, description, icon: TitleIcon, headerVisual, children, footer, className }: AuthCardProps) {
  return (
    <Card className={cn("relative w-full sm:max-w-md", className)}>
      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>
      <CardHeader className="flex flex-col items-center gap-7 text-center">
        <AuthBrand />
        <div className="flex flex-col items-center gap-3">
          {headerVisual ?? (TitleIcon ? (
            <div className="relative flex size-14 items-center justify-center bg-primary rounded-2xl">
              <div
                className="absolute inset-0 z-0 rounded-xl bg-blue-400 dark:bg-blue-500/60 blur-2xl"
                aria-hidden="true"
              />
              <TitleIcon className="size-7 text-white z-50" aria-hidden="true" />
            </div>
          ) : null)}
          <div className="flex flex-col gap-1">
            <CardTitle className="text-xl">{title}</CardTitle>
            {description ? <CardDescription>{description}</CardDescription> : null}
          </div>
        </div>
      </CardHeader>
      <CardContent>{children}</CardContent>
      {footer ? <CardFooter className="flex flex-col gap-4">{footer}</CardFooter> : null}
    </Card>
  )
}

export { AuthCard }
/*
          {TitleIcon ? (
            <div className="relative flex size-14 items-center justify-center bg-foreground/5 rounded-2xl">
              <div
                className="absolute inset-0 z-0 rounded-xl bg-blue-400 dark:bg-blue-500/60 blur-2xl"
                aria-hidden="true"
              />
              <TitleIcon className="size-7 text-foreground z-50" aria-hidden="true" />
            </div>
          ) : null}
*/
