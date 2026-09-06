import {
  IconArrowUpRight,
  IconBell,
  IconCalendarEvent,
  IconHeartHandshake,
  IconHome2,
  IconMessageCircle,
  IconUser,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"

import { listAvailableProgramsOptions } from "@/generated/@tanstack/react-query.gen"
import { useMe } from "@/hooks/use-auth"
import { cn } from "@/lib/utils"

const NAV_ICONS = [
  { icon: IconHome2, active: true },
  { icon: IconHeartHandshake, active: false },
  { icon: IconCalendarEvent, active: false },
  { icon: IconUser, active: false },
]

const MAX_VISIBLE_PROGRAMS = 3

function formatProgramDate(value?: string) {
  if (!value) return "Fecha a confirmar"

  const [year, month, day] = value.split("-").map(Number)
  return new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "short" }).format(new Date(year, month - 1, day))
}

/** Phone-frame preview of the app's available programs screen. */
function AppPhoneMock() {
  const { data } = useMe()
  const programsQuery = useQuery(
    listAvailableProgramsOptions({ query: { page: 0, size: MAX_VISIBLE_PROGRAMS } }),
  )
  const user = data?.user
  const firstName = (user?.name || user?.username)?.split(" ")[0]
  const programs = (programsQuery.data?.content ?? []).slice(0, MAX_VISIBLE_PROGRAMS)
  const totalPrograms = programsQuery.data?.totalElements ?? programs.length

  return (
    <div
      className="relative mx-auto flex h-[39rem] w-full max-w-[31rem] select-none items-center justify-center sm:h-[42rem]"
      aria-label="Vista previa de la aplicación"
    >
      <div className="absolute inset-x-8 top-20 h-72 rounded-full bg-primary/20 blur-[80px] dark:bg-primary/25" />
      <div className="absolute inset-x-2 bottom-8 h-20 rounded-[50%] bg-foreground/10 blur-2xl" />

      <div className="absolute right-0 top-20 z-20 hidden w-48 rounded-2xl border border-white/40 bg-background/75 p-3.5 shadow-xl shadow-primary/10 backdrop-blur-xl sm:block dark:border-white/10">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm">
            <IconHeartHandshake className="size-5" />
          </span>
          <div>
            <p className="text-xs font-semibold text-foreground">Nuevas oportunidades</p>
            <p className="mt-0.5 text-[10px] leading-4 text-muted-foreground">Conocé los programas vigentes.</p>
          </div>
        </div>
      </div>

      <div className="absolute bottom-24 left-0 z-20 hidden w-44 rounded-2xl border border-white/40 bg-background/75 p-3.5 shadow-xl shadow-primary/10 backdrop-blur-xl sm:block dark:border-white/10">
        <div className="flex items-center gap-2.5">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <IconMessageCircle className="size-5" />
          </span>
          <div>
            <p className="text-xs font-semibold text-foreground">Te acompañamos</p>
            <p className="mt-0.5 text-[10px] text-muted-foreground">Ayuda en cada paso</p>
          </div>
        </div>
      </div>

      <div className="relative z-10 w-[17.5rem] -rotate-[2deg] rounded-[3.1rem] border border-white/30 bg-zinc-950 p-[7px] shadow-[0_40px_90px_-30px_rgba(30,64,175,0.45),0_24px_50px_-24px_rgba(0,0,0,0.55)] ring-1 ring-black/20 sm:w-[19.5rem] dark:border-white/15">
        <div className="relative overflow-hidden rounded-[2.7rem] bg-background ring-1 ring-white/10">
          <div className="absolute inset-x-0 top-0 z-30 flex justify-center pt-2">
            <div className="h-[1.35rem] w-[5.7rem] rounded-full bg-zinc-950" />
          </div>

          <div className="absolute inset-x-0 top-0 z-20 flex h-9 items-center justify-between px-6 text-[9px] font-semibold text-foreground">
            <span>9:41</span>
            <span className="flex items-center gap-1">
              <span className="h-2 w-3 rounded-[2px] border border-current opacity-70" />
              <span className="h-2 w-3 rounded-sm bg-current opacity-80" />
            </span>
          </div>

          <div className="flex h-[35rem] flex-col bg-[linear-gradient(180deg,color-mix(in_oklab,var(--primary)_8%,var(--background))_0%,var(--background)_34%)] px-4 pb-4 pt-12 sm:h-[38rem] sm:px-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[10px] font-medium text-primary">Hola, {firstName || "Sofía"}</p>
                <p className="font-heading text-base font-semibold tracking-tight">Municipalidad UADE</p>
              </div>
              <span className="relative flex size-9 items-center justify-center rounded-full bg-card text-muted-foreground shadow-sm ring-1 ring-border">
                <IconBell className="size-4" />
                <span className="absolute right-0 top-0 size-2 rounded-full bg-primary ring-2 ring-card" />
              </span>
            </div>

            <div className="relative mt-5 overflow-hidden rounded-[1.35rem] bg-primary p-4 text-primary-foreground shadow-lg shadow-primary/20">
              <div className="absolute -right-7 -top-8 size-28 rounded-full border-[18px] border-white/10" />
              <div className="absolute -bottom-9 right-12 size-20 rounded-full bg-white/5" />
              <div className="relative">
                <div className="flex items-center justify-between">
                  <p className="text-[10px] font-medium text-primary-foreground/70">PARA VOS</p>
                  <IconArrowUpRight className="size-4 text-primary-foreground/70" />
                </div>
                <p className="mt-2 text-2xl font-semibold tracking-tight">
                  {programsQuery.isPending || programsQuery.isError
                    ? "—"
                    : `${totalPrograms} programa${totalPrograms === 1 ? "" : "s"}`}
                </p>
                <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-black/15">
                  <div className="h-full w-full rounded-full bg-white" />
                </div>
                <p className="mt-2 text-[10px] text-primary-foreground/70">Vigentes y próximos a comenzar</p>
              </div>
            </div>

            <div className="mt-5 flex items-center justify-between">
              <p className="text-xs font-semibold">Programas disponibles</p>
              {totalPrograms > MAX_VISIBLE_PROGRAMS && <p className="text-[10px] font-medium text-primary">Ver todos</p>}
            </div>

            <div className="mt-2.5 flex flex-col gap-2.5">
              {programsQuery.isPending ? (
                Array.from({ length: MAX_VISIBLE_PROGRAMS }).map((_, index) => (
                  <div key={index} className="flex animate-pulse items-center gap-2.5 rounded-2xl bg-card/90 p-2.5 shadow-sm ring-1 ring-border/70">
                    <span className="size-9 shrink-0 rounded-xl bg-muted" />
                    <span className="flex flex-1 flex-col gap-1.5">
                      <span className="h-2.5 w-3/4 rounded bg-muted" />
                      <span className="h-2 w-1/2 rounded bg-muted" />
                    </span>
                  </div>
                ))
              ) : programsQuery.isError ? (
                <div className="rounded-2xl bg-card/90 p-3 text-center shadow-sm ring-1 ring-border/70">
                  <p className="text-[10px] font-medium">No pudimos cargar los programas.</p>
                  <button type="button" className="mt-1 text-[9px] font-semibold text-primary" onClick={() => programsQuery.refetch()}>
                    Reintentar
                  </button>
                </div>
              ) : programs.length === 0 ? (
                <div className="rounded-2xl bg-card/90 p-3 text-center shadow-sm ring-1 ring-border/70">
                  <p className="text-[10px] text-muted-foreground">No hay programas disponibles por el momento.</p>
                </div>
              ) : (
                programs.map((program) => {
                  const editionCount = program.availableEditions ?? 0

                  return (
                    <div key={program.id ?? program.name} className="flex items-center gap-2.5 rounded-2xl bg-card/90 p-2.5 shadow-sm ring-1 ring-border/70">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                        <IconHeartHandshake className="size-4" />
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-[11px] font-semibold">{program.name}</p>
                        <p className="truncate text-[9px] text-muted-foreground">
                          Próxima edición: {formatProgramDate(program.nextEditionStartDate)}
                        </p>
                      </div>
                      <span className="shrink-0 rounded-full bg-emerald-500/10 px-1.5 py-0.5 text-[8px] font-semibold text-emerald-700 dark:text-emerald-400">
                        {editionCount} {editionCount === 1 ? "edición" : "ediciones"}
                      </span>
                    </div>
                  )
                })
              )}
            </div>

            <div className="mt-auto grid grid-cols-4 gap-1 rounded-2xl bg-card/95 p-1.5 shadow-sm ring-1 ring-border/70">
              {NAV_ICONS.map(({ icon: Icon, active }, index) => (
                <span
                  key={index}
                  className={cn(
                    "flex items-center justify-center rounded-xl py-2",
                    active ? "bg-primary text-primary-foreground shadow-sm" : "text-muted-foreground"
                  )}
                >
                  <Icon className="size-4" />
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export { AppPhoneMock }
