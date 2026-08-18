import { createFileRoute } from "@tanstack/react-router"

import { PageHeader } from "@/components/layout/PageHeader"

export const Route = createFileRoute("/_app/gestion/")({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <>
      <PageHeader>
        <h1 className="text-sm font-medium">Tablero operativo</h1>
      </PageHeader>
      <div className="flex flex-1 flex-col gap-4 p-4">
        <p className="text-sm text-muted-foreground">Hello "/_app/gestion/"!</p>
      </div>
    </>
  )
}
