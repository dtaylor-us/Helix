import { Link, Outlet } from '@tanstack/react-router'

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

export function AppLayout() {
  return (
    <div className="shell">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <header className="shell-header">
        <div>
          <h1>Helix</h1>
          <p className="subtitle">A private place to turn who you want to become into small, tested steps.</p>
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
              <details className="more-menu">
                <summary className="nav-link more-menu-trigger">More</summary>
                <ul className="more-menu-list">
                  {secondaryLinks.map((item) => (
                    <li key={item.to}>
                      <Link to={item.to} className="nav-link" activeProps={activeLinkProps}>
                        {item.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </details>
            </li>
          </ul>
        </nav>
      </header>
      <main id="main-content" className="content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
