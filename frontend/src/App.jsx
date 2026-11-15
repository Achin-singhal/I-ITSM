import React, { useCallback, useEffect, useState } from 'react';
import { Footer } from './components/Footer';
import { Navbar } from './components/Navbar';
import { LandingPage } from './pages/LandingPage';
import { HealthPage } from './pages/HealthPage';
import { IncidentsPage } from './pages/IncidentsPage';
import { LoginPage } from './pages/LoginPage';
import { AdminPage } from './pages/AdminPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { checkBackendHealth, clearSession, readSession, setUnauthorizedHandler } from './services/api';

export function App() {
  const [session, setSession] = useState(() => readSession());
  const [activePage, setActivePage] = useState(() => readSession() ? 'dashboard' : 'landing');
  const [healthStatus, setHealthStatus] = useState(null);
  const [loadingHealth, setLoadingHealth] = useState(true);
  const [toast, setToast] = useState(null);

  const notify = useCallback((message, tone = 'info') => {
    setToast({ message, tone });
    window.clearTimeout(notify.timer);
    notify.timer = window.setTimeout(() => setToast(null), 4200);
  }, []);

  const signOut = useCallback((message = 'You have been signed out.') => {
    clearSession();
    setSession(null);
    setActivePage('landing');
    if (message) notify(message, 'info');
  }, [notify]);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setSession(null);
      setActivePage('login');
      notify('Your session expired. Sign in again to continue.', 'error');
    });
    return () => setUnauthorizedHandler(null);
  }, [notify]);

  useEffect(() => {
    if (!session?.expiresAt) return undefined;
    const timer = window.setTimeout(() => {
      clearSession();
      setSession(null);
      setActivePage('login');
      notify('Your session expired. Please sign in again.', 'info');
    }, Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(timer);
  }, [session, notify]);

  useEffect(() => {
    let mounted = true;
    const refresh = async () => {
      setLoadingHealth(true);
      const result = await checkBackendHealth();
      if (mounted) { setHealthStatus(result); setLoadingHealth(false); }
    };
    refresh();
    const interval = window.setInterval(refresh, 30000);
    return () => { mounted = false; window.clearInterval(interval); };
  }, []);

  const navigate = (page) => {
    const protectedPages = ['dashboard', 'incidents', 'notifications', 'admin'];
    if (protectedPages.includes(page) && !readSession()) {
      setActivePage('login');
      return;
    }
    if (page === 'admin' && session?.user?.role !== 'ADMIN') return;
    setActivePage(page);
  };

  const authenticated = (nextSession) => {
    setSession(nextSession);
    setActivePage('dashboard');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 selection:bg-blue-500/40 selection:text-white">
      <Navbar activePage={activePage} setActivePage={navigate} healthStatus={healthStatus} session={session} onSignOut={() => signOut()} />
      <main className="min-h-[calc(100vh-16rem)]">
        {activePage === 'landing' && <LandingPage setActivePage={navigate} healthStatus={healthStatus} loadingHealth={loadingHealth} />}
        {activePage === 'health' && <HealthPage healthStatus={healthStatus} setHealthStatus={setHealthStatus} />}
        {activePage === 'login' && <LoginPage onAuthenticated={authenticated} onBack={() => setActivePage('landing')} notify={notify} />}
        {activePage === 'dashboard' && session && <IncidentsPage session={session} notify={notify} />}
        {activePage === 'incidents' && session && <IncidentsPage session={session} notify={notify} />}
        {activePage === 'notifications' && session && <NotificationsPage notify={notify} />}
        {activePage === 'admin' && session?.user?.role === 'ADMIN' && <AdminPage notify={notify} />}
      </main>
      {toast && <div role="status" className={`fixed bottom-5 right-4 z-[70] max-w-sm rounded-xl border px-4 py-3 text-sm shadow-xl ${toast.tone === 'error' ? 'border-rose-400/30 bg-rose-950 text-rose-100' : toast.tone === 'success' ? 'border-emerald-400/30 bg-emerald-950 text-emerald-100' : 'border-slate-700 bg-slate-900 text-slate-100'}`}>{toast.message}</div>}
      <Footer />
    </div>
  );
}

export default App;
