import type { RouterOptions } from "@tanstack/react-router"
import type { CSSProperties } from "react"

// router-core no se re-exporta desde react-router; se deriva de las opciones.
type ViewTransitionOptions = Exclude<
  NonNullable<RouterOptions<never, never>["defaultViewTransition"]>,
  boolean
>

/**
 * Tipos de View Transition del portal ciudadano. El CSS de `index.css` elige
 * la animación con `:active-view-transition-type(...)`:
 *
 * - `portal-fade`: crossfade entre secciones (sidebar).
 * - `portal-forward`: lista → detalle, desliza hacia la izquierda.
 * - `portal-back`: detalle → lista, desliza hacia la derecha.
 * - `portal-search`: paginación y filtros, crossfade corto.
 */
export type PortalTransitionType =
  | "portal-fade"
  | "portal-forward"
  | "portal-back"
  | "portal-search"

const PORTAL_PREFIX = "/portal"

/** Segmentos debajo de `/portal`, o `null` si la ruta está fuera del portal. */
export function portalSegments(pathname: string): string[] | null {
  if (pathname !== PORTAL_PREFIX && !pathname.startsWith(`${PORTAL_PREFIX}/`)) return null
  return pathname.slice(PORTAL_PREFIX.length).split("/").filter(Boolean)
}

/**
 * Dirección de una navegación entre dos rutas del portal, según la jerarquía:
 * profundidad 0 es Inicio, 1 son las secciones de la sidebar y 2+ son
 * detalles o formularios dentro de una sección.
 */
export function portalDirection(from: string[], to: string[]): PortalTransitionType {
  const fromDepth = from.length
  const toDepth = to.length

  // Inicio y las secciones son hermanos: se navega entre ellos desde la sidebar.
  if (fromDepth <= 1 && toDepth <= 1) return "portal-fade"

  // Dentro de una sección, o entre Inicio y cualquier detalle, la profundidad
  // indica si se entra o se vuelve.
  if (from[0] === to[0] || fromDepth === 0 || toDepth === 0) {
    if (toDepth > fromDepth) return "portal-forward"
    if (toDepth < fromDepth) return "portal-back"
    return "portal-fade"
  }

  // Entre secciones distintas: una lista que abre el detalle de otra sección
  // sigue siendo "entrar"; el resto (p. ej. sidebar desde un detalle) es lateral.
  if (fromDepth === 1 && toDepth >= 2) return "portal-forward"
  return "portal-fade"
}

export const portalViewTransition: ViewTransitionOptions = {
  types: ({ fromLocation, toLocation, pathChanged, hrefChanged }) => {
    // `router.invalidate()` y recargas de la misma URL no deben animar.
    if (!hrefChanged) return false

    const to = portalSegments(toLocation.pathname)
    // Salir del portal (p. ej. hacia /gestion) no anima.
    if (!to) return false

    const from = fromLocation ? portalSegments(fromLocation.pathname) : null
    if (!fromLocation || !from) return ["portal-fade"]

    if (!pathChanged) {
      // Sólo cambió el hash: no hay contenido nuevo que animar.
      if (toLocation.searchStr === fromLocation.searchStr) return false
      return ["portal-search"]
    }

    return [portalDirection(from, to)]
  },
}

/*
 * Shared element opcional (tarjeta → detalle). Sólo el elemento marcado con
 * `data-vt-shared` recibe `view-transition-name`, y únicamente durante
 * `portal-forward`/`portal-back` (ver `index.css`). Así una grilla con varias
 * tarjetas, o el ticker con sus copias duplicadas, nunca repite nombres.
 */

let lastSharedKey: string | null = null

export function sharedTransitionStyle(key: string): CSSProperties {
  return { "--vt-shared-name": `vt-${key}` } as CSSProperties
}

/** Marca `element` como origen del shared element de la próxima navegación. */
export function markSharedTransition(element: Element | null | undefined, key: string) {
  lastSharedKey = key
  document.querySelectorAll("[data-vt-shared]").forEach((node) => {
    if (node !== element) node.removeAttribute("data-vt-shared")
  })
  element?.setAttribute("data-vt-shared", "")
}

/** `true` si `key` fue el último elemento compartido (para animar el regreso). */
export function isLastSharedTransition(key: string) {
  return lastSharedKey === key
}

export function programSharedKey(programId: string) {
  return `program-${programId}`
}
