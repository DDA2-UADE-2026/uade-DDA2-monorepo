import { useRouterState } from '@tanstack/react-router'
import { useEffect, useState } from 'react'

export function AppLoadingBar() {
  // Suscribirse al estado del router
  const isLoading = useRouterState({ select: s => s.status === 'pending' })
  const [progress, setProgress] = useState(0)
  const [wasLoading, setWasLoading] = useState(isLoading)

  if (isLoading !== wasLoading) {
    setWasLoading(isLoading)
    setProgress(isLoading ? 10 : 100)
  }

  useEffect(() => {
    if (!isLoading) return

    const interval = setInterval(() => {
      setProgress(prev => Math.min(90, prev + Math.random() * 10))
    }, 300)

    return () => clearInterval(interval)
  }, [isLoading])

  useEffect(() => {
    if (isLoading || progress === 0) return

    const timeout = setTimeout(() => setProgress(0), 300)
    return () => clearTimeout(timeout)
  }, [isLoading, progress])

  if (progress === 0) return null

  return (
    <div className="top-0 left-0 w-full h-0 z-50 fixed overflow-visible">
      <div
        className="h-0.5 origin-left bg-[#5d39ff] shadow-[0_0_8px_#5d39ff99] transition-transform duration-200 ease-out will-change-transform"
        style={{ transform: `scaleX(${progress / 100})` }}
      />
    </div>
  )
}
