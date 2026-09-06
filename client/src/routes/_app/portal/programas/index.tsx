import {
  IconAlertCircle,
  IconArrowRight,
  IconCalendarEvent,
  IconHeartHandshake,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { listAvailableProgramsOptions } from "@/generated/@tanstack/react-query.gen"

const PAGE_SIZE = 9
const PROGRAM_IMAGE = `${import.meta.env.BASE_URL}brand/og.png`
const PROGRAM_DESCRIPTION =
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Conocé los detalles y requisitos del programa."

const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
})

export const Route = createFileRoute("/_app/portal/programas/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function formatProgramDate(value?: string): string {
  if (!value) return "A confirmar"

  const [year, month, day] = value.split("-").map(Number)
  return new Intl.DateTimeFormat("es-AR", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(year, month - 1, day))
}

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const query = useQuery(
    listAvailableProgramsOptions({
      query: { page: page - 1, size: PAGE_SIZE },
    }),
  )
  const programs = query.data?.content ?? []
  const totalItems = Number(query.data?.totalElements ?? 0)
  const totalPages = Math.max(1, query.data?.totalPages ?? 1)
  const currentPage = Math.min(page, totalPages)

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Programas" }]} />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="flex w-full flex-col gap-6 p-4 lg:p-6">
            <header className="space-y-1">
              <h1 className="font-heading text-2xl font-medium tracking-tight">
                Programas disponibles
              </h1>
              <p className="text-sm text-muted-foreground">
                Encontrá programas sociales vigentes o próximos a comenzar.
              </p>
            </header>

            {query.isPending ? (
              <ProgramGridSkeleton />
            ) : query.isError ? (
              <Alert variant="destructive">
                <IconAlertCircle />
                <AlertTitle>No pudimos cargar los programas</AlertTitle>
                <AlertDescription>
                  Ocurrió un problema al consultar los programas disponibles.
                </AlertDescription>
                <AlertAction>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={query.isFetching}
                    onClick={() => query.refetch()}
                  >
                    Reintentar
                  </Button>
                </AlertAction>
              </Alert>
            ) : programs.length === 0 ? (
              <Empty className="min-h-80 border">
                <EmptyHeader>
                  <EmptyMedia variant="icon">
                    <IconHeartHandshake />
                  </EmptyMedia>
                  <EmptyTitle>No hay programas disponibles</EmptyTitle>
                  <EmptyDescription>
                    Volvé a consultar más adelante para conocer nuevas oportunidades.
                  </EmptyDescription>
                </EmptyHeader>
              </Empty>
            ) : (
              <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
                {programs.map((program) => {
                  const editionCount = program.availableEditions ?? 0

                  return (
                    <Card key={program.id ?? program.name} className="h-full transition-shadow hover:shadow-md">
                      <img
                        src={PROGRAM_IMAGE}
                        alt=""
                        className="aspect-[1.91/1] w-full object-cover"
                        loading="lazy"
                      />
                      <CardHeader>
                        <CardTitle className="text-lg">{program.name || "Programa sin nombre"}</CardTitle>
                        <CardDescription className="line-clamp-2">
                          {PROGRAM_DESCRIPTION}
                        </CardDescription>
                        <CardAction>
                          <Badge variant="secondary">
                            {editionCount} {editionCount === 1 ? "edición" : "ediciones"}
                          </Badge>
                        </CardAction>
                      </CardHeader>
                      <CardContent className="grid grid-cols-2 gap-3">
                        <ProgramDate
                          label="Comienza"
                          value={program.nextEditionStartDate}
                        />
                        <ProgramDate
                          label="Finaliza"
                          value={program.nextEditionEndDate}
                        />
                      </CardContent>
                      <CardFooter>
                        <Button
                          className="w-full"
                          variant="outline"
                          render={
                            <Link
                              to="/portal/programas/$programaId"
                              params={{ programaId: program.id ?? "" }}
                            />
                          }
                        >
                          Ver programa
                          <IconArrowRight />
                        </Button>
                      </CardFooter>
                    </Card>
                  )
                })}
              </div>
            )}
          </main>
        </div>

        {!query.isPending && !query.isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={(nextPage) => navigate({ search: { page: nextPage } })}
          />
        )}
      </SidebarShellContent>
    </SidebarShell>
  )
}

function ProgramDate({ label, value }: { label: string; value?: string }) {
  return (
    <div className="flex min-w-0 items-start gap-2 rounded-lg bg-muted/60 p-3">
      <IconCalendarEvent className="mt-0.5 size-4 shrink-0 text-primary" />
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="truncate text-xs font-medium">{formatProgramDate(value)}</p>
      </div>
    </div>
  )
}

function ProgramGridSkeleton() {
  return (
    <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3" aria-label="Cargando programas">
      {Array.from({ length: 6 }).map((_, index) => (
        <Card key={index} className="h-full">
          <Skeleton className="aspect-[1.91/1] w-full rounded-none" />
          <CardHeader>
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-4/5" />
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-3">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </CardContent>
          <CardFooter>
            <Skeleton className="h-9 w-full" />
          </CardFooter>
        </Card>
      ))}
    </div>
  )
}
