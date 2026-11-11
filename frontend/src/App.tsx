import { type FormEvent, useEffect, useState } from 'react'
import './App.css'

type ApiStatus = 'checking' | 'connected' | 'unavailable'

function App() {
  const verificationToken = new URLSearchParams(window.location.search).get('token')

  if (window.location.pathname === '/verify-email' && verificationToken) {
    return <EmailVerificationPage token={verificationToken} />
  }

  return <FoundationPage />
}

function FoundationPage() {
  const [apiStatus, setApiStatus] = useState<ApiStatus>('checking')
  const [accessToken, setAccessToken] = useState<string | null>(null)

  useEffect(() => {
    const checkApi = async () => {
      try {
        const response = await fetch('/api/v1/health')
        if (!response.ok) throw new Error('The API returned an error')
        const health: { status: string } = await response.json()
        setApiStatus(health.status === 'UP' ? 'connected' : 'unavailable')
      } catch {
        setApiStatus('unavailable')
      }
    }

    void checkApi()
  }, [])

  if (accessToken) {
    return <WorkspaceSession token={accessToken} onLogout={() => setAccessToken(null)} />
  }

  const statusText = {
    checking: 'Checking API connection',
    connected: 'API connected',
    unavailable: 'API unavailable',
  }[apiStatus]

  return (
    <main className="min-h-screen bg-stone-50 text-slate-900">
      <header className="flex items-center justify-between border-b border-stone-200 bg-white px-6 py-4 sm:px-10">
        <a className="text-xl font-semibold tracking-tight text-emerald-900" href="/">
          EventFlow
        </a>
        <span className="rounded-full bg-stone-100 px-3 py-1 text-xs font-medium text-stone-600">
          Foundation
        </span>
      </header>

      <section className="mx-auto flex max-w-5xl flex-col gap-8 px-6 py-16 sm:px-10 sm:py-24">
        <div className="max-w-2xl">
          <p className="mb-3 text-sm font-semibold uppercase tracking-[0.16em] text-emerald-800">
            Your planning workspace
          </p>
          <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
            Plan better. Manage smarter.
          </h1>
          <p className="mt-5 text-lg leading-8 text-slate-600">
            EventFlow is taking shape. This foundation connects the React app to
            the Java API and prepares the project for workspace features.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <article className="rounded-2xl border border-stone-200 bg-white p-6 shadow-sm">
            <h2 className="font-semibold">Application status</h2>
            <p className="mt-2 text-sm text-slate-600">
              React and TypeScript are ready for feature development.
            </p>
          </article>
          <article className="rounded-2xl border border-stone-200 bg-white p-6 shadow-sm">
            <h2 className="font-semibold">Backend connection</h2>
            <p
              aria-live="polite"
              className={`mt-2 text-sm ${apiStatus === 'connected' ? 'text-emerald-700' : 'text-slate-600'}`}
            >
              {statusText}
            </p>
          </article>
        </div>

        <LoginForm onLogin={setAccessToken} />
      </section>
    </main>
  )
}

type LoginResponse = { accessToken: string; tokenType: string; expiresAt: string }
type CurrentUser = {
  email: string
  fullName: string
  workspaceName: string
  role: string
}

