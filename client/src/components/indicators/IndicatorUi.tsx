import { IconArrowDownRight, IconArrowUpRight, IconChartBar, IconTable } from "@tabler/icons-react"
import { useState, type ReactNode } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

export type TableView = {
  columns: string[]
  rows: Array<Array<string | number>>
}

/**
 * Card de un gráfico. Toda card tiene su vista de tabla: es la alternativa accesible al color
 * y deja cada valor al alcance sin depender del tooltip.
 */
export function ChartCard({ title, description, table, children, className, footer }: {
  title: string
  description?: ReactNode
  table: TableView
  children: ReactNode
  className?: string
  footer?: ReactNode
}) {
  const [showTable, setShowTable] = useState(false)
  return (
    <Card className={cn("min-w-0", className)}>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
        <CardAction>
          <Button
            size="xs"
            variant="ghost"
            aria-pressed={showTable}
            onClick={() => setShowTable((value) => !value)}
          >
            {showTable ? <IconChartBar /> : <IconTable />}
            {showTable ? "Ver gráfico" : "Ver tabla"}
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent className="min-w-0">
        {showTable ? <DataTable table={table} /> : children}
        {footer}
      </CardContent>
    </Card>
  )
}

function DataTable({ table }: { table: TableView }) {
  if (table.rows.length === 0) return <EmptyChart />
  return (
    <div className="max-h-80 overflow-auto rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            {table.columns.map((column, index) => (
              <TableHead key={column} className={index > 0 ? "text-right" : undefined}>{column}</TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {table.rows.map((row, rowIndex) => (
            <TableRow key={rowIndex}>
              {row.map((cell, index) => (
                <TableCell key={index} className={index > 0 ? "text-right tabular-nums" : undefined}>{cell}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

export function EmptyChart({ message = "Sin datos para el período elegido." }: { message?: string }) {
  return <p className="flex min-h-40 items-center justify-center rounded-lg border border-dashed text-sm text-muted-foreground">{message}</p>
}

/** Métrica suelta: etiqueta, valor y, opcionalmente, la variación contra el período anterior. */
export function StatTile({ label, value, hint, change, upIsGood = true, className }: {
  label: string
  value: string
  hint?: ReactNode
  change?: number | null
  upIsGood?: boolean
  className?: string
}) {
  return (
    <div className={cn("min-w-0 rounded-xl border bg-card p-4", className)}>
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 font-heading text-3xl font-semibold">{value}</p>
      {change != null && <Change value={change} upIsGood={upIsGood} />}
      {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
    </div>
  )
}

function Change({ value, upIsGood }: { value: number; upIsGood: boolean }) {
  const up = value > 0
  const flat = value === 0
  const good = flat ? null : up === upIsGood
  const Icon = up ? IconArrowUpRight : IconArrowDownRight
  const text = `${up ? "+" : ""}${Math.round(value * 100)}% vs. período anterior`
  return (
    <p className={cn(
      "mt-1 flex items-center gap-1 text-xs font-medium",
      good === null ? "text-muted-foreground" : good ? "text-emerald-700 dark:text-emerald-400" : "text-red-700 dark:text-red-400",
    )}>
      {!flat && <Icon aria-hidden="true" className="size-3.5" />}
      {text}
    </p>
  )
}

/** Clave de leyenda: el color va en la marca, nunca en el texto. */
export function LegendKey({ color, label, value, shape = "square" }: {
  color: string
  label: string
  value?: string
  shape?: "square" | "line"
}) {
  return (
    <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
      <span
        aria-hidden="true"
        className={shape === "line" ? "h-0.5 w-3 rounded-full" : "size-2.5 rounded-sm"}
        style={{ backgroundColor: color }}
      />
      <span className="text-foreground">{label}</span>
      {value && <span className="tabular-nums">{value}</span>}
    </span>
  )
}

export function SectionTitle({ title, description }: { title: string; description?: string }) {
  return (
    <div className="px-1 pt-2">
      <h2 className="font-heading text-lg font-medium">{title}</h2>
      {description && <p className="text-sm text-muted-foreground">{description}</p>}
    </div>
  )
}
