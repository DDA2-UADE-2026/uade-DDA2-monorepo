import { IconFileCheck, IconFilePlus } from "@tabler/icons-react"
import { Link, createFileRoute } from "@tanstack/react-router"

import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { UserAvatar } from "@/components/UserAvatar"
import { useMe } from "@/hooks/use-auth"

export const Route = createFileRoute("/_app/gestion/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { data } = useMe()
  const displayName = data?.user?.name || data?.user?.username || "Usuario"

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Inicio" }]} />
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="flex w-full flex-col gap-6 p-4 lg:p-6">
            <header className="flex w-full items-center gap-4 px-1 py-2 sm:py-3">
              <UserAvatar
                user={data?.user ?? { name: displayName }}
                className="size-12 sm:size-14"
                fallbackClassName="text-base"
              />
              <div className="min-w-0 space-y-1.5">
                <h1 className="truncate font-heading text-xl font-medium sm:text-2xl">
                  Hola, {displayName}
                </h1>
                <p className="text-sm text-muted-foreground">
                  Desde acá podés administrar y hacer seguimiento de la gestión social del municipio.
                </p>
              </div>
            </header>

            <section aria-labelledby="application-management-title">
              <Card className="w-full">
                <CardHeader className="border-b">
                  <CardTitle id="application-management-title">
                    Gestión de solicitudes
                  </CardTitle>
                  <CardDescription>
                    Registrá presentaciones asistidas y revisá la documentación entregada.
                  </CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-3 sm:flex-row">
                  <Button render={<Link to="/gestion/solicitudes" />}>
                    <IconFilePlus />
                    Registrar solicitud asistida
                  </Button>
                  <Button variant="outline" render={<Link to="/gestion/documentos" search={{ solicitudId: "" }} />}>
                    <IconFileCheck />
                    Revisar documentos
                  </Button>
                </CardContent>
              </Card>
            </section>
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
