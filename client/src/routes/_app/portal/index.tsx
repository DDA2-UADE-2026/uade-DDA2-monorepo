import { IconClipboardText } from "@tabler/icons-react"
import { createFileRoute } from "@tanstack/react-router"

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
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { UserAvatar } from "@/components/UserAvatar"
import { useMe } from "@/hooks/use-auth"

export const Route = createFileRoute("/_app/portal/")({
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
                addBlob
                blobProps="blur-xl!"
                user={data?.user ?? { name: displayName }}
                className="size-12 sm:size-14"
                fallbackClassName="text-base"
              />
              <div className="min-w-0 space-y-1.5">
                <h1 className="truncate font-heading text-xl font-medium sm:text-2xl">
                  Hola, {displayName}
                </h1>
                <p className="text-sm text-muted-foreground">
                  Desde acá podés consultar los programas disponibles y seguir el estado de tus solicitudes.
                </p>
              </div>
            </header>

            <section aria-labelledby="recent-applications-title">
              <Card className="w-full">
                <CardHeader className="border-b">
                  <CardTitle id="recent-applications-title">
                    Mis solicitudes recientes
                  </CardTitle>
                  <CardDescription>
                    Tus solicitudes más recientes aparecerán en esta sección.
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Empty className="min-h-64 border">
                    <EmptyHeader>
                      <EmptyMedia variant="icon">
                        <IconClipboardText />
                      </EmptyMedia>
                      <EmptyTitle>No tenés solicitudes recientes</EmptyTitle>
                      <EmptyDescription>
                        Cuando solicites un programa, vas a poder seguirlo desde acá.
                      </EmptyDescription>
                    </EmptyHeader>
                  </Empty>
                </CardContent>
              </Card>
            </section>
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
