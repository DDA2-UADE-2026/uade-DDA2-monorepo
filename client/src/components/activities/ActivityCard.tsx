import { IconCalendarEvent, IconMapPin, IconUsers } from "@tabler/icons-react"
import type { ReactNode } from "react"

import { availableSeats, formatActivityDateRange, seatsLabel } from "@/components/activities/activity-ui"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import type { CitizenActivityResponse } from "@/generated/types.gen"
import { cn } from "@/lib/utils"

export function ActivityCard({ activity, action, className }: {
  activity: CitizenActivityResponse
  action?: ReactNode
  className?: string
}) {
  const seats = availableSeats(activity)

  return (
    <Card className={cn("h-full", className)}>
      <CardHeader>
        <CardTitle className="text-lg">{activity.name || "Actividad sin nombre"}</CardTitle>
        <CardDescription className="line-clamp-2">
          {activity.description || "La descripción de esta actividad se informará próximamente."}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-2.5 text-sm">
        <p className="flex items-start gap-2 text-muted-foreground">
          <IconMapPin className="mt-0.5 size-4 shrink-0 text-primary" />
          <span className="min-w-0">{activity.location || "Lugar a confirmar"}</span>
        </p>
        <p className="flex items-start gap-2 text-muted-foreground">
          <IconCalendarEvent className="mt-0.5 size-4 shrink-0 text-primary" />
          <span className="min-w-0">{formatActivityDateRange(activity.startDate, activity.endDate)}</span>
        </p>
        <Badge variant={seats === 0 ? "secondary" : "outline"}>
          <IconUsers />
          {seatsLabel(activity)}
        </Badge>
      </CardContent>

      {action && <CardFooter className="mt-auto">{action}</CardFooter>}
    </Card>
  )
}

export function ActivityCardSkeleton() {
  return (
    <Card className="h-full">
      <CardHeader>
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-4/5" />
      </CardHeader>
      <CardContent className="space-y-2.5">
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-6 w-40 rounded-full" />
      </CardContent>
      <CardFooter>
        <Skeleton className="h-9 w-full" />
      </CardFooter>
    </Card>
  )
}
