import { Suspense, lazy } from "react"
import { createFileRoute } from "@tanstack/react-router"
import {
  IconArrowUpRight,
  IconBolt,
  IconFileText,
  IconWorld,
  type Icon,
} from "@tabler/icons-react"

import { BackendStatusPill } from "@/components/BackendStatusPill"
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
  link: string
}

// TODO: reemplazar los "#" por las URLs reales de cada ambiente.
const DEV_LINKS: TreeLink[] = [
  {
    title: "Sitio de desarrollo",
    description: "Última versión desplegada desde la rama develop.",
    icon: IconWorld,
    link: "#",
  },
  {
    title: "Documentación de la API",
    description: "Swagger/OpenAPI del ambiente de desarrollo.",
    icon: IconFileText,
    link: "#",
  },
  {
    title: "Eventos asíncronos",
    description: "Documentación de eventos y colas del ambiente de desarrollo.",
    icon: IconBolt,
    link: "#",
  }
]

const PROD_LINKS: TreeLink[] = [
  {
    title: "Sitio de producción",
    description: "Última versión desplegada desde la rama main.",
    icon: IconWorld,
    link: "#",
  },
  {
    title: "Documentación de la API",
    description: "Swagger/OpenAPI del ambiente de producción.",
    icon: IconFileText,
    link: "#",
  },
  {
    title: "Eventos asíncronos",
    description: "Documentación de eventos y colas del ambiente de producción.",
    icon: IconBolt,
    link: "#",
  }
]

function TreeColumn({
  label,
  accentClassName,
  links,
}: {
  label: string
  accentClassName: string
  links: TreeLink[]
}) {
  return (
    <div className="flex flex-col gap-4">
      <span
        className={cn(
          "w-fit rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-wide",
          accentClassName
        )}
      >
        {label}
      </span>
      <div className="flex flex-col gap-3">
        {links.map(({ title, description, icon: LinkIcon, link }) => (
          <a
            key={title}
            href={link}
            target="_blank"
            rel="noopener noreferrer"
            className="group flex items-center gap-4 rounded-2xl border border-border bg-card/60 p-4 backdrop-blur-md transition-colors hover:border-primary/40 hover:bg-card/80"
          >
            <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <LinkIcon className="size-5" />
            </span>
            <span className="flex flex-1 flex-col gap-0.5">
              <span className="font-medium">{title}</span>
              <span className="text-sm text-muted-foreground">{description}</span>
            </span>
            <IconArrowUpRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
          </a>
        ))}
      </div>
    </div>
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

      <div className="relative z-10 mx-auto flex w-full max-w-5xl flex-col gap-10 px-6 py-16 sm:py-20">
        <div className="flex flex-col items-start gap-5 lg:flex-row-reverse lg:items-center lg:justify-between">
          <div className="flex items-center gap-3">
            <AppLogoIconThemed className="h-14" />
            <AppLogoTitleThemed className="h-14" />
          </div>
          <div className="flex flex-col gap-1 text-left">
            <h1 className="text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
              Enlaces del proyecto
            </h1>
            <p className="max-w-md text-balance leading-snug text-muted-foreground">
              Accesos rápidos a los sitios, la documentación y el estado de cada ambiente.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
          <TreeColumn
            label="Desarrollo"
            accentClassName="border-amber-400 bg-amber-400 text-neutral-900"
            links={DEV_LINKS}
          />
          <TreeColumn
            label="Producción"
            accentClassName="border-emerald-400 bg-emerald-400 text-neutral-900"
            links={PROD_LINKS}
          />
        </div>

        <div className="flex justify-center">
          <BackendStatusPill />
        </div>
      </div>
    </div>
  )
}
