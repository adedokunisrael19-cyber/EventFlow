import { useEffect, useState } from 'react'
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
      </section>
    </main>
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
