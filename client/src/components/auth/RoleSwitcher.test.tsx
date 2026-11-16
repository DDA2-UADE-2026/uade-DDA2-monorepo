import type { ReactNode } from "react"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { RoleSwitcher } from "@/components/auth/RoleSwitcher"
import { useLogout, useSelectRole, useSwitchRole } from "@/hooks/use-auth"

const navigate = vi.hoisted(() => vi.fn())

vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  useNavigate: () => navigate,
  Link: ({ children }: { children?: ReactNode }) => <a href="#">{children}</a>,
}))
vi.mock("@/hooks/use-auth", () => ({ useLogout: vi.fn(), useSelectRole: vi.fn(), useSwitchRole: vi.fn() }))
vi.mock("@/components/UserAvatar", () => ({ UserAvatar: () => <span aria-hidden="true" /> }))

type Mutation = ReturnType<typeof useSwitchRole>

function mutation(overrides: Partial<Mutation> = {}) {
  return { mutateAsync: vi.fn(), isPending: false, isError: false, error: null, variables: undefined, ...overrides } as unknown as Mutation
}

const user = { name: "Ana", roles: ["CIUDADANO", "TRABAJADOR_SOCIAL", "MESA_ENTRADAS"], activeRole: "CIUDADANO" }

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(useLogout).mockReturnValue({ logout: vi.fn(), isPending: false } as unknown as ReturnType<typeof useLogout>)
})

describe("RoleSwitcher al cambiar de rol", () => {
  it("cambia al rol elegido y navega a su inicio", async () => {
    const switchRole = mutation({ mutateAsync: vi.fn().mockResolvedValue({ user: { activeRole: "TRABAJADOR_SOCIAL" } }) } as Partial<Mutation>)
    vi.mocked(useSwitchRole).mockReturnValue(switchRole)
    vi.mocked(useSelectRole).mockReturnValue(mutation())
    render(<RoleSwitcher roleChoice={{ mode: "switch", user }} />)

    expect(screen.getByText("Actual")).toBeInTheDocument()
    expect(screen.getByText("Mesa Entradas")).toBeInTheDocument()
    fireEvent.click(screen.getByText("Trabajo social"))

    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: "/gestion", replace: true }))
    expect(switchRole.mutateAsync).toHaveBeenCalledWith({ body: { role: "TRABAJADOR_SOCIAL" } })
  })

  it("conservar el rol actual vuelve a su inicio sin cambiar nada", () => {
    vi.mocked(useSwitchRole).mockReturnValue(mutation())
    vi.mocked(useSelectRole).mockReturnValue(mutation())
    render(<RoleSwitcher roleChoice={{ mode: "switch", user }} />)

    fireEvent.click(screen.getByRole("button", { name: "Conservar rol actual" }))
    expect(navigate).toHaveBeenCalledWith({ to: "/portal", replace: true })
  })

  it("mantiene la pantalla y muestra el error si el cambio falla", async () => {
    const switchRole = mutation({ mutateAsync: vi.fn().mockRejectedValue(new Error("x")), isError: true, error: { message: "Rol no habilitado" } } as Partial<Mutation>)
    vi.mocked(useSwitchRole).mockReturnValue(switchRole)
    vi.mocked(useSelectRole).mockReturnValue(mutation())
    render(<RoleSwitcher roleChoice={{ mode: "switch", user }} />)

    expect(screen.getByText("Rol no habilitado")).toBeInTheDocument()
    fireEvent.click(screen.getByText("Trabajo social"))
    await waitFor(() => expect(switchRole.mutateAsync).toHaveBeenCalled())
    expect(navigate).not.toHaveBeenCalled()
  })
})
