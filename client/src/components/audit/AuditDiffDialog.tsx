import { useMemo, useState } from "react"
import { IconArrowsDiff } from "@tabler/icons-react"
import { diff } from "jsondiffpatch"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import type { LogResponse } from "@/generated/types.gen"
import { cn } from "@/lib/utils"

interface DiffLine {
  type: "unchanged" | "added" | "removed"
  indent: number
  text: string
}

function parseJsonValues(value?: string): unknown {
  if (!value) return undefined

  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value)
}

function pushValueLines(
  lines: DiffLine[],
  indent: number,
  type: DiffLine["type"],
  prefix: string,
  value: unknown,
  suffix: string,
) {
  const json = value === undefined ? "undefined" : JSON.stringify(value, null, 2)
  const jsonLines = json.split("\n")

  jsonLines.forEach((jsonLine, index) => {
    const isFirst = index === 0
    const isLast = index === jsonLines.length - 1
    lines.push({
      type,
      indent,
      text: `${isFirst ? prefix : ""}${jsonLine}${isLast ? suffix : ""}`,
    })
  })
}

// A delta entry is either an added/modified/deleted/textdiff/moved tuple
// (see jsondiffpatch's Delta type), or a nested object/array delta. Rather
// than reconstructing values from the delta's own tuple encoding, we walk
// the original old/new values in lockstep so nested keys and out-of-order
// keys resolve naturally, and just use the delta to decide what changed.
function walkValue(
  lines: DiffLine[],
  indent: number,
  prefix: string,
  suffix: string,
  oldValue: unknown,
  newValue: unknown,
  deltaEntry: unknown,
) {
  if (deltaEntry === undefined) {
    pushValueLines(lines, indent, "unchanged", prefix, oldValue, suffix)
    return
  }

  if (Array.isArray(deltaEntry)) {
    const isDeleted = deltaEntry.length === 3 && deltaEntry[2] === 0
    const isAdded = deltaEntry.length === 1

    if (isAdded) {
      pushValueLines(lines, indent, "added", prefix, newValue, suffix)
      return
    }
    if (isDeleted) {
      pushValueLines(lines, indent, "removed", prefix, oldValue, suffix)
      return
    }
    // modified, textdiff, or moved: show old vs. new in full
    pushValueLines(lines, indent, "removed", prefix, oldValue, suffix)
    pushValueLines(lines, indent, "added", prefix, newValue, suffix)
    return
  }

  if (isRecord(deltaEntry)) {
    if (deltaEntry._t === "a") {
      // array structural diff: show as a whole-value replacement
      pushValueLines(lines, indent, "removed", prefix, oldValue, suffix)
      pushValueLines(lines, indent, "added", prefix, newValue, suffix)
      return
    }

    lines.push({ type: "unchanged", indent, text: `${prefix}{` })
    walkObjectChildren(
      lines,
      indent + 1,
      isRecord(oldValue) ? oldValue : {},
      isRecord(newValue) ? newValue : {},
      deltaEntry,
    )
    lines.push({ type: "unchanged", indent, text: `}${suffix}` })
  }
}

function walkObjectChildren(
  lines: DiffLine[],
  indent: number,
  oldObj: Record<string, unknown>,
  newObj: Record<string, unknown>,
  delta: Record<string, unknown>,
) {
  const keys = Object.keys(oldObj)
  for (const key of Object.keys(newObj)) {
    if (!keys.includes(key)) keys.push(key)
  }

  for (const key of keys) {
    walkValue(lines, indent, `${key}: `, ",", oldObj[key], newObj[key], delta[key])
  }
}

function buildDiffLines(oldValue: unknown, newValue: unknown): DiffLine[] {
  const delta = diff(oldValue, newValue)
  if (delta === undefined) return []

  const lines: DiffLine[] = []
  walkValue(lines, 0, "", "", oldValue, newValue, delta)
  return lines
}

export function AuditDiffDialog({
  log,
}: {
  log: Pick<LogResponse, "entityId" | "oldValues" | "newValues">
}) {
  const [open, setOpen] = useState(false)

  const diffLines = useMemo(() => {
    const oldValue = parseJsonValues(log.oldValues)
    const newValue = parseJsonValues(log.newValues)
    return buildDiffLines(oldValue, newValue)
  }, [log.oldValues, log.newValues])

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button size="xs" />}>
        <IconArrowsDiff />
        Ver diff
      </DialogTrigger>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle>Diferencias del cambio</DialogTitle>
          <DialogDescription>
            Comparación entre el estado anterior y posterior de la entidad{" "}
            <span className="font-mono">{log.entityId ?? "—"}</span>.
          </DialogDescription>
        </DialogHeader>

        {diffLines.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No hay diferencias entre los valores registrados.
          </p>
        ) : (
          <pre className="m-0 max-h-[60vh] overflow-auto rounded-2xl border bg-muted/20 p-3 font-mono text-xs leading-relaxed">
            {diffLines.map((line, index) => (
              <span
                key={index}
                className={cn(
                  "block rounded-sm px-1",
                  line.type === "added" &&
                    "bg-emerald-500/15 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300",
                  line.type === "removed" &&
                    "bg-destructive/10 text-destructive line-through decoration-destructive/30 dark:bg-destructive/20",
                  line.type === "unchanged" && "text-muted-foreground",
                )}
                style={{ paddingLeft: `${line.indent * 16 + 4}px` }}
              >
                {line.text}
              </span>
            ))}
          </pre>
        )}
      </DialogContent>
    </Dialog>
  )
}
