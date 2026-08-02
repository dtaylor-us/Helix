import { useRouter } from '@tanstack/react-router'

type BackNavigationProps = {
  fallbackTo: '/today' | '/transformations' | '/knowledge' | '/library' | '/settings/memory'
  label: string
}

/** Returns to the actual prior in-app view, with a stable parent route for direct entry/refresh. */
export function BackNavigation({ fallbackTo, label }: BackNavigationProps) {
  const router = useRouter()

  return (
    <button
      type="button"
      className="back-navigation"
      onClick={() => {
        if (router.history.canGoBack()) {
          router.history.back()
        } else {
          void router.navigate({ to: fallbackTo })
        }
      }}
      aria-label={`Back to ${label}`}
    >
      <span aria-hidden="true">←</span>
      {label}
    </button>
  )
}
