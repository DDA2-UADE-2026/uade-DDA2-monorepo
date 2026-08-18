import type { ReactNode } from "react"

import { AuthBrand } from "@/components/auth/auth-brand"
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
  children: ReactNode
  footer?: ReactNode
  className?: string
}

function AuthCard({ title, description, children, footer, className }: AuthCardProps) {
  return (
    <Card className={cn("w-full sm:max-w-md", className)}>
      <CardHeader className="flex flex-col items-center gap-4 text-center">
        <AuthBrand />
        <div className="flex flex-col gap-1.5">
          <CardTitle className="text-xl">{title}</CardTitle>
          {description ? <CardDescription>{description}</CardDescription> : null}
        </div>
      </CardHeader>
      <CardContent>{children}</CardContent>
      {footer ? <CardFooter className="flex flex-col gap-4">{footer}</CardFooter> : null}
    </Card>
  )
}

export { AuthCard }
