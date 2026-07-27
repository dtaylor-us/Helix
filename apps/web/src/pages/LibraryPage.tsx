import { Link } from '@tanstack/react-router'
import { WisdomPage } from './WisdomPage'

export function LibraryPage() {
  return (
    <div className="stack">
      <section className="card intro-card">
        <h2>Library</h2>
        <p className="muted">
          Wisdom: a lesson you have tested or recognized and want to carry forward. Your weekly retrospective and
          accepted wisdom live here, kept alongside the reflections and evidence they came from.
        </p>
      </section>

      <WisdomPage />

      <section className="card stack">
        <h2>Looking for something specific?</h2>
        <p className="muted">These grow more useful as you accumulate reflections, beliefs, and wisdom over time.</p>
        <div className="row">
          <Link to="/search" className="secondary-button">
            Search all records
          </Link>
          <Link to="/knowledge" className="secondary-button">
            Explore beliefs &amp; evidence
          </Link>
          <Link to="/settings/memory" className="secondary-button">
            Review remembered facts
          </Link>
        </div>
      </section>
    </div>
  )
}
