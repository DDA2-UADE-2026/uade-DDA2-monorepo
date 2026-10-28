import { IconArrowRight, IconSpeakerphone } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import type { ReactNode } from "react"
import { useState } from "react"

import { ActivityCard, ActivityCardSkeleton } from "@/components/activities/ActivityCard"
import { availableSeats } from "@/components/activities/activity-ui"
import { ActivityEnrollDialog } from "@/components/activities/ActivityEnrollDialog"
import { SectionHeading } from "@/components/layout/SectionHeading"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { list10Options } from "@/generated/@tanstack/react-query.gen"
import type { CitizenActivityResponse } from "@/generated/types.gen"
import { cn } from "@/lib/utils"

const PREVIEW_COUNT = 3

export function CitizenActivitiesSection({ className, canEnroll }: { className?: string; canEnroll: boolean }) {
  const [enrolling, setEnrolling] = useState<CitizenActivityResponse | null>(null)
  // Mantiene el estado confirmado mientras se actualiza el listado.
  const [justEnrolled, setJustEnrolled] = useState<ReadonlySet<string>>(new Set())

  const query = useQuery(list10Options({ query: { page: 0, size: PREVIEW_COUNT } }))
  const activities = query.data?.content ?? []

  if (query.isError) return null

  return (
    <section aria-labelledby="citizen-activities-title" className={cn("space-y-4", className)}>
      <SectionHeading
        id="citizen-activities-title"
        title="Actividades comunitarias"
        description="Talleres y jornadas abiertas a todo el barrio."
        action={
          <Button
            size="sm"
            variant="ghost"
            className="shrink-0"
            render={<Link to="/portal/actividades" search={{ page: 1 }} />}
          >
            Ver todas
            <IconArrowRight />
          </Button>
        }
      />

      <ActivityGroup title="Disponibles">
        {query.isPending ? (
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3" aria-label="Cargando actividades">
            {Array.from({ length: PREVIEW_COUNT }).map((_, index) => <ActivityCardSkeleton key={index} />)}
          </div>
        ) : activities.length === 0 ? (
          <Empty className="min-h-48 rounded-xl border">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <IconSpeakerphone />
              </EmptyMedia>
              <EmptyTitle>No hay actividades abiertas</EmptyTitle>
              <EmptyDescription>
                Volvé a consultar más adelante: publicamos nuevas actividades seguido.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {activities.map((activity) => {
              const enrolled = !!activity.myEnrollment || (activity.id !== undefined && justEnrolled.has(activity.id))
              const full = availableSeats(activity) === 0

              return (
                <ActivityCard
                  key={activity.id ?? activity.name}
                  activity={activity}
                  enrolled={enrolled}
                  action={
                    enrolled ? <Button className="w-full" variant="outline" render={<Link to="/portal/actividades/$actividadId" params={{ actividadId: activity.id ?? "" }} />}>Ver detalle <IconArrowRight /></Button> : canEnroll && <Button
                      className="w-full"
                      disabled={full || !activity.id}
                      onClick={() => setEnrolling(activity)}
                    >
                      {full ? "Sin cupo" : "Inscribirme"}
                    </Button>
                  }
                />
              )
            })}
          </div>
        )}
      </ActivityGroup>

      {enrolling && (
        <ActivityEnrollDialog
          activity={enrolling}
          onOpenChange={(open) => { if (!open) setEnrolling(null) }}
          onEnrolled={(activityId) => setJustEnrolled((current) => new Set(current).add(activityId))}
        />
      )}
    </section>
  )
}

function ActivityGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="space-y-2.5">
      <h3 className="px-1 text-xs font-medium tracking-wide text-muted-foreground uppercase">
        {title}
      </h3>
      {children}
    </div>
  )
}
