import { IconCalendar } from "@tabler/icons-react"
import { format } from "date-fns"
import { es } from "date-fns/locale"
import { useState } from "react"

import { formatLocalDate, parseLocalDate } from "@/components/programs/ProgramRouteUi"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"

export function ProgramDatePicker({
  value,
  onChange,
  placeholder = "Seleccionar fecha",
  disabled,
}: {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  disabled?: React.ComponentProps<typeof Calendar>["disabled"]
}) {
  const [open, setOpen] = useState(false)
  const date = parseLocalDate(value)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            type="button"
            variant="outline"
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
          disabled={disabled}
          onSelect={(selected) => {
            onChange(formatLocalDate(selected))
            if (selected) setOpen(false)
          }}
        />
      </PopoverContent>
    </Popover>
  )
}
