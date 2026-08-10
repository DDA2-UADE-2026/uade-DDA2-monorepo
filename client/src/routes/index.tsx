import { createFileRoute, Link } from "@tanstack/react-router"

export const Route = createFileRoute("/")({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <div>
      <div>Hello "/"!</div>
      <Link to="/" className="underline">
        Test loading bar
      </Link>
    </div>
  )
}
