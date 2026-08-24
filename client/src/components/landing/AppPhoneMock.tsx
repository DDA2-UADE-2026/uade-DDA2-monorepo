import {
  IconArrowUpRight,
  IconBell,
  IconCalendarEvent,
  IconCheck,
  IconHeartHandshake,
  IconHome2,
  IconHomeCheck,
  IconMessageCircle,
  IconStethoscope,
  IconUser,
} from "@tabler/icons-react"

import { useMe } from "@/hooks/use-auth"
import { cn } from "@/lib/utils"

const MOCK_CASOS = [
  {
    icon: IconHeartHandshake,
    title: "Programa Primera Infancia",
    subtitle: "Actualizado hoy",
    status: "Aprobado",
    statusClass: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  },
  {
    icon: IconHomeCheck,
    title: "Visita social domiciliaria",
    subtitle: "Vie 21 ago · 10:00",
    status: "Programada",
    statusClass: "bg-amber-500/10 text-amber-700 dark:text-amber-400",
  },
  {
    icon: IconStethoscope,
    title: "Turno salud comunitaria",
    subtitle: "CeSAC N.º 12",
    status: "Pendiente",
    statusClass: "bg-muted text-muted-foreground",
  },
]

const NAV_ICONS = [
  { icon: IconHome2, active: true },
  { icon: IconHeartHandshake, active: false },
  { icon: IconCalendarEvent, active: false },
  { icon: IconUser, active: false },
]

/** Decorative phone-frame mock of the app's "Mis programas sociales" screen. */
function AppPhoneMock() {
  const { data } = useMe()
  const user = data?.user
  const firstName = (user?.name || user?.username)?.split(" ")[0]

  return (
    <div
      className="relative mx-auto flex h-[39rem] w-full max-w-[31rem] select-none items-center justify-center sm:h-[42rem]"
      aria-label="Vista previa de la aplicación"
    >
      <div className="absolute inset-x-8 top-20 h-72 rounded-full bg-primary/20 blur-[80px] dark:bg-primary/25" />
      <div className="absolute inset-x-2 bottom-8 h-20 rounded-[50%] bg-foreground/10 blur-2xl" />

      <div className="absolute right-0 top-20 z-20 hidden w-48 rounded-2xl border border-white/40 bg-background/75 p-3.5 shadow-xl shadow-primary/10 backdrop-blur-xl sm:block dark:border-white/10">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-emerald-500 text-white shadow-sm">
            <IconCheck className="size-5" stroke={3} />
          </span>
          <div>
            <p className="text-xs font-semibold text-foreground">Solicitud aprobada</p>
            <p className="mt-0.5 text-[10px] leading-4 text-muted-foreground">Tu beneficio ya está activo.</p>
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
                <p className="font-heading text-base font-semibold tracking-tight">Tu espacio social</p>
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
                  <p className="text-[10px] font-medium text-primary-foreground/70">TU ACTIVIDAD</p>
                  <IconArrowUpRight className="size-4 text-primary-foreground/70" />
                </div>
                <p className="mt-2 text-2xl font-semibold tracking-tight">3 gestiones</p>
                <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-black/15">
                  <div className="h-full w-3/4 rounded-full bg-white" />
                </div>
                <p className="mt-2 text-[10px] text-primary-foreground/70">Todo está al día</p>
              </div>
            </div>

            <div className="mt-5 flex items-center justify-between">
              <p className="text-xs font-semibold">Mis gestiones</p>
              <p className="text-[10px] font-medium text-primary">Ver todas</p>
            </div>

            <div className="mt-2.5 flex flex-col gap-2.5">
              {MOCK_CASOS.map(({ icon: Icon, title, subtitle, status, statusClass }) => (
                <div key={title} className="flex items-center gap-2.5 rounded-2xl bg-card/90 p-2.5 shadow-sm ring-1 ring-border/70">
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <Icon className="size-4" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[11px] font-semibold">{title}</p>
                    <p className="truncate text-[9px] text-muted-foreground">{subtitle}</p>
                  </div>
                  <span className={cn("shrink-0 rounded-full px-1.5 py-0.5 text-[8px] font-semibold", statusClass)}>
                    {status}
                  </span>
                </div>
              ))}
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
