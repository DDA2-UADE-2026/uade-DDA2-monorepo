import { useState } from "react"
import { IconCheck, IconCopy, IconExternalLink, IconId } from "@tabler/icons-react"

import { Button } from "@/components/ui/button"
import { useMe } from "@/hooks/use-auth"

/** Login page of the Ciudadanos (M1) portal. */
const CIUDADANOS_PORTAL_URL = "https://modulo1-ciudadanos-grupo8.onrender.com/ingresar"

/** Sidebar card shown only for sessions started through the Ciudadanos (M1) login. */
function CitizenSessionCard() {
  const { data } = useMe()
  const [copied, setCopied] = useState(false)
  const citizenId = data?.user?.externalCitizenId

  if (data?.user?.authSource !== "M1" || !citizenId) return null

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(citizenId)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch { /* Clipboard unavailable: the ID stays visible to select by hand. */ }
  }

  return (
    <section
      aria-label="Sesión de Ciudadanos"
      className="flex flex-col gap-2 rounded-xl border bg-card border-primary/60 p-3 text-sm group-data-[collapsible=icon]:hidden"
    >
      <div className="flex items-center gap-2">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-primary text-primary-foreground">
          <IconId className="size-5" aria-hidden="true" />
        </span>
        <div className="flex min-w-0 flex-1 flex-col">
          <span className="text-xs text-muted-foreground">Citizen ID</span>
          <code className="truncate font-mono text-xs" title={citizenId}>{citizenId}</code>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="icon-xs"
          aria-label={copied ? "Citizen ID copiado" : "Copiar citizen ID"}
          onClick={() => void copy()}
        >
          {copied ? <IconCheck /> : <IconCopy />}
        </Button>
      </div>
      <Button
        variant="outline"
        size="xs"
        className="w-full"
        render={<a href={CIUDADANOS_PORTAL_URL} target="_blank" rel="noopener noreferrer" />}
      >
        Ir a Ciudadanos
        <IconExternalLink aria-hidden="true" />
      </Button>
    </section>
  )
}

export { CitizenSessionCard }
