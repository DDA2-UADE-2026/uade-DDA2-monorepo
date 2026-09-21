import {
  IconAlertCircle,
  IconArrowLeft,
  IconArrowUpRight,
  IconBan,
  IconCalendarEvent,
  IconChecklist,
  IconClock,
  IconFileDescription,
  IconGift,
  IconHeartHandshake,
  IconUsers,
} from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"

import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertAction, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Item,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from "@/components/ui/item"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { getAvailableProgramOptions } from "@/generated/@tanstack/react-query.gen"
import { applicationPeriodUnavailableReason, enrollmentStatusLabels } from "@/lib/application-flow"
import { programImageSource } from "@/lib/program-images"
import type {
  AvailableProgramBenefitResponse,
  AvailableProgramDocumentRequirementResponse,
  AvailableProgramEditionResponse,
  AvailableProgramRequirementResponse,
} from "@/generated/types.gen"

type BenefitType = NonNullable<AvailableProgramBenefitResponse["type"]>
type RequirementType = NonNullable<AvailableProgramRequirementResponse["type"]>
type EditionStatus = NonNullable<AvailableProgramEditionResponse["status"]>

const PROGRAM_IMAGE = `${import.meta.env.BASE_URL}brand/og.png`
const PROGRAM_DESCRIPTION_FALLBACK = "El objetivo de este programa se informará próximamente."

const benefitLabels: Record<BenefitType, string> = {
  TAX_EXEMPTION: "Exención impositiva",
  HOUSING_SUBSIDY: "Subsidio habitacional",
  FOOD_ASSISTANCE: "Asistencia alimentaria",
  UTILITY_SUBSIDY: "Subsidio de servicios",
}

const requirementLabels: Record<RequirementType, string> = {
  MIN_AGE: "Edad mínima",
  MAX_INCOME: "Ingreso máximo",
  RESIDENCY_YEARS: "Años de residencia",
  HAS_CHILDREN: "Tiene hijos",
}

const editionStatusLabels: Record<EditionStatus, string> = {
  DRAFT: "Borrador",
  ACTIVE: "Activa",
  SUSPENDED: "Suspendida",
  CLOSED: "Cerrada",
}

export const Route = createFileRoute("/_app/portal/programas/$programaId")({
  component: RouteComponent,
})

function formatProgramDate(value?: string): string {
  if (!value) return "A confirmar"

  const [year, month, day] = value.split("-").map(Number)
  return new Intl.DateTimeFormat("es-AR", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(year, month - 1, day))
}

function formatAmount(value?: number): string | undefined {
  if (value === undefined) return undefined

  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
  }).format(value)
}

