import { Suspense, lazy } from "react"
import { createFileRoute, Link } from "@tanstack/react-router"
import {
  IconArrowRight,
  IconCircleCheckFilled,
  IconClipboardCheck,
  IconHeartHandshake,
  IconHomeCheck,
  IconLock,
  IconMap,
  IconStethoscope,
  IconUserPlus,
  IconUsersGroup,
} from "@tabler/icons-react"

import { AppPhoneMock } from "@/components/landing/AppPhoneMock"
import { SiteHeader } from "@/components/landing/SiteHeader"
import { BackendStatusPill } from "@/components/BackendStatusPill"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { useIpLocation } from "@/hooks/use-ip-city"

const SideRaysBackground = lazy(() => import("@/components/visual/SideRaysBackground"))

export const Route = createFileRoute("/")({
  component: RouteComponent,
})

const FEATURES = [
  {
    icon: IconHeartHandshake,
    title: "Programas y beneficios",
    description: "Consultá programas activos, requisitos y cupos, y postulate a convocatorias abiertas en tu comuna.",
  },
  {
    icon: IconHomeCheck,
    title: "Visitas y evaluación social",
    description: "Coordiná entrevistas y visitas domiciliarias para evaluar prioridad y situación de vulnerabilidad.",
  },
  {
    icon: IconStethoscope,
    title: "Salud comunitaria y turnos",
    description: "Sacá turno en centros municipales y sumate a campañas, talleres y actividades comunitarias.",
  },
]

const STEPS = [
  {
    icon: IconUserPlus,
    title: "Registrate como ciudadano",
    description: "Validamos tu identidad, grupo familiar y domicilio antes de continuar.",
  },
  {
    icon: IconClipboardCheck,
    title: "Solicitá un programa",
    description: "Elegí una convocatoria abierta y presentá la documentación requerida.",
  },
  {
    icon: IconHomeCheck,
    title: "Evaluación y visita social",
    description: "Un trabajador social evalúa tu prioridad, con visita domiciliaria si corresponde.",
  },
  {
    icon: IconHeartHandshake,
    title: "Recibí tu beneficio",
    description: "Asignamos, hacemos seguimiento y renovamos tu beneficio según tu situación.",
  },
]

const AUDIENCES = [
  { icon: IconUsersGroup, label: "Vecinos y grupos familiares" },
  { icon: IconHomeCheck, label: "Trabajadores sociales" },
  { icon: IconStethoscope, label: "Profesionales de centros municipales" },
  { icon: IconClipboardCheck, label: "Coordinadores y auditores" },
]

