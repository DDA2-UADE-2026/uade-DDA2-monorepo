import { createFileRoute } from "@tanstack/react-router"

import { ProfilePage } from "@/components/profile/ProfilePage"

export const Route = createFileRoute("/_app/portal/perfil")({
  component: RouteComponent,
})

function RouteComponent() {
  return <ProfilePage displayType="portal" />
}
