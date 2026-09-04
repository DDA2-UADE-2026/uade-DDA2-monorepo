import { Fragment, type ReactNode } from "react"
import { Link } from "@tanstack/react-router"

import { OutletNavContent } from "@/components/layout/OutletNav"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"

export type OutletBreadcrumbItem = {
  label: ReactNode
  to?: string
}

export function OutletNavBreadcrumbs({ items }: { items: OutletBreadcrumbItem[] }) {
  return (
    <OutletNavContent className="overflow-hidden">
      <Breadcrumb>
        <BreadcrumbList className="flex-nowrap overflow-hidden">
          {items.map((item, index) => {
            const current = index === items.length - 1
            const title = typeof item.label === "string" ? item.label : undefined

            return (
              <Fragment key={`${item.to ?? "current"}-${index}`}>
                {index > 0 && <BreadcrumbSeparator className="hidden sm:list-item" />}
                <BreadcrumbItem className={current ? "min-w-0" : "hidden min-w-0 sm:inline-flex"}>
                  {current || !item.to ? (
                    <BreadcrumbPage className="block truncate" title={title}>
                      {item.label}
                    </BreadcrumbPage>
                  ) : (
                    <BreadcrumbLink
                      className="block max-w-32 truncate lg:max-w-48"
                      title={title}
                      render={<Link to={item.to as string} />}
                    >
                      {item.label}
                    </BreadcrumbLink>
                  )}
                </BreadcrumbItem>
              </Fragment>
            )
          })}
        </BreadcrumbList>
      </Breadcrumb>
    </OutletNavContent>
  )
}