function RouteComponent() {
  const { city: ipCity } = useIpLocation()

  return (
    <div className="min-h-svh bg-background">
      <SiteHeader />

      <section className="relative flex min-h-svh items-center justify-center overflow-hidden px-6 pb-16 pt-28 sm:pb-20 sm:pt-32">
        <div className="absolute inset-0 z-0 pointer-events-none opacity-45 dark:opacity-100">
          <Suspense fallback={null}>
            <SideRaysBackground
              speed={2.5}
              rayColor1="#2b7fff"
              rayColor2="#3c3cfa"
              intensity={2}
              spread={2}
              origin="top-right"
              tilt={0}
              saturation={1.5}
              blend={0.75}
              falloff={1.6}
              opacity={1}
            />
          </Suspense>
        </div>

        <div className="relative z-10 mx-auto grid w-full max-w-6xl items-center gap-14 lg:grid-cols-[1.02fr_0.98fr] lg:gap-8 xl:gap-16">
          <div className="flex flex-col items-center text-center lg:items-start lg:text-left">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/15 bg-background/65 px-3 py-1.5 text-xs font-medium text-foreground/75 shadow-sm backdrop-blur-md">
              <IconMap className="size-3.5 shrink-0 " />
              Conectado cerca de {ipCity}
            </span>

            <h1 className="mt-6 max-w-3xl text-balance font-heading text-[2.65rem] font-semibold leading-[1.05] tracking-[-0.045em] sm:text-6xl lg:text-[4.25rem]">
              La ayuda que necesitás,{" "}
              <span className="relative whitespace-nowrap text-primary">
                sin vueltas
                <svg
                  aria-hidden="true"
                  className="absolute -bottom-2 left-0 h-3 w-full text-primary/25"
                  viewBox="0 0 240 12"
                  preserveAspectRatio="none"
                >
                  <path d="M2 9C62 2 169 2 238 8" fill="none" stroke="currentColor" strokeWidth="5" strokeLinecap="round" />
                </svg>
              </span>
            </h1>

            <p className="mt-7 max-w-xl text-balance text-base leading-7 text-muted-foreground sm:text-lg sm:leading-8">
              Encontrá programas sociales, presentá tu solicitud y seguí cada avance desde un solo lugar.
              Simple, claro y disponible cuando lo necesites.
            </p>

            <div className="mt-8 flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
              <Button className="h-12 rounded-xl px-6 shadow-lg shadow-primary/20" size="lg" render={<Link to="/register" />}>
                Solicitar un programa
                <IconArrowRight />
              </Button>
              <Button className="h-12 rounded-xl border-foreground/10 bg-background/50 px-6 backdrop-blur-sm" size="lg" variant="outline" render={<a href="#como-funciona" />}>
                Cómo funciona
              </Button>
            </div>

            <div className="mt-8 flex flex-wrap items-center justify-center gap-x-5 gap-y-2 text-xs font-medium text-muted-foreground lg:justify-start">
              <span className="flex items-center gap-1.5">
                <IconCircleCheckFilled className="size-4 text-emerald-500" />
                Trámite 100% online
              </span>
              <span className="flex items-center gap-1.5">
                <IconCircleCheckFilled className="size-4 text-emerald-500" />
                Seguimiento en tiempo real
              </span>
              <span className="flex items-center gap-1.5">
                <IconLock className="size-4 text-primary" />
                Tus datos protegidos
              </span>
            </div>
          </div>

          <AppPhoneMock />
        </div>
      </section>

      <section className="relative overflow-hidden px-6 py-24 sm:py-28">
        <div className="pointer-events-none absolute -left-40 top-24 size-80 rounded-full bg-primary/5 blur-3xl" />
        <div className="relative mx-auto max-w-6xl">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Todo conectado</p>
            <h2 className="mt-4 text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
              Acompañamiento social de principio a fin
            </h2>
            <p className="mt-4 text-balance leading-7 text-muted-foreground">
              Programas, visitas domiciliarias y salud comunitaria en una experiencia simple y transparente.
            </p>
          </div>

          <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(({ icon: Icon, title, description }, index) => (
              <Card
                key={title}
                className="group relative overflow-hidden rounded-3xl border-foreground/8 bg-card/70 py-0 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-primary/20 hover:shadow-xl hover:shadow-primary/5"
              >
                <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/35 to-transparent opacity-0 transition-opacity group-hover:opacity-100" />
                <CardHeader className="gap-0 p-6 sm:p-7">
                  <div className="flex items-start justify-between">
                    <span className="flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary transition-transform duration-300 group-hover:scale-105">
                      <Icon className="size-6" />
                    </span>
                    <span className="font-heading text-xs font-semibold text-muted-foreground/50">
                      0{index + 1}
                    </span>
                  </div>
                  <CardTitle className="mt-6 text-lg">{title}</CardTitle>
                  <CardDescription className="mt-2 text-sm leading-6">{description}</CardDescription>
                  <span className="mt-6 inline-flex items-center gap-1.5 text-xs font-semibold text-primary opacity-80">
                    <IconCircleCheckFilled className="size-3.5" />
                    Todo desde tu cuenta
                  </span>
                </CardHeader>
              </Card>
            ))}
          </div>

          <div className="mt-10 rounded-3xl border border-foreground/8 bg-muted/35 p-5 sm:p-6">
            <div className="flex flex-col items-center gap-5 lg:flex-row lg:justify-between">
              <div className="text-center lg:text-left">
                <p className="text-sm font-semibold text-foreground">Una plataforma para toda la comunidad</p>
                <p className="mt-1 text-xs text-muted-foreground">Cada persona ve las herramientas que necesita.</p>
              </div>
              <div className="flex flex-wrap items-center justify-center gap-2">
                {AUDIENCES.map(({ icon: Icon, label }) => (
                  <span
                    key={label}
                    className="flex items-center gap-2 rounded-full border border-foreground/8 bg-background/75 px-3.5 py-2 text-xs font-medium text-muted-foreground shadow-sm"
                  >
                    <Icon className="size-3.5 text-primary" />
                    {label}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="como-funciona" className="relative overflow-hidden border-y border-border/60 bg-muted/25 px-6 py-24 sm:py-28">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-primary/5 to-transparent" />
        <div className="relative mx-auto max-w-6xl">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Paso a paso</p>
            <h2 className="mt-4 text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
              Simple desde el primer momento
            </h2>
            <p className="mt-4 text-balance leading-7 text-muted-foreground">
              Te mostramos qué sigue y te acompañamos desde la solicitud hasta la entrega del beneficio.
            </p>
          </div>

          <div className="relative mt-14 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="absolute left-[12.5%] right-[12.5%] top-8 hidden h-px bg-gradient-to-r from-primary/20 via-primary/50 to-primary/20 lg:block" />
            {STEPS.map(({ icon: Icon, title, description }, index) => (
              <div key={title} className="group relative flex flex-col items-center rounded-3xl border border-foreground/8 bg-background/70 px-5 pb-6 pt-0 text-center shadow-sm backdrop-blur-sm">
                <span className="-mt-px flex h-16 w-16 items-center justify-center rounded-b-2xl bg-primary text-primary-foreground shadow-lg shadow-primary/15 transition-transform duration-300 group-hover:translate-y-1">
                  <Icon className="size-6" />
                </span>
                <span className="mt-5 text-[10px] font-bold uppercase tracking-[0.18em] text-primary">
                  Paso {index + 1}
                </span>
                <h3 className="mt-2 font-heading text-sm font-semibold">{title}</h3>
                <p className="mt-2 text-xs leading-5 text-muted-foreground">{description}</p>
                {index < STEPS.length - 1 && (
                  <span className="absolute -right-2 top-8 z-10 hidden size-4 items-center justify-center rounded-full bg-background ring-4 ring-background lg:flex">
                    <span className="size-1.5 rounded-full bg-primary" />
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="px-6 py-24 sm:py-28">
        <Card className="relative mx-auto max-w-5xl overflow-hidden rounded-[2rem] border-primary/20 bg-primary py-0 text-primary-foreground shadow-2xl shadow-primary/15">
          <div className="absolute -right-20 -top-28 size-80 rounded-full border-[50px] border-white/8" />
          <div className="absolute -bottom-36 -left-20 size-72 rounded-full bg-white/5" />
          <div className="absolute inset-0 bg-[linear-gradient(110deg,transparent_35%,rgba(255,255,255,0.08)_100%)]" />
          <CardContent className="relative flex flex-col items-center gap-5 px-6 py-12 text-center sm:px-12 sm:py-16">
            <span className="rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-medium text-primary-foreground/80">
              Empezá hoy
            </span>
            <h2 className="max-w-2xl text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
              Tu próxima gestión puede ser mucho más simple
            </h2>
            <p className="max-w-xl text-balance leading-7 text-primary-foreground/70">
              Creá tu cuenta, encontrá programas disponibles en tu comuna y seguí tu solicitud sin perderte ningún paso.
            </p>
            <div className="mt-2 flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
              <Button
                className="h-12 rounded-xl bg-white px-6 text-primary shadow-lg hover:bg-white/90"
                size="lg"
                render={<Link to="/register" />}
              >
                Crear cuenta gratis
                <IconArrowRight />
              </Button>
              <Button
                className="h-12 rounded-xl border-white/20 bg-white/10 px-6 text-white hover:bg-white/15 hover:text-white"
                size="lg"
                variant="outline"
                render={<Link to="/login" />}
              >
                Ya tengo una cuenta
              </Button>
            </div>
          </CardContent>
        </Card>
      </section>

      <footer className="border-t border-border/60 bg-muted/15 px-6 py-10">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-6 text-center text-sm text-muted-foreground sm:flex-row sm:text-left">
          <div>
            <p className="font-medium text-foreground">Municipalidad UADE</p>
            <p className="mt-1 text-xs">© {new Date().getFullYear()} Módulo Desarrollo Social</p>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-3">
            <BackendStatusPill />
            <Link to="/login" className="transition-colors hover:text-foreground">
              Iniciar sesión
            </Link>
            <Link to="/register" className="transition-colors hover:text-foreground">
              Registrarme
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
