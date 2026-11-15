import React, { useState } from 'react';
import { ArrowRight, LockKeyhole, Mail, ShieldCheck, UserRound } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { loginUser, registerUser, saveSession } from '../services/api';

export function LoginPage({ onAuthenticated, onBack, notify }) {
  const [registering, setRegistering] = useState(false);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      const auth = registering
        ? await registerUser({ email, password, fullName })
        : await loginUser(email, password);
      const session = saveSession(auth);
      onAuthenticated(session);
      notify(registering ? 'Your employee account is ready.' : `Welcome back, ${session.user.fullName}.`, 'success');
    } catch (requestError) {
      setError([requestError.message, ...requestError.errors].filter(Boolean).join(' '));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto grid min-h-[calc(100vh-9rem)] max-w-6xl items-center gap-12 px-4 py-10 md:grid-cols-[1fr_440px] md:px-8">
      <section className="hidden space-y-6 md:block">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-blue-400/20 bg-blue-400/10 text-blue-300"><ShieldCheck /></div>
        <div className="space-y-3">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-blue-300">I-ITSM Service Desk</p>
          <h1 className="max-w-xl text-5xl font-semibold leading-tight tracking-tight text-white">Keep every service request moving.</h1>
          <p className="max-w-lg text-base leading-7 text-slate-400">Sign in to follow incidents, coordinate response, and keep service commitments visible.</p>
        </div>
        <div className="grid max-w-lg grid-cols-3 gap-3 text-xs text-slate-400">
          {['Incident tracking', 'Team assignment', 'SLA visibility'].map((item) => <div key={item} className="rounded-xl border border-slate-800 bg-slate-900/60 p-3">{item}</div>)}
        </div>
      </section>

      <Card className="border-slate-700/80 bg-slate-900/95 shadow-2xl shadow-black/20">
        <form onSubmit={handleSubmit}>
          <CardHeader className="space-y-2">
            <div className="mb-2 flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10 text-blue-300 md:hidden"><ShieldCheck /></div>
            <CardTitle className="text-2xl">{registering ? 'Create your account' : 'Sign in to your workspace'}</CardTitle>
            <CardDescription>{registering ? 'Registration creates an employee account.' : 'Use your I-ITSM account credentials.'}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {registering && <label className="block space-y-1.5 text-sm text-slate-300">Full name
              <span className="relative block"><UserRound className="absolute left-3 top-3 h-4 w-4 text-slate-500" /><input autoComplete="name" value={fullName} onChange={(e) => setFullName(e.target.value)} required maxLength={120} className="form-input pl-10" /></span>
            </label>}
            <label className="block space-y-1.5 text-sm text-slate-300">Email
              <span className="relative block"><Mail className="absolute left-3 top-3 h-4 w-4 text-slate-500" /><input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required className="form-input pl-10" /></span>
            </label>
            <label className="block space-y-1.5 text-sm text-slate-300">Password
              <span className="relative block"><LockKeyhole className="absolute left-3 top-3 h-4 w-4 text-slate-500" /><input type="password" autoComplete={registering ? 'new-password' : 'current-password'} value={password} onChange={(e) => setPassword(e.target.value)} required minLength={registering ? 6 : undefined} className="form-input pl-10" /></span>
            </label>
            {error && <div role="alert" className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{error}</div>}
            <p className="text-xs leading-5 text-slate-500">Authentication is handled by the backend. New registrations always receive the EMPLOYEE role.</p>
          </CardContent>
          <CardFooter className="flex-col gap-3">
            <Button type="submit" disabled={busy} className="w-full gap-2">{busy ? 'Please wait…' : registering ? 'Create employee account' : 'Sign in'} {!busy && <ArrowRight className="h-4 w-4" />}</Button>
            <button type="button" onClick={() => { setRegistering((value) => !value); setError(''); }} className="text-sm text-blue-300 hover:text-blue-200">{registering ? 'Already registered? Sign in' : 'Need an account? Register'}</button>
            <button type="button" onClick={onBack} className="text-xs text-slate-500 hover:text-slate-300">Back to overview</button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
