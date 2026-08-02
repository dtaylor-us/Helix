import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, googleLoginUrl } from '../api/http'
import { AppLayout } from './AppLayout'

/**
 * ADR-021: gates the whole app behind an authenticated session. Deliberately a separate component
 * from AppLayout (rather than folding this logic into it) so AppLayout's existing tests -- which
 * render it directly, with no auth context -- are unaffected.
 */
export function AuthGate() {
  const queryClient = useQueryClient()
  const { data: currentUser, isLoading, isError } = useQuery({
    queryKey: ['current-user'],
    queryFn: api.getCurrentUser,
    retry: false,
  })

  if (isLoading) {
    return (
      <div className="shell">
        <p className="muted">Loading…</p>
      </div>
    )
  }

  if (isError || !currentUser) {
    // The allowlist check happens server-side during Google's OAuth2 callback (see SecurityConfig's
    // failureUrl) -- a rejected login redirects the browser back here with this query param, since
    // there's no other way for the SPA to learn why a login attempt failed.
    const wasNotInvited = new URLSearchParams(window.location.search).get('error') === 'not_invited'
    return (
      <div className="shell login-shell">
        <div className="login-card">
          <h1>Helix</h1>
          <p className="subtitle">Discover. Practice. Become.</p>
          {wasNotInvited ? (
            <p className="muted">
              That Google account isn&rsquo;t on the Helix invite list yet. If you think this is a
              mistake, reach out to whoever invited you.
            </p>
          ) : (
            <p className="muted">Sign in with the Google account you were invited with to continue.</p>
          )}
          <a href={googleLoginUrl} className="cta-button">
            Sign in with Google
          </a>
        </div>
      </div>
    )
  }

  return (
    <AppLayout
      currentUser={currentUser}
      onLogout={() => {
        api.logout().finally(() => {
          queryClient.clear()
          window.location.href = '/'
        })
      }}
    />
  )
}
