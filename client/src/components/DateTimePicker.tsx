import { IconCalendar } from "@tabler/icons-react"
import { format } from "date-fns"
import { es } from "date-fns/locale"
import { useState } from "react"

import { formatLocalDate, parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Input } from "@/components/ui/input"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { cn } from "@/lib/utils"

/**
 * shadcn date + time picker. Speaks the same `YYYY-MM-DDTHH:mm` string as
 * <input type="datetime-local">, and emits "" until both parts are chosen.
 */
export function DateTimePicker({
  id,
  value,
  onChange,
  min,
  timeLabel = "Hora",
  required,
  className,
}: {
  id?: string
  value: string
  onChange: (value: string) => void
  /** Earliest allowed moment, same format as `value`. */
  min?: string
  timeLabel?: string
  required?: boolean
  className?: string
}) {
  const [open, setOpen] = useState(false)
  const [valueDate = "", valueTime = ""] = value ? value.split("T") : []
  // Keep a half-filled pick (date without time or vice versa) while `value` is still "".
  const [draft, setDraft] = useState({ date: valueDate, time: valueTime.slice(0, 5) })
  const datePart = value ? valueDate : draft.date
  const timePart = value ? valueTime.slice(0, 5) : draft.time
  const date = parseLocalDate(datePart)
  const minDate = min?.slice(0, 10)
  const minTime = min && datePart === minDate ? min.slice(11, 16) : undefined

  const update = (next: { date?: string; time?: string }) => {
    const merged = { date: next.date ?? datePart, time: next.time ?? timePart }
    setDraft(merged)
    onChange(merged.date && merged.time ? `${merged.date}T${merged.time}` : "")
  }

  return (
    <div className={cn("flex min-w-0 gap-2", className)}>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          render={
            <Button
              id={id}
              type="button"
              variant="outline"
              data-empty={!date}
              aria-required={required}
              className="min-w-0 flex-1 justify-start text-left font-normal data-[empty=true]:text-muted-foreground"
            />
          }
        >
          <IconCalendar />
          <span className="truncate">{date ? format(date, "PPP", { locale: es }) : "Seleccionar fecha"}</span>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            locale={es}
            selected={date}
            defaultMonth={date}
            disabled={minDate ? { before: parseLocalDate(minDate)! } : undefined}
            onSelect={(selected) => {
              if (!selected) return
              update({ date: formatLocalDate(selected) })
              setOpen(false)
            }}
          />
        </PopoverContent>
      </Popover>
      <Input
        type="time"
        aria-label={timeLabel}
        required={required}
        step={60}
        min={minTime}
        value={timePart}
        onChange={(event) => update({ time: event.target.value })}
        className="w-28 shrink-0 appearance-none bg-background [&::-webkit-calendar-picker-indicator]:hidden [&::-webkit-calendar-picker-indicator]:appearance-none"
      />
    </div>
  )
}
