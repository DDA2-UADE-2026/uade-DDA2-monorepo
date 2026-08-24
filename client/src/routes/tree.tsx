import { Suspense, lazy } from "react"
import { createFileRoute, Link } from "@tanstack/react-router"
import {
  IconArrowUpRight,
  IconBolt,
  IconBrandGithub,
  IconFileText,
  IconGitBranch,
  IconWorld,
  type Icon,
} from "@tabler/icons-react"

import { BackendStatusPill } from "@/components/BackendStatusPill"
import { ThemeToggle } from "@/components/ThemeToggle"
import AppLogoIconThemed from "@/components/branding/AppLogoIconThemed"
import AppLogoTitleThemed from "@/components/branding/AppLogoTitleThemed"
import { cn } from "@/lib/utils"

const SideRaysBackground = lazy(() => import("@/components/visual/SideRaysBackground"))

export const Route = createFileRoute("/tree")({
  component: RouteComponent,
})

interface TreeLink {
  title: string
  description: string
  icon: Icon
  link?: string
}

const DEV_LINKS: TreeLink[] = [
  {
    title: "Sitio de desarrollo",
    description: "Última versión desplegada desde la rama develop.",
    icon: IconWorld,
    link: "https://app-muni-uade-dev.fabriziob.com/",
  },
  {
    title: "Documentación de la API",
    description: "Swagger/OpenAPI del ambiente de desarrollo.",
    icon: IconFileText,
    link: "https://api-muni-uade-dev.fabriziob.com/swagger-ui/index.html",
  },
  {
    title: "Eventos asíncronos",
    description: "Documentación de eventos y colas del ambiente de desarrollo.",
    icon: IconBolt,
  }
]

const PROD_LINKS: TreeLink[] = [
  {
    title: "Sitio de producción",
    description: "Última versión desplegada desde la rama main.",
    icon: IconWorld,
    link: "https://app-muni-uade.fabriziob.com/",
  },
  {
    title: "Documentación de la API",
    description: "Swagger/OpenAPI del ambiente de producción.",
    icon: IconFileText,
    link: "https://api-muni-uade.fabriziob.com/swagger-ui/index.html",
  },
  {
    title: "Eventos asíncronos",
    description: "Documentación de eventos y colas del ambiente de producción.",
    icon: IconBolt,
  }
]

function TreeLinkCard({ item, iconClassName }: { item: TreeLink; iconClassName: string }) {
  const { title, description, icon: LinkIcon, link } = item

  const content = (
    <>
      <span className={cn("flex size-11 shrink-0 items-center justify-center rounded-xl border", iconClassName)}>
        <LinkIcon className="size-5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block font-medium text-foreground">{title}</span>
        <span className="mt-0.5 block text-sm leading-5 text-muted-foreground">{description}</span>
      </span>
      {link ? (
        <IconArrowUpRight className="mt-1 size-4 shrink-0 text-muted-foreground transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-focus-visible:-translate-y-0.5 group-focus-visible:translate-x-0.5" />
      ) : (
        <span className="mt-0.5 shrink-0 rounded-full bg-muted px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
          Próximamente
        </span>
      )}
    </>
  )

  if (!link) {
    return (
      <div
        aria-disabled="true"
        className="flex items-start gap-4 rounded-2xl border border-dashed border-border/80 bg-muted/20 p-4 opacity-75"
      >
        {content}
      </div>
    )
  }

  return (
    <a
      href={link}
      target="_blank"
      rel="noopener noreferrer"
      className="group flex items-start gap-4 rounded-2xl border border-border bg-card/75 p-4 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/35 hover:bg-card hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background motion-reduce:hover:translate-y-0"
    >
      {content}
    </a>
  )
}

