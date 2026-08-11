import { Outlet, createFileRoute } from "@tanstack/react-router"
import { AuthBackdrop } from '@/components/auth-backdrop';

export const Route = createFileRoute("/_auth")({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <div className="relative isolate min-h-svh overflow-hidden bg-background">
      <AuthBackdrop />
      <main className="relative z-10 grid min-h-svh place-items-center px-6 py-12">
        <Outlet />
      </main>
    </div>
  );
}

/*
 *   import { Outlet, createFileRoute } from '@tanstack/react-router';
 *   import { AuthBackdrop } from '@/components/auth-backdrop';
 *
 *   export const Route = createFileRoute('/_auth')({ component: AuthLayout });
 *
 *   function AuthLayout() {
 *     return (
 *       <div className="relative isolate min-h-svh overflow-hidden bg-background">
 *         <AuthBackdrop />
 *         <main className="relative z-10 grid min-h-svh place-items-center px-6 py-12">
 *           <Outlet />
 *         </main>
 *       </div>
 *     );
 *   }
 */