import { createFileRoute, Link } from "@tanstack/react-router"

export const Route = createFileRoute("/loading-test")({
  loader: async () => {
    await new Promise(resolve => setTimeout(resolve, 1500))
    return { loadedAt: new Date().toLocaleTimeString() }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { loadedAt } = Route.useLoaderData()

  return (
    <div className="p-8">
      <h1 className="text-xl font-semibold">Loaded after a delay</h1>
      <p>Loader finished at {loadedAt}</p>
      <Link to="/" className="underline">
        Back home
      </Link>
    </div>
  )
}