function TreeColumn({
  label,
  description,
  branch,
  accentClassName,
  iconClassName,
  links,
}: {
  label: string
  description: string
  branch: string
  accentClassName: string
  iconClassName: string
  links: TreeLink[]
}) {
  return (
    <section className="rounded-3xl border border-border/80 bg-background/70 p-5 shadow-lg shadow-black/5 backdrop-blur-xl sm:p-6">
      <div className="mb-5 flex items-start justify-between gap-4">
        <div>
          <span
            className={cn(
              "inline-flex rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-wide",
              accentClassName
            )}
          >
            {label}
          </span>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">{description}</p>
        </div>
        <span className="flex shrink-0 items-center gap-1.5 rounded-full border border-border bg-muted/50 px-2.5 py-1 font-mono text-[11px] text-muted-foreground">
          <IconGitBranch className="size-3.5" />
          {branch}
        </span>
      </div>
      <div className="flex flex-col gap-3">
        {links.map((item) => (
          <TreeLinkCard key={item.title} item={item} iconClassName={iconClassName} />
        ))}
      </div>
    </section>
  )
}

function RouteComponent() {
  return (
    <div className="relative min-h-svh w-full bg-background">
      <div className="absolute inset-0 h-full w-full pointer-events-none opacity-45 dark:opacity-100">
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

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col px-6 py-8 sm:py-10">
        <header className="flex items-center justify-between gap-4">
          <Link to="/" aria-label="Ir al inicio" className="flex items-center gap-2.5 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <AppLogoIconThemed className="h-10 sm:h-11" />
            <AppLogoTitleThemed className="h-9 sm:h-10" />
          </Link>
          <ThemeToggle />
        </header>

        <div className="mx-auto mt-12 max-w-2xl text-center sm:mt-16">
          <h1 className="text-balance font-heading text-3xl font-semibold tracking-tight sm:text-5xl">
            Recursos y ambientes del proyecto
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-balance leading-7 text-muted-foreground">
            Elegí el ambiente que necesitás para acceder a la aplicación y su documentación técnica.
          </p>
        </div>

        <a
          href="https://github.com/DDA2-UADE-2026/uade-DDA2-monorepo"
          target="_blank"
          rel="noopener noreferrer"
          className="group mt-10 flex w-full items-center gap-4 rounded-2xl border border-border/80 bg-background/70 p-4 shadow-sm backdrop-blur-xl transition-all hover:-translate-y-0.5 hover:border-primary/35 hover:bg-card hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background motion-reduce:hover:translate-y-0 sm:p-5"
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-xl border border-border bg-foreground text-background">
            <IconBrandGithub className="size-5" />
          </span>
          <span className="min-w-0 flex-1">
            <span className="block font-medium text-foreground">Monorepo del proyecto</span>
            <span className="mt-0.5 block text-sm text-muted-foreground">
              Código fuente, historial de cambios y colaboración del equipo.
            </span>
          </span>
          <IconArrowUpRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-focus-visible:-translate-y-0.5 group-focus-visible:translate-x-0.5" />
        </a>

        <main className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <TreeColumn
            label="Desarrollo"
            description="Pruebas, integración y validación de los últimos cambios."
            branch="develop"
            accentClassName="border-amber-400/40 bg-amber-400/15 text-amber-800 dark:text-amber-300"
            iconClassName="border-amber-400/20 bg-amber-400/10 text-amber-700 dark:text-amber-300"
            links={DEV_LINKS}
          />
          <TreeColumn
            label="Producción"
            description="Servicios estables y datos del entorno operativo."
            branch="main"
            accentClassName="border-emerald-400/40 bg-emerald-400/15 text-emerald-800 dark:text-emerald-300"
            iconClassName="border-emerald-400/20 bg-emerald-400/10 text-emerald-700 dark:text-emerald-300"
            links={PROD_LINKS}
          />
        </main>

        <footer className="mt-8 flex flex-col items-center justify-between gap-3 border-t border-border/60 pt-6 text-center text-xs text-muted-foreground sm:flex-row sm:text-left">
          <span>El indicador corresponde a la API configurada en esta versión del sitio.</span>
          <BackendStatusPill />
        </footer>
      </div>
    </div>
  )
}
