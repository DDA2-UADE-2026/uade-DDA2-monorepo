import type { CSSProperties } from "react"
import { IconArrowRight, IconCalendarEvent } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { SectionHeading } from "@/components/layout/SectionHeading"
import { ProgramThumbnail } from "@/components/programs/ProgramThumbnail"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { listAvailableProgramsOptions } from "@/generated/@tanstack/react-query.gen"
import type { AvailableProgramListItemResponse } from "@/generated/types.gen"
import { formatProgramDate } from "@/lib/program-dates"
import { cn } from "@/lib/utils"

const TICKER_SIZE = 12
const SECONDS_PER_PROGRAM = 7
const PROGRAM_DESCRIPTION_FALLBACK = "El objetivo de este programa se informará próximamente."
// Con pocos programas el track queda más angosto que la pantalla y el bucle
// mostraría un hueco, así que se repite la tanda hasta cubrirla.
const MIN_TICKER_ITEMS = 8

function buildTickerLoop(
  programs: Array<AvailableProgramListItemResponse>,
): Array<AvailableProgramListItemResponse> {
  if (programs.length === 0) return programs

  const repeats = Math.ceil(MIN_TICKER_ITEMS / programs.length)
  return Array.from({ length: repeats }, () => programs).flat()
}

export function ProgramTicker({ className }: { className?: string }) {
  const query = useQuery(
    listAvailableProgramsOptions({ query: { page: 0, size: TICKER_SIZE } }),
  )
  const programs = buildTickerLoop(query.data?.content ?? [])

  // Es una sección de descubrimiento: si falla o no hay nada que mostrar,
  // desaparece en lugar de ocupar la home con un error.
  if (query.isError || (!query.isPending && programs.length === 0)) return null

  return (
    <section aria-labelledby="program-ticker-title" className={cn("space-y-3", className)}>
      <SectionHeading
        id="program-ticker-title"
        title="Programas disponibles"
        description="Descubrí las oportunidades vigentes y conocé sus requisitos."
        action={
          <Button
            size="sm"
            variant="ghost"
            className="shrink-0"
            render={<Link to="/portal/programas" />}
          >
            Ver todos
            <IconArrowRight />
          </Button>
        }
      />

      {query.isPending ? (
        <ProgramTickerSkeleton />
      ) : (
        <div
          className={cn(
            "group/ticker relative overflow-hidden py-2 [--marquee-gap:1rem]",
            "mask-[linear-gradient(to_right,transparent,black_2rem,black_calc(100%-2rem),transparent)]",
            "motion-reduce:overflow-x-auto motion-reduce:mask-none",
          )}
          style={{ "--marquee-duration": `${programs.length * SECONDS_PER_PROGRAM}s` } as CSSProperties}
        >
          <div
            className={cn(
              "flex w-max animate-marquee gap-(--marquee-gap)",
              "group-hover/ticker:paused group-focus-within/ticker:paused",
              "motion-reduce:animate-none",
            )}
          >
            <ProgramTickerGroup programs={programs} />
            <ProgramTickerGroup programs={programs} duplicate />
          </div>
        </div>
      )}
    </section>
  )
}

function ProgramTickerGroup({
  programs,
  duplicate = false,
}: {
  programs: Array<AvailableProgramListItemResponse>
  duplicate?: boolean
}) {
  return (
    <ul
      // La segunda copia sólo existe para que el bucle sea continuo.
      aria-hidden={duplicate || undefined}
      className={cn("flex shrink-0 items-stretch gap-(--marquee-gap)", duplicate && "motion-reduce:hidden")}
    >
      {programs.map((program, index) => (
        <li key={`${program.id ?? program.name}-${index}`} className="flex">
          <ProgramTickerCard program={program} tabIndex={duplicate ? -1 : undefined} />
        </li>
      ))}
    </ul>
  )
}

function ProgramTickerCard({
  program,
  tabIndex,
}: {
  program: AvailableProgramListItemResponse
  tabIndex?: number
}) {
  const editionCount = program.availableEditions ?? 0
  const startLabel = program.nextEditionStartDate
    ? `Desde ${formatProgramDate(program.nextEditionStartDate)}`
    : "Fecha a confirmar"

  return (
    <Link
      to="/portal/programas/$programaId"
      params={{ programaId: program.id ?? "" }}
      tabIndex={tabIndex}
      className="relative overflow-hidden flex w-80 items-center gap-3 rounded-2xl bg-card p-2 ring-1 ring-foreground/10 transition-shadow hover:shadow-md hover:ring-primary/30"
    >
      <ProgramThumbnail imageUrl={program.imageUrl} className="size-16" />
      <div className="min-w-0 flex-1 pr-1 flex flex-col h-full">
        <span className="flex-1 flex-col flex">
          <p className="truncate font-heading text-sm font-medium capitalize">
            {program.name || "Programa sin nombre"}
          </p>
          <p className="line-clamp-1 text-xs text-muted-foreground">
            {program.objective || PROGRAM_DESCRIPTION_FALLBACK}
          </p>
        </span>
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-[11px]! flex gap-1 items-center">
            <IconCalendarEvent className="size-3!" />
            <span >{startLabel}</span>
            <span className="opacity-60">({editionCount} edición{editionCount === 1 ? "" : "es"})</span>
          </span>
          {editionCount > 0 && (
            <span className="text-xs text-muted-foreground hidden">
              {editionCount} {editionCount === 1 ? "edición" : "ediciones"}
            </span>
          )}
        </div>
      </div>
    </Link>
  )
}

function ProgramTickerSkeleton() {
  return (
    <div className="flex gap-4 overflow-hidden py-2" aria-label="Cargando programas">
      {Array.from({ length: 4 }).map((_, index) => (
        <div
          key={index}
          className="flex w-80 shrink-0 items-center gap-3 rounded-2xl bg-card p-2 ring-1 ring-foreground/10"
        >
          <Skeleton className="size-16 shrink-0 rounded-xl" />
          <div className="min-w-0 flex-1 space-y-2 pr-1">
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-5 w-1/2 rounded-full" />
          </div>
        </div>
      ))}
    </div>
  )
}
