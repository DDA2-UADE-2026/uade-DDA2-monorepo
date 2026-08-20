import { Suspense, lazy } from "react"
import { createFileRoute, Link } from "@tanstack/react-router"
import {
  IconArrowRight,
  IconClipboardCheck,
  IconHeartHandshake,
  IconHomeCheck,
  IconStethoscope,
  IconUserPlus,
  IconUsersGroup,
} from "@tabler/icons-react"

import { AppPhoneMock } from "@/components/landing/AppPhoneMock"
import { SiteHeader } from "@/components/landing/SiteHeader"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"

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
  return (
    <div className="min-h-svh bg-background">
      <SiteHeader />

      <section className="relative flex min-h-svh items-center justify-center overflow-hidden px-6">
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

        <div className="relative z-10 mx-auto grid w-full max-w-6xl items-center gap-12 pt-24 md:grid-cols-2 md:gap-8">
          <div className="flex flex-col items-center gap-6 text-center md:items-start md:text-left">
            <span className="rounded-full border border-border bg-background/60 px-3 py-1 text-xs font-medium text-muted-foreground backdrop-blur-sm">
              Desarrollo social · Salud comunitaria
            </span>
            <h1 className="text-balance font-heading text-4xl font-semibold tracking-tight sm:text-6xl">
              Todos tus programas sociales, en un solo lugar
            </h1>
            <p className="text-balance text-lg text-muted-foreground">
              Solicitá beneficios, seguí tu evaluación y coordiná turnos de salud comunitaria sin
              perder tiempo en ventanillas. Para vecinos, trabajadores sociales y equipos de coordinación.
            </p>
            <div className="mt-2 flex flex-col gap-3 sm:flex-row">
              <Button size="lg" render={<Link to="/register" />}>
                Solicitar un programa
                <IconArrowRight />
              </Button>
              <Button size="lg" variant="outline" render={<a href="#como-funciona" />}>
                Cómo funciona
              </Button>
            </div>
          </div>

          <AppPhoneMock />
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 py-24">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            Todo el acompañamiento social, conectado
          </h2>
          <p className="mt-3 text-balance text-muted-foreground">
            Programas, visitas domiciliarias y salud comunitaria en una sola plataforma.
          </p>
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, description }) => (
            <Card key={title}>
              <CardHeader>
                <span className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Icon className="size-5" />
                </span>
                <CardTitle className="mt-2 text-lg">{title}</CardTitle>
                <CardDescription>{description}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>

        <div className="mt-16 flex flex-wrap items-center justify-center gap-3">
          {AUDIENCES.map(({ icon: Icon, label }) => (
            <span
              key={label}
              className="flex items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm text-muted-foreground"
            >
              <Icon className="size-4 text-primary" />
              {label}
            </span>
          ))}
        </div>
      </section>

      <section id="como-funciona" className="border-t border-border bg-muted/30 px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
              Cómo funciona
            </h2>
            <p className="mt-3 text-balance text-muted-foreground">
              De la solicitud al beneficio, con seguimiento en cada etapa.
            </p>
          </div>

          <div className="mt-12 grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
            {STEPS.map(({ icon: Icon, title, description }, index) => (
              <div key={title} className="flex flex-col items-center gap-3 text-center">
                <span className="relative flex size-12 items-center justify-center rounded-full bg-primary text-primary-foreground">
                  <Icon className="size-5" />
                  <span className="absolute -right-1 -top-1 flex size-5 items-center justify-center rounded-full bg-background text-xs font-semibold ring-1 ring-border">
                    {index + 1}
                  </span>
                </span>
                <h3 className="font-heading font-medium">{title}</h3>
                <p className="text-sm text-muted-foreground">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="px-6 py-24">
        <Card className="mx-auto flex max-w-4xl flex-col items-center gap-4 p-10 text-center ring-primary/20">
          <CardContent className="flex flex-col items-center gap-4 px-0">
            <h2 className="text-balance font-heading text-3xl font-semibold tracking-tight">
              ¿Necesitás ayuda o querés postularte a un programa?
            </h2>
            <p className="text-balance text-muted-foreground">
              Creá tu cuenta, consultá los programas activos en tu comuna y seguí el estado de tu solicitud.
            </p>
            <Button size="lg" render={<Link to="/register" />}>
              Crear cuenta gratis
              <IconArrowRight />
            </Button>
          </CardContent>
        </Card>
      </section>

      <footer className="border-t border-border px-6 py-10">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 text-center text-sm text-muted-foreground sm:flex-row sm:text-left">
          <div>
            <p>© {new Date().getFullYear()} Acme Inc. Todos los derechos reservados.</p>
            <p className="text-xs">Módulo Desarrollo Social · Municipalidad UADE</p>
          </div>
          <div className="flex items-center gap-6">
            <Link to="/login" className="hover:text-foreground">
              Iniciar sesión
            </Link>
            <Link to="/register" className="hover:text-foreground">
              Registrarme
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