function LoginForm({ onLogin }: { onLogin: (token: string) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const signIn = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const response = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      if (!response.ok) {
        const problem = (await response.json().catch(() => ({}))) as { detail?: string }
        setError(problem.detail ?? 'Sign-in failed. Check your details and email verification.')
        return
      }
      const authentication: LoginResponse = await response.json()
      onLogin(authentication.accessToken)
      setPassword('')
    } catch {
      setError('EventFlow could not reach the sign-in service. Try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={(event) => void signIn(event)} className="max-w-md rounded-2xl border border-stone-200 bg-white p-6 shadow-sm">
      <h2 className="font-semibold">Sign in</h2>
      <p className="mt-2 text-sm text-slate-600">Use the email and password for your verified account.</p>
      <label className="mt-5 block text-sm font-medium" htmlFor="login-email">Email address</label>
      <input
        autoComplete="email"
        className="mt-2 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-700/20"
        id="login-email"
        onChange={(event) => setEmail(event.target.value)}
        required
        type="email"
        value={email}
      />
      <label className="mt-4 block text-sm font-medium" htmlFor="login-password">Password</label>
      <input
        autoComplete="current-password"
        className="mt-2 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-700/20"
        id="login-password"
        onChange={(event) => setPassword(event.target.value)}
        required
        type="password"
        value={password}
      />
      {error && <p aria-live="polite" className="mt-3 text-sm text-red-700">{error}</p>}
      <button
        className="mt-5 w-full rounded-lg bg-emerald-800 px-4 py-3 text-sm font-semibold text-white hover:bg-emerald-900 focus:outline-none focus:ring-2 focus:ring-emerald-700 focus:ring-offset-2 disabled:cursor-wait disabled:opacity-60"
        disabled={submitting}
        type="submit"
      >
        {submitting ? 'Signing in…' : 'Sign in'}
      </button>
    </form>
  )
}

function WorkspaceSession({ token, onLogout }: { token: string; onLogout: () => void }) {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loggingOut, setLoggingOut] = useState(false)

  useEffect(() => {
    let cancelled = false
    const loadCurrentUser = async () => {
      try {
        const response = await fetch('/api/v1/auth/me', {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!response.ok) throw new Error('Your session expired. Sign in again.')
        const user: CurrentUser = await response.json()
        if (!cancelled) setCurrentUser(user)
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause.message : 'Could not load your account.')
      }
    }
    void loadCurrentUser()
    return () => { cancelled = true }
  }, [token])

  const signOut = async () => {
    setLoggingOut(true)
    try {
      const response = await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (response.status === 401) {
        onLogout()
        return
      }
      if (!response.ok) throw new Error('Sign-out failed. Try again.')
      onLogout()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Sign-out failed. Try again.')
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <section className="max-w-xl rounded-2xl border border-stone-200 bg-white p-8 shadow-sm">
      <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-800">Your workspace</p>
      {currentUser ? (
        <>
          <h1 className="mt-3 text-2xl font-semibold">Welcome, {currentUser.fullName}</h1>
          <p className="mt-2 text-slate-600">{currentUser.workspaceName}</p>
          <p className="mt-1 text-sm text-slate-500">{currentUser.email} · {currentUser.role.replaceAll('_', ' ')}</p>
        </>
      ) : (
        <p aria-live="polite" className="mt-3 text-sm text-slate-600">{error ?? 'Loading your workspace…'}</p>
      )}
      {error && currentUser && <p aria-live="polite" className="mt-4 text-sm text-red-700">{error}</p>}
      <button
        className="mt-6 rounded-lg border border-stone-300 px-4 py-2 text-sm font-semibold hover:bg-stone-50 disabled:opacity-60"
        disabled={loggingOut}
        onClick={() => void signOut()}
        type="button"
      >
        {loggingOut ? 'Signing out…' : 'Sign out'}
      </button>
    </section>
  )
}

type VerificationStatus = 'ready' | 'verifying' | 'verified' | 'invalid'

function EmailVerificationPage({ token }: { token: string }) {
  const [status, setStatus] = useState<VerificationStatus>('ready')

  useEffect(() => {
    // The token is retained in component state; remove it from browser history before the user continues.
    window.history.replaceState({}, document.title, window.location.pathname)
  }, [])

  const verifyEmail = async () => {
    setStatus('verifying')
    try {
      const response = await fetch('/api/v1/auth/verify-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      })
      setStatus(response.ok ? 'verified' : 'invalid')
    } catch {
      setStatus('invalid')
    }
  }

  const message = {
    ready: 'Confirm that you own this email address to activate your EventFlow account.',
    verifying: 'Verifying your email address…',
    verified: 'Your email is verified. You can now continue to EventFlow.',
    invalid: 'This verification link is invalid or has expired. Request a new link to continue.',
  }[status]

  return (
    <main className="flex min-h-screen items-center justify-center bg-stone-50 px-6 py-16 text-slate-900">
      <section className="w-full max-w-md rounded-2xl border border-stone-200 bg-white p-8 shadow-sm">
        <a className="text-xl font-semibold tracking-tight text-emerald-900" href="/">
          EventFlow
        </a>
        <h1 className="mt-8 text-2xl font-semibold tracking-tight">Verify your email</h1>
        <p aria-live="polite" className="mt-3 text-sm leading-6 text-slate-600">
          {message}
        </p>
        {status === 'ready' && (
          <button
            className="mt-6 w-full rounded-lg bg-emerald-800 px-4 py-3 text-sm font-semibold text-white hover:bg-emerald-900 focus:outline-none focus:ring-2 focus:ring-emerald-700 focus:ring-offset-2"
            onClick={() => void verifyEmail()}
          >
            Verify email address
          </button>
        )}
        {status === 'verified' && (
          <a className="mt-6 inline-flex text-sm font-semibold text-emerald-800" href="/">
            Continue to EventFlow
          </a>
        )}
      </section>
    </main>
  )
}

export default App
