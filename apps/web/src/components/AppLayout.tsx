import { Link, Outlet } from '@tanstack/react-router'

const links = [
  { to: '/today', label: 'Today' },
  { to: '/transformations', label: 'Transformations' },
  { to: '/wisdom', label: 'Wisdom' },
  { to: '/search', label: 'Search' },
  { to: '/knowledge', label: 'Knowledge' },
  { to: '/settings', label: 'Settings' },
] as const

export function AppLayout() {
  return (
    <div className="shell">
      <header className="shell-header">
        <div>
          <h1>Helix</h1>
          <p className="subtitle">A private place for experiments, reflection, and personal wisdom.</p>
        </div>
        <nav aria-label="Primary">
          <ul className="nav-list">
            {links.map((item) => (
              <li key={item.to}>
                <Link to={item.to} className="nav-link">
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
