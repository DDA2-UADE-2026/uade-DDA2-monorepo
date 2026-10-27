import { IconArrowRight } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { useState } from "react"

import { CitizenVisitCard, CitizenVisitDialog, CitizenVisitError } from "@/components/applications/CitizenVisitUi"
import { SectionHeading } from "@/components/layout/SectionHeading"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useMe } from "@/hooks/use-auth"
import { citizenVisitError, citizenVisitQueries, type CitizenVisit } from "@/lib/citizen-visits"
import { cn } from "@/lib/utils"

const UPCOMING_COUNT = 3

/** Próximas visitas y entrevistas en la home del portal. Sin permiso o sin visitas programadas no se muestra. */
export function UpcomingVisits({ className }: { className?: string }) {
  const { data: session } = useMe()
  const allowed = !!session?.user?.permissions?.includes("applications:own:visits:view")
  const [selected, setSelected] = useState<CitizenVisit | null>(null)
  const query = useQuery({ ...citizenVisitQueries.listAll(true, 0, UPCOMING_COUNT), enabled: allowed })

  if (!allowed || (query.isSuccess && query.data.content.length === 0)) return null

  return (
    <section aria-labelledby="upcoming-visits-title" className={cn("space-y-3", className)}>
      <SectionHeading
        id="upcoming-visits-title"
        title="Próximas visitas y entrevistas"
        description="Actuaciones programadas para tus solicitudes."
        action={
          <Button size="sm" variant="ghost" className="shrink-0" render={<Link to="/portal/visitas" search={{ page: 1, vista: "proximas" }} />}>
            Ver todas
            <IconArrowRight />
          </Button>
        }
      />
      {query.isPending ? (
        <div className="space-y-3" role="status" aria-label="Cargando visitas">
          <Skeleton className="h-32 w-full rounded-xl" />
          <span className="sr-only">Cargando visitas…</span>
        </div>
      ) : query.isError ? (
        <CitizenVisitError message={citizenVisitError(query.error)} retry={() => query.refetch()} />
      ) : (
        <div className="space-y-3">
          {query.data.content.map((visit) => <CitizenVisitCard key={visit.id} visit={visit} showApplication onOpen={() => setSelected(visit)} />)}
        </div>
      )}
      {selected && <CitizenVisitDialog key={selected.id} applicationId={selected.applicationId} visitId={selected.id} onClose={() => setSelected(null)} />}
    </section>
  )
}
