import { ProfileForm } from "@/components/profile/ProfileForm"
import { SidebarTrigger } from "@/components/ui/sidebar"

/**
 * Shared by /gestion/perfil and /portal/perfil (see notes/routes.md #7 —
 * shared presentation component, one thin route file per branch). No
 * PageHeader here, so the trigger floats to keep the sidebar reachable.
 */
function ProfilePage() {
  return (
    <div className="relative flex h-full w-full items-center justify-center px-6 py-10">
      <SidebarTrigger variant="secondary" className="absolute top-3 left-3 z-50" />
      <ProfileForm />
    </div>
  )
}

export { ProfilePage }
