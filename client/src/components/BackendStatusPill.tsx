import { useBackendHealth } from "@/hooks/use-backend-health"

function BackendStatusPill() {
  const { isPending, isError } = useBackendHealth()

  const label = isPending ? "Verificando estado…" : isError ? "Servicio no disponible" : "Servicio operativo"

  return (
    <span
      title={label}
      className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs text-muted-foreground"
    >
      <span
        className={`size-2 shrink-0 rounded-full ${
          isPending ? "animate-pulse bg-muted-foreground" : isError ? "bg-destructive" : "bg-emerald-500"
        }`}
      />
      {label}
    </span>
  )
}

export { BackendStatusPill }
