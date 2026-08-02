import { Link, Outlet } from '@tanstack/react-router'
import { useEffect, useRef, useState } from 'react'
import type { CurrentUser } from '../../../../packages/contracts/src/index'

const primaryLinks = [
  { to: '/today', label: 'Today', description: 'Your current focus and next step' },
  { to: '/transformations', label: 'Journey', description: 'The change you are working toward' },
  { to: '/library', label: 'Library', description: 'Wisdom you have kept, and how to find more' },
] as const

const secondaryLinks = [
  { to: '/search', label: 'Search' },
  { to: '/knowledge', label: 'Knowledge graph' },
  { to: '/settings/memory', label: 'Memories' },
  { to: '/settings/export', label: 'Export & delete data' },
  { to: '/settings', label: 'Settings' },
] as const

const activeLinkProps = { className: 'nav-link nav-link-active', 'aria-current': 'page' as const }

interface AppLayoutProps {
  // Optional: AuthGate passes this once signed in. Left optional (with no fallback UI) so this
  // component's own existing tests, which render it directly with no auth context, are unaffected.
  currentUser?: CurrentUser
  onLogout?: () => void
}

export function AppLayout({ currentUser, onLogout }: AppLayoutProps = {}) {
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false)
  const moreMenuRef = useRef<HTMLDetailsElement>(null)

  useEffect(() => {
    if (!isMoreMenuOpen) return

    const closeWhenOutside = (event: Event) => {
      if (!moreMenuRef.current?.contains(event.target as Node)) {
        setIsMoreMenuOpen(false)
      }
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsMoreMenuOpen(false)
        moreMenuRef.current?.querySelector<HTMLElement>('summary')?.focus()
      }
    }

    document.addEventListener('pointerdown', closeWhenOutside)
    document.addEventListener('focusin', closeWhenOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeWhenOutside)
      document.removeEventListener('focusin', closeWhenOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [isMoreMenuOpen])

  return (
    <div className="shell">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <header className="shell-header">
        <div>
          <h1>Helix</h1>
          <p className="subtitle">Discover. Practice. Become.</p>
        </div>
        <nav aria-label="Primary">
          <ul className="nav-list">
            {primaryLinks.map((item) => (
              <li key={item.to}>
                <Link to={item.to} className="nav-link" activeProps={activeLinkProps} title={item.description}>
                  {item.label}
                </Link>
              </li>
            ))}
            <li>
              <details
                ref={moreMenuRef}
                className="more-menu"
                open={isMoreMenuOpen}
              >
                <summary
                  className="nav-link more-menu-trigger"
                  onClick={(event) => {
                    event.preventDefault()
                    setIsMoreMenuOpen((isOpen) => !isOpen)
                  }}
                >
                  More
                </summary>
                <ul className="more-menu-list">
                  {secondaryLinks.map((item) => (
                    <li key={item.to}>
                      <Link
                        to={item.to}
                        className="nav-link"
                        activeProps={activeLinkProps}
                        onClick={() => setIsMoreMenuOpen(false)}
                      >
                        {item.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </details>
            </li>
          </ul>
        </nav>
        {currentUser && (
          <div className="account-menu">
            <span className="muted">{currentUser.displayName || currentUser.email}</span>
            {onLogout && (
              <button type="button" className="secondary-button" onClick={onLogout}>
                Sign out
              </button>
            )}
          </div>
        )}
      </header>
      <main id="main-content" className="content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
