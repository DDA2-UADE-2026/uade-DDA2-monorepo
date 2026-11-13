import { render, screen } from "@testing-library/react"
import { expect, it, vi } from "vitest"

import {
  OutletNav,
  OutletNavContent,
  OutletNavContentFlex,
  OutletNavRightButton,
  OutletNavSeparator,
  OutletNavSidebarTrigger,
  OutletNavStatic,
  OutletNavSticky,
  OutletNavVerticalSpace,
  SidebarShell,
  SidebarShellContent,
} from "./OutletNav"

vi.mock("@/components/ui/SidebarTriggerFull", () => ({ SidebarTriggerFull: () => <button type="button">Abrir menú</button> }))

it("compone variantes de navegación y distribuye correctamente el contenido del encabezado", () => {
  const view = render(
    <>
      <OutletNav aria-label="Principal" data-testid="navigation">
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavContent><OutletNavContentFlex>Programas</OutletNavContentFlex></OutletNavContent>
        <OutletNavRightButton><button type="button">Perfil</button></OutletNavRightButton>
      </OutletNav>
      <OutletNavStatic data-testid="static-navigation">Encabezado fijo</OutletNavStatic>
      <OutletNavSticky data-testid="sticky-navigation">Encabezado adherente</OutletNavSticky>
      <OutletNavVerticalSpace data-testid="navigation-space" />
      <SidebarShell data-testid="sidebar-shell"><SidebarShellContent>Contenido lateral</SidebarShellContent></SidebarShell>
      <OutletNavSeparator data-testid="separator" />
    </>,
  )

  expect(screen.getByTestId("navigation")).toHaveAttribute("aria-label", "Principal")
  expect(screen.getByText("Programas")).toBeInTheDocument()
  expect(screen.getByRole("button", { name: "Abrir menú" })).toBeInTheDocument()
  expect(screen.getByRole("button", { name: "Perfil" })).toBeInTheDocument()
  expect(screen.getByTestId("static-navigation")).toHaveTextContent("Encabezado fijo")
  expect(screen.getByTestId("sticky-navigation")).toHaveTextContent("Encabezado adherente")
  expect(screen.getByTestId("navigation-space")).toBeInTheDocument()
  expect(screen.getByTestId("sidebar-shell")).toHaveTextContent("Contenido lateral")
  expect(screen.getByTestId("separator")).toBeInTheDocument()
  expect(view.container.querySelectorAll("span.w-px")).toHaveLength(2)
  expect(view.container.querySelector("header.sticky")).toBeInTheDocument()
})
