import {
  IconCalendarEvent,
  IconHeartHandshake,
  IconHome2,
  IconHomeCheck,
  IconStethoscope,
  IconUser,
} from "@tabler/icons-react"

import { cn } from "@/lib/utils"

const MOCK_CASOS = [
  {
    icon: IconHeartHandshake,
    title: "Programa Primera Infancia",
    subtitle: "Comuna 4",
    status: "Aprobado",
    statusClass: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
  },
  {
    icon: IconHomeCheck,
    title: "Visita social domiciliaria",
    subtitle: "Vie 21/08 · 10:00",
    status: "Programada",
    statusClass: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
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
  return (
    <div className="relative mx-auto w-64 select-none sm:w-72">
      <div className="relative overflow-hidden rounded-[2.5rem] border-8 border-foreground/10 bg-card shadow-2xl ring-1 ring-border">
        <div className="absolute inset-x-0 top-0 z-10 flex justify-center pt-2">
          <div className="h-5 w-24 rounded-full bg-foreground/10" />
        </div>

        <div className="flex h-140 flex-col bg-background px-4 pt-10 pb-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-muted-foreground">Comuna 4</p>
              <p className="font-heading text-sm font-semibold">Mis programas</p>
            </div>
            <span className="flex size-9 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <IconUser className="size-4" />
            </span>
          </div>

          <div className="mt-6 flex flex-col gap-3">
            {MOCK_CASOS.map(({ icon: Icon, title, subtitle, status, statusClass }) => (
              <div key={title} className="flex items-center gap-3 rounded-2xl bg-card p-3 ring-1 ring-border">
                <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Icon className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{title}</p>
                  <p className="truncate text-xs text-muted-foreground">{subtitle}</p>
                </div>
                <span className={cn("shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium", statusClass)}>
                  {status}
                </span>
              </div>
            ))}
          </div>

          <div className="mt-auto grid grid-cols-4 gap-2 rounded-2xl bg-card p-2 ring-1 ring-border">
            {NAV_ICONS.map(({ icon: Icon, active }, index) => (
              <span
                key={index}
                className={cn(
                  "flex items-center justify-center rounded-xl py-2",
                  active ? "bg-primary/10 text-primary" : "text-muted-foreground"
                )}
              >
                <Icon className="size-4" />
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export { AppPhoneMock }
