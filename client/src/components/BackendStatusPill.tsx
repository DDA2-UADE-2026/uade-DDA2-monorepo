import { useBackendHealth } from "@/hooks/use-backend-health"

function BackendStatusPill({ xs = false }: { xs?: boolean }) {
  const { isPending, isError } = useBackendHealth()

  const label = isPending ? "Verificando estado…" : isError ? "Servicio no disponible" : "Servicio operativo"

  return (
    <span
      title={label}
      className={`text-nowrap whitespace-nowrap flex-nowrap flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs text-muted-foreground ${xs ? "px-1! py-0! gap-1!" : ""}`}
    >
      <span
        className={`size-2 shrink-0 rounded-full ${
          isPending ? "animate-pulse bg-muted-foreground" : isError ? "bg-destructive" : "bg-emerald-500"
          } ${xs ? "size-1.5!" : ""}`}
      />
      <span
        className={`font-medium text-nowrap whitespace-nowrap ${xs ? " text-[10px]" : ""}`}
      >
        {label}
      </span>

    </span>
  )
}

export { BackendStatusPill }