function RouteComponent() {
  const { programaId } = Route.useParams()
  const query = useQuery(
    getAvailableProgramOptions({ path: { id: programaId } }),
  )
  const program = query.data

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs
          items={[
            { label: "Programas", to: "/portal/programas" },
            { label: program?.name ?? "Detalle" },
          ]}
        />
        <OutletNavRightButton>
          <Button
            variant="ghost"
            size="sm"
            render={<Link to="/portal/programas" search={{ page: 1 }} />}
          >
            <IconArrowLeft />
            <span className="hidden sm:inline">Volver</span>
          </Button>
        </OutletNavRightButton>
      </OutletNavSticky>

      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 lg:p-6">
            {query.isPending ? (
              <ProgramDetailSkeleton />
            ) : query.isError ? (
              <Alert variant="destructive">
                <IconAlertCircle />
                <AlertTitle>No pudimos cargar el programa</AlertTitle>
                <AlertDescription>
                  El programa puede no estar disponible o pudo ocurrir un problema al consultarlo.
                </AlertDescription>
                <AlertAction>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={query.isFetching}
                    onClick={() => query.refetch()}
                  >
                    Reintentar
                  </Button>
                </AlertAction>
              </Alert>
            ) : program ? (
              <>
                <Card className="gap-0 overflow-hidden py-0 lg:grid lg:grid-cols-[minmax(18rem,0.8fr)_minmax(0,1.2fr)]">
                  <img
                    src={programImageSource(program.imageUrl) ?? PROGRAM_IMAGE}
                    alt=""
                    className="aspect-[1.91/1] size-full max-h-96 object-cover lg:aspect-auto lg:min-h-80"
                  />
                  <div className="flex min-w-0 flex-col justify-center py-6">
                    <CardHeader>
                      <Badge variant="secondary">
                        <IconHeartHandshake />
                        Programa social
                      </Badge>
                      <CardTitle className="mt-2 text-2xl sm:text-3xl">
                        {program.name || "Programa sin nombre"}
                      </CardTitle>
                      <CardDescription className="max-w-2xl text-sm leading-6">
                        {program.objective || PROGRAM_DESCRIPTION_FALLBACK}
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <Badge variant="outline" className="hidden">
                        {(program.editions ?? []).length}{" "}
                        {(program.editions ?? []).length === 1
                          ? "edición disponible"
                          : "ediciones disponibles"}
                      </Badge>
                    </CardContent>
                  </div>
                </Card>

                <section className="space-y-4" aria-labelledby="available-editions-title">
                  <div>
                    <h2 id="available-editions-title" className="font-heading text-xl font-medium">
                      Ediciones disponibles
                    </h2>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Revisá las fechas, los cupos y las condiciones de cada edición.
                    </p>
                  </div>

                  {(program.editions ?? []).length === 0 ? (
                    <Card>
                      <CardContent className="text-sm text-muted-foreground">
                        No hay ediciones disponibles en este momento.
                      </CardContent>
                    </Card>
                  ) : (
                    <div className="grid gap-5">
                      {(program.editions ?? []).map((edition) => (
                        <EditionCard key={edition.id ?? edition.name} edition={edition} programaId={programaId} />
                      ))}
                    </div>
                  )}
                </section>

                {(program.incompatibilities ?? []).length > 0 && (
                  <section aria-labelledby="incompatibilities-title">
                    <Card>
                      <CardHeader>
                        <CardTitle id="incompatibilities-title" className="flex items-center gap-2">
                          <IconBan className="size-5 text-destructive" />
                          Programas incompatibles
                        </CardTitle>
                        <CardDescription>
                          Este programa no puede combinarse con los siguientes programas.
                        </CardDescription>
                      </CardHeader>
                      <CardContent className="flex flex-wrap gap-2">
                        {(program.incompatibilities ?? []).map((incompatibility) => (
                          <Button
                            key={incompatibility.id ?? incompatibility.name}
                            variant="destructive"
                            size="sm"
                            render={
                              <Link
                                to="/portal/programas/$programaId"
                                params={{ programaId: incompatibility.id ?? "" }}
                              />
                            }
                          >
                            {incompatibility.name || "Programa sin nombre"}
                            <IconArrowUpRight />
                          </Button>
                        ))}
                      </CardContent>
                    </Card>
                  </section>
                )}
              </>
            ) : null}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function EditionCard({ edition, programaId }: { edition: AvailableProgramEditionResponse; programaId: string }) {
  const currentEnrollment = edition.currentEnrollment ?? 0
  const maxCapacity = edition.maxCapacity ?? 0
  const availableCapacity = edition.availableCapacity ?? Math.max(0, maxCapacity - currentEnrollment)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">{edition.name || "Edición sin nombre"}</CardTitle>
        <CardDescription>
          Del {formatProgramDate(edition.startDate)} al {formatProgramDate(edition.endDate)}
        </CardDescription>
        <CardAction>
          <Badge variant={edition.status === "ACTIVE" ? "default" : "secondary"}>
            {edition.status ? editionStatusLabels[edition.status] : "Sin estado"}
          </Badge>
        </CardAction>
      </CardHeader>

      <CardContent className="space-y-6">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <SummaryItem icon={<IconCalendarEvent />} label="Inicio" value={formatProgramDate(edition.startDate)} />
          <SummaryItem icon={<IconCalendarEvent />} label="Finalización" value={formatProgramDate(edition.endDate)} />
          <SummaryItem icon={<IconUsers />} label="Personas inscriptas" value={String(currentEnrollment)} />
          <SummaryItem icon={<IconUsers />} label="Vacantes disponibles" value={String(availableCapacity)} />
        </div>

        <Separator />

        <div className="grid gap-6 lg:grid-cols-2">
          <EditionBenefits benefits={edition.benefits ?? []} />
          <EditionRequirements requirements={edition.requirements ?? []} />
        </div>

        <Separator />

        <EditionDocumentRequirements requirements={edition.documentRequirements ?? []} />

        <Separator />

        <div className="space-y-3">
          <div>
            <h3 className="font-heading font-medium">Períodos de inscripción</h3>
            <p className="text-sm text-muted-foreground">
              Consultá las fechas y el estado de cada período para presentar una solicitud.
            </p>
          </div>
          {(edition.enrollmentPeriods ?? []).length === 0 ? (
            <p className="rounded-lg bg-muted/50 p-4 text-sm text-muted-foreground">
              No hay un período de inscripción abierto para esta edición.
            </p>
          ) : (
            <ItemGroup className="grid gap-3 md:grid-cols-2">
              {(edition.enrollmentPeriods ?? []).map((period) => {
                const unavailableReason = applicationPeriodUnavailableReason(edition, period)
                return (
                <Item key={period.id} variant="outline">
                  <ItemMedia variant="icon">
                    <IconClock className="text-primary" />
                  </ItemMedia>
                  <ItemContent>
                    <ItemTitle>Período de inscripción</ItemTitle>
                    <ItemDescription>
                      Del {formatProgramDate(period.openDate)} al {formatProgramDate(period.closeDate)}
                    </ItemDescription>
                  </ItemContent>
                  <Badge variant="secondary">{period.status ? enrollmentStatusLabels[period.status] : "Sin estado"}</Badge>
                  {!unavailableReason && period.id ? (
                    <Button className="w-full animate-pulse" render={<Link to="/portal/solicitudes/nueva" search={{ programaId, periodoId: period.id }} />}>
                      Solicitar este programa
                    </Button>
                  ) : (
                    <div className="w-full space-y-2">
                      <Button className="w-full" disabled>Solicitar este programa</Button>
                      <p className="text-sm text-muted-foreground">{unavailableReason}</p>
                    </div>
                  )}
                </Item>
                )
              })}
            </ItemGroup>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

function SummaryItem({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <Item variant="muted">
      <ItemMedia variant="icon" className="text-primary">
        {icon}
      </ItemMedia>
      <ItemContent>
        <ItemDescription>{label}</ItemDescription>
        <ItemTitle>{value}</ItemTitle>
      </ItemContent>
    </Item>
  )
}

function EditionBenefits({ benefits }: { benefits: AvailableProgramBenefitResponse[] }) {
  return (
    <div className="space-y-3">
      <div>
        <h3 className="font-heading font-medium">Beneficios</h3>
        <p className="text-sm text-muted-foreground">Qué ofrece esta edición.</p>
      </div>
      {benefits.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay beneficios informados.</p>
      ) : (
        <ItemGroup>
          {benefits.map((benefit) => {
            const details = [benefit.description, formatAmount(benefit.amount)].filter(Boolean).join(" · ")

            return (
              <Item key={benefit.id} variant="outline">
                <ItemMedia variant="icon">
                  <IconGift className="text-primary" />
                </ItemMedia>
                <ItemContent>
                  <ItemTitle>{benefit.type ? benefitLabels[benefit.type] : "Beneficio"}</ItemTitle>
                  <ItemDescription>{details || "Beneficio incluido en la edición."}</ItemDescription>
                </ItemContent>
              </Item>
            )
          })}
        </ItemGroup>
      )}
    </div>
  )
}

function EditionRequirements({ requirements }: { requirements: AvailableProgramRequirementResponse[] }) {
  return (
    <div className="space-y-3">
      <div>
        <h3 className="font-heading font-medium">Requisitos</h3>
        <p className="text-sm text-muted-foreground">Condiciones necesarias para postularte.</p>
      </div>
      {requirements.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay requisitos informados.</p>
      ) : (
        <ItemGroup>
          {requirements.map((requirement) => {
            const details = [
              requirement.value ? `Valor requerido: ${requirement.value}` : undefined,
              requirement.description,
            ].filter(Boolean).join(" · ")

            return (
              <Item key={requirement.id} variant="outline">
                <ItemMedia variant="icon">
                  <IconChecklist className="text-primary" />
                </ItemMedia>
                <ItemContent>
                  <ItemTitle>
                    {requirement.type ? requirementLabels[requirement.type] : "Requisito"}
                  </ItemTitle>
                  <ItemDescription>{details || "Requisito obligatorio."}</ItemDescription>
                </ItemContent>
              </Item>
            )
          })}
        </ItemGroup>
      )}
    </div>
  )
}

function EditionDocumentRequirements({ requirements }: { requirements: AvailableProgramDocumentRequirementResponse[] }) {
  return (
    <div className="space-y-3">
      <div>
        <h3 className="font-heading font-medium">Documentación requerida</h3>
        <p className="text-sm text-muted-foreground">Documentos solicitados para esta edición.</p>
      </div>
      {requirements.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay documentos requeridos para esta edición.</p>
      ) : (
        <ItemGroup className="grid gap-3 md:grid-cols-2">
          {requirements.map((requirement) => (
            <Item key={requirement.id ?? requirement.code} variant="outline" role="listitem">
              <ItemMedia variant="icon">
                <IconFileDescription className="text-primary" />
              </ItemMedia>
              <ItemContent>
                <ItemTitle>{requirement.name || "Documento sin nombre"}</ItemTitle>
                {requirement.description && (
                  <ItemDescription className="line-clamp-none whitespace-pre-line">
                    {requirement.description}
                  </ItemDescription>
                )}
              </ItemContent>
              <Badge variant={requirement.required ? "default" : "secondary"}>
                {requirement.required ? "Obligatorio" : "Opcional"}
              </Badge>
            </Item>
          ))}
        </ItemGroup>
      )}
    </div>
  )
}

function ProgramDetailSkeleton() {
  return (
    <div className="space-y-8" aria-label="Cargando programa">
      <Card className="gap-0 overflow-hidden py-0 lg:grid lg:grid-cols-2">
        <Skeleton className="aspect-[1.91/1] w-full rounded-none lg:min-h-80" />
        <div className="space-y-5 p-6">
          <Skeleton className="h-5 w-32" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-20 w-full" />
        </div>
      </Card>
      <div className="space-y-4">
        <Skeleton className="h-7 w-64" />
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-1/3" />
            <Skeleton className="h-4 w-1/2" />
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-18 w-full" />
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
