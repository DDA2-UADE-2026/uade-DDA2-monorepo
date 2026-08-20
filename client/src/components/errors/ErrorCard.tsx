import { useState } from 'react'
import { Link, type ErrorComponentProps } from '@tanstack/react-router'
import { IconAlertTriangle, IconChevronDown, IconHome, IconRefresh } from '@tabler/icons-react'

import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'

interface ErrorCardProps extends ErrorComponentProps {
  title?: string
  description?: string
  homeHref?: string
  homeLabel?: string
  className?: string
}

/**
 * Card body shared by every TanStack Router `errorComponent` in the app —
 * reads `error` / `info` / `reset` from `ErrorComponentProps`, so any route
 * can wire it in directly (or via a thin wrapper like `RouteErrorPage` /
 * `SectionErrorFallback`) and override `title`/`description`/`homeHref` for
 * that route's context.
 */
function ErrorCard({
  error,
  info,
  reset,
  title = 'Algo salió mal',
  description,
  homeHref = '/',
  homeLabel = 'Ir al inicio',
  className,
}: ErrorCardProps) {
  const [showTrace, setShowTrace] = useState(false)
  const trace = info?.componentStack ?? error.stack ?? String(error)

  return (
    <div className={cn('flex w-full flex-col items-center gap-3', className)}>
      <Card className="w-full">
        <CardHeader className="flex flex-col items-center gap-4 text-center">
          <span className="flex size-11 items-center justify-center rounded-xl bg-destructive/10 text-destructive">
            <IconAlertTriangle className="size-5" />
          </span>
          <div className="flex flex-col gap-1.5">
            <CardTitle className="text-xl">{title}</CardTitle>
            <CardDescription>{description ?? (error.message || 'Ocurrió un error inesperado.')}</CardDescription>
          </div>
        </CardHeader>

        <CardFooter className="flex flex-col gap-2">
          <Button className="w-full" onClick={reset}>
            <IconRefresh />
            Reintentar
          </Button>
          <Button className="w-full" variant="outline" render={<Link to={homeHref} />}>
            <IconHome />
            {homeLabel}
          </Button>
        </CardFooter>
      </Card>

      <Collapsible open={showTrace} onOpenChange={setShowTrace} className="w-full">
        <CollapsibleTrigger
          render={
            <Button variant="ghost" size="sm" className="w-full justify-center gap-1.5 text-muted-foreground/80" />
          }
        >
          Detalles del error
          <IconChevronDown className={cn('size-4 transition-transform', showTrace && 'rotate-180')} />
        </CollapsibleTrigger>
        <CollapsibleContent>
          <pre className="mt-2 whitespace-pre-wrap wrap-break-word bg-transparent p-2 text-left font-mono text-xs text-muted-foreground">
            <code>{trace}</code>
          </pre>
        </CollapsibleContent>
      </Collapsible>
    </div>
  )
}

export { ErrorCard }
export type { ErrorCardProps }
