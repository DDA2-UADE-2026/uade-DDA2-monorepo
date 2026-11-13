import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { expect, it, vi } from "vitest"

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarInput,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSkeleton,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarSeparator,
} from "./sidebar"

it("compone los elementos de navegación, acciones, submenú y esqueleto con sus atributos accesibles", async () => {
  const action = vi.fn()
  const user = userEvent.setup()
  render(
    <SidebarProvider>
      <Sidebar collapsible="none" data-testid="sidebar">
        <SidebarHeader>Municipalidad</SidebarHeader>
        <SidebarInput aria-label="Buscar en el menú" />
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel>Gestión</SidebarGroupLabel>
            <SidebarGroupAction aria-label="Agregar sección" onClick={action}>+</SidebarGroupAction>
            <SidebarGroupContent>
              <SidebarMenu>
                <SidebarMenuItem>
                  <SidebarMenuButton isActive variant="outline" size="sm" tooltip="Programas">Programas</SidebarMenuButton>
                  <SidebarMenuAction aria-label="Acciones de programas" showOnHover>⋯</SidebarMenuAction>
                  <SidebarMenuBadge>3</SidebarMenuBadge>
                  <SidebarMenuSub>
                    <SidebarMenuSubItem><SidebarMenuSubButton href="/programas" isActive size="sm">Vigentes</SidebarMenuSubButton></SidebarMenuSubItem>
                  </SidebarMenuSub>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
          <SidebarMenuSkeleton showIcon data-testid="menu-skeleton" />
          <SidebarSeparator />
        </SidebarContent>
        <SidebarFooter>Usuario de prueba</SidebarFooter>
      </Sidebar>
      <SidebarInset data-testid="main-content">Contenido</SidebarInset>
    </SidebarProvider>,
  )

  expect(screen.getByTestId("sidebar")).toHaveTextContent("Municipalidad")
  expect(screen.getByLabelText("Buscar en el menú")).toHaveAttribute("data-sidebar", "input")
  expect(screen.getByRole("button", { name: "Programas" })).toHaveAttribute("data-active")
  expect(screen.getByRole("link", { name: "Vigentes" })).toHaveAttribute("href", "/programas")
  expect(screen.getByText("3")).toHaveAttribute("data-sidebar", "menu-badge")
  expect(screen.getByTestId("menu-skeleton").querySelector('[data-sidebar="menu-skeleton-icon"]')).toBeInTheDocument()
  expect(screen.getByTestId("main-content")).toHaveTextContent("Contenido")
  expect(screen.getByText("Usuario de prueba")).toBeInTheDocument()

  await user.click(screen.getByRole("button", { name: "Agregar sección" }))
  expect(action).toHaveBeenCalledOnce()
  expect(screen.getByRole("button", { name: "Acciones de programas" })).toBeInTheDocument()
})
