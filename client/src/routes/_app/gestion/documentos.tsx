import {
  IconAlertTriangle,
  IconFileCheck,
  IconFileDescription,
  IconRefresh,
  IconSearch,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { AdminDocumentsTable } from "@/components/applications/AdminApplicationDocuments"
import { DataPagination } from "@/components/DataPagination"
import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Input } from "@/components/ui/input"
import { list5Options } from "@/generated/@tanstack/react-query.gen"
import type { ErrorResponse } from "@/generated/types.gen"

const PAGE_SIZE = 10
const uuidSchema = z.uuid()
const searchSchema = z.object({
  solicitudId: z.string().catch("").default(""),
})

export const Route = createFileRoute("/_app/gestion/documentos")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const searchedApplicationId = solicitudId.trim()
  const hasValidApplicationId = uuidSchema.safeParse(searchedApplicationId).success
  const [applicationIdDraft, setApplicationIdDraft] = useState<string | null>(null)
  const [searchError, setSearchError] = useState<string | null>(null)
  const [page, setPage] = useState(1)

  const documents = useQuery({
    ...list5Options({ path: { applicationId: searchedApplicationId } }),
    enabled: hasValidApplicationId,
  })

  const applicationIdInput = applicationIdDraft ?? searchedApplicationId

  const totalItems = documents.data?.length ?? 0
  const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageItems = documents.data?.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  ) ?? []

  const submitSearch = () => {
    const nextApplicationId = applicationIdInput.trim()
    if (!uuidSchema.safeParse(nextApplicationId).success) {
      setSearchError("Ingresá un UUID de solicitud válido.")
      return
    }

    setSearchError(null)
    setPage(1)
    setApplicationIdDraft(null)
    if (nextApplicationId === searchedApplicationId) {
      documents.refetch()
      return
    }

    navigate({ search: { solicitudId: nextApplicationId } })
  }

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[
          { label: "Solicitudes", to: "/gestion/solicitudes" },
          { label: "Revisión documental" },
        ]} />
        {hasValidApplicationId && (
          <OutletNavRightButton>
            <Button
              size="sm"
              variant="outline"
              render={(
                <Link
                  to="/gestion/solicitudes/$solicitudId"
                  params={{ solicitudId: searchedApplicationId }}
                />
              )}
            >
              <IconFileDescription />
              Ver solicitud
            </Button>
          </OutletNavRightButton>
        )}
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 py-4 sm:mx-4 lg:py-6">
            <div className="mx-auto w-full max-w-7xl space-y-5">
              <div>
                <h1 className="font-heading text-xl font-semibold">Revisión de documentos</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  Consultá las entregas de una solicitud y resolvé los documentos pendientes.
                </p>
              </div>

              <form
                className="rounded-xl border bg-card p-4"
                noValidate
                onSubmit={(event) => {
                  event.preventDefault()
                  submitSearch()
                }}
              >
                <label className="text-sm font-medium" htmlFor="application-id">
                  ID de solicitud
                </label>
                <div className="mt-2 flex flex-col gap-2 sm:flex-row">
                  <Input
                    id="application-id"
                    className="font-mono"
                    value={applicationIdInput}
                    placeholder="00000000-0000-0000-0000-000000000000"
                    autoComplete="off"
                    spellCheck={false}
                    aria-invalid={Boolean(searchError)}
                    onChange={(event) => {
                      setApplicationIdDraft(event.target.value)
                      if (searchError) setSearchError(null)
                    }}
                  />
                  <Button type="submit" className="sm:min-w-28">
                    <IconSearch />
                    Buscar
                  </Button>
                </div>
                <p className={searchError ? "mt-2 text-sm text-destructive" : "mt-2 text-xs text-muted-foreground"}>
                  {searchError ?? "Usá el UUID interno de la solicitud o abrila desde la bandeja de solicitudes."}
                </p>
              </form>

              {!searchedApplicationId ? (
                <Empty className="min-h-72 border">
                  <EmptyHeader>
                    <EmptyMedia variant="icon"><IconFileCheck /></EmptyMedia>
                    <EmptyTitle>Buscá una solicitud</EmptyTitle>
                    <EmptyDescription>
                      Ingresá su UUID para consultar los documentos entregados y su estado de revisión.
                    </EmptyDescription>
                  </EmptyHeader>
                  <EmptyContent>
                    <Button
                      size="sm"
                      variant="outline"
                      render={<Link to="/gestion/solicitudes" search={{ page: 1, estado: undefined, q: "" }} />}
                    >
                      Ver todas las solicitudes
                    </Button>
                  </EmptyContent>
                </Empty>
              ) : !hasValidApplicationId ? (
                <Alert variant="destructive">
                  <IconAlertTriangle />
                  <AlertTitle>ID de solicitud inválido</AlertTitle>
                  <AlertDescription>Corregí el UUID para poder consultar sus documentos.</AlertDescription>
                </Alert>
              ) : documents.isPending ? (
                <div className="flex min-h-48 items-center justify-center text-sm text-muted-foreground">
                  Cargando documentos…
                </div>
              ) : documents.isError ? (
                <DocumentsQueryError error={documents.error} retry={() => documents.refetch()} />
              ) : totalItems === 0 ? (
                <Empty className="min-h-72 border">
                  <EmptyHeader>
                    <EmptyMedia variant="icon"><IconFileDescription /></EmptyMedia>
                    <EmptyTitle>Sin documentos entregados</EmptyTitle>
                    <EmptyDescription>
                      La solicitud seleccionada todavía no tiene archivos para revisar.
                    </EmptyDescription>
                  </EmptyHeader>
                </Empty>
              ) : (
                <>
                  <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
                    <div className="min-w-0">
                      <p className="text-sm font-medium">Solicitud</p>
                      <p className="truncate font-mono text-xs text-muted-foreground">
                        {searchedApplicationId}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      {documents.dataUpdatedAt > 0 && (
                        <span className="hidden sm:inline">
                          Actualizado {new Date(documents.dataUpdatedAt).toLocaleTimeString("es-AR")}
                        </span>
                      )}
                      <Button
                        type="button"
                        size="xs"
                        variant="ghost"
                        disabled={documents.isFetching}
                        onClick={() => documents.refetch()}
                      >
                        <IconRefresh className={documents.isFetching ? "animate-spin" : undefined} />
                        Actualizar
                      </Button>
                    </div>
                  </div>

                  <AdminDocumentsTable applicationId={searchedApplicationId} documents={pageItems} />
                </>
              )}
            </div>
          </div>
        </div>

        {!documents.isPending && !documents.isError && totalItems > 0 && (
          <DataPagination
            page={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
          />
        )}
      </SidebarShellContent>
    </SidebarShell>
  )
}

function DocumentsQueryError({ error, retry }: { error: ErrorResponse; retry: () => void }) {
  const notFound = error.status === 404
  return (
    <Alert variant="destructive">
      <IconAlertTriangle />
      <AlertTitle>{notFound ? "No se encontró la solicitud" : "No se pudieron cargar los documentos"}</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-3">
        <span>{error.message ?? (notFound ? "Verificá el UUID ingresado." : "Intentá nuevamente.")}</span>
        <Button type="button" size="sm" variant="outline" onClick={retry}>Reintentar</Button>
      </AlertDescription>
    </Alert>
  )
}
