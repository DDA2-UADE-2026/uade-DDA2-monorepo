import { Link, useCanGoBack, useRouter } from '@tanstack/react-router'
import { IconArrowLeft, IconHome, IconMapSearch } from '@tabler/icons-react'

import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'

interface NotFoundCardProps {
  title?: string
  description?: string
  /** Path that didn't match, shown under the card so the user can spot a typo. */
  path?: string
  homeHref?: string
  homeLabel?: string
  className?: string
}

/**
 * Card body shared by every TanStack Router `notFoundComponent` in the app —
 * the not-found counterpart of `ErrorCard`. Wired in through
 * `RouteNotFoundPage`, or usable on its own inside a custom shell; each route
 * overrides `homeHref`/`homeLabel` so "volver" lands in the right section.
 */
function NotFoundCard({
  title = 'Página no encontrada',
  description = 'La dirección a la que quisiste entrar no existe o ya no está disponible.',
  path,
  homeHref = '/',
  homeLabel = 'Ir al inicio',
  className,
}: NotFoundCardProps) {
  const router = useRouter()
  const canGoBack = useCanGoBack()

  return (
    <div className={cn('flex w-full flex-col items-center gap-3', className)}>
      <Card className="w-full">
        <CardHeader className="flex flex-col items-center gap-4 text-center">
          <span className="flex size-11 items-center justify-center rounded-xl bg-muted text-muted-foreground">
            <IconMapSearch className="size-5" />
          </span>
          <div className="flex flex-col gap-1.5">
            <CardTitle className="text-xl">{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </div>
        </CardHeader>

        <CardFooter className="flex flex-col gap-2">
          {canGoBack ? (
            <Button className="w-full" onClick={() => router.history.back()}>
              <IconArrowLeft />
              Volver atrás
            </Button>
          ) : null}
          <Button
            className="w-full"
            variant={canGoBack ? 'outline' : 'default'}
            render={<Link to={homeHref} />}
          >
            <IconHome />
            {homeLabel}
          </Button>
        </CardFooter>
      </Card>

      {path ? (
        <p className="w-full truncate text-center font-mono text-xs text-muted-foreground/80">{path}</p>
      ) : null}
    </div>
  )
}

export { NotFoundCard }
export type { NotFoundCardProps }
