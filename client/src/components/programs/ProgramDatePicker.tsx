import { IconCalendar } from "@tabler/icons-react"
import { format } from "date-fns"
import { es } from "date-fns/locale"
import { useState } from "react"
import type { DateRange, Matcher } from "react-day-picker"

import { formatLocalDate, parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"

export function ProgramDatePicker({
  id,
  value,
  onChange,
  placeholder = "Seleccionar fecha",
  disabled,
  readOnly = false,
  occupied = [],
  occupiedLabel = "Ocupado",
}: {
  id?: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  disabled?: Matcher | Matcher[]
  readOnly?: boolean
  /** Rangos tomados por otros registros: se muestran con otro color y no se pueden elegir. */
  occupied?: DateRange[]
  occupiedLabel?: string
}) {
  const [open, setOpen] = useState(false)
  const date = parseLocalDate(value)
  const disabledMatchers = [...(Array.isArray(disabled) ? disabled : disabled ? [disabled] : []), ...occupied]

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            id={id}
            type="button"
            variant="outline"
            disabled={readOnly}
            data-empty={!date}
            className="w-full justify-start text-left font-normal data-[empty=true]:text-muted-foreground"
          />
        }
      >
        <IconCalendar />
        {date ? format(date, "PPP", { locale: es }) : <span>{placeholder}</span>}
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          locale={es}
          selected={date}
          defaultMonth={date}
          disabled={disabledMatchers}
          modifiers={{ occupied }}
          modifiersClassNames={{
            // La celda deshabilitada trae opacity-50; acá se fuerza el color completo para que se distinga.
            occupied: "opacity-100! [&>button]:bg-amber-500/20 [&>button]:text-amber-800 [&>button]:line-through dark:[&>button]:text-amber-200",
          }}
          onSelect={(selected) => {
            if (readOnly) return
            onChange(formatLocalDate(selected))
            if (selected) setOpen(false)
          }}
        />
        {occupied.length > 0 && (
          <p className="flex items-center gap-2 border-t px-3 py-2 text-xs text-muted-foreground">
            <span aria-hidden="true" className="size-3 rounded-sm bg-amber-500/40" />
            {occupiedLabel}
          </p>
        )}
      </PopoverContent>
    </Popover>
  )
}
