import React from 'react';
import { Activity, Bell, BriefcaseBusiness, LayoutDashboard, LogIn, LogOut, ShieldCheck } from 'lucide-react';
import { Button } from './ui/button';

export function Navbar({ activePage, setActivePage, healthStatus, session, onSignOut }) {
  const links = session
    ? [
        { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { id: 'incidents', label: 'Incidents', icon: BriefcaseBusiness },
        { id: 'notifications', label: 'Notifications', icon: Bell },
        ...(session.user.role === 'ADMIN' ? [{ id: 'admin', label: 'Administration', icon: ShieldCheck }] : []),
        { id: 'health', label: 'System health', icon: Activity },
      ]
    : [
        { id: 'landing', label: 'Overview', icon: LayoutDashboard },
        { id: 'health', label: 'System health', icon: Activity },
      ];

  return (
    <header className="sticky top-0 z-50 border-b border-slate-800/90 bg-slate-950/95 backdrop-blur">
      <div className="mx-auto flex min-h-16 max-w-[1440px] items-center justify-between gap-4 px-4 sm:px-6">
        <button className="flex shrink-0 items-center gap-3 text-left" onClick={() => setActivePage(session ? 'dashboard' : 'landing')} aria-label="I-ITSM home">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl border border-blue-400/20 bg-blue-500/10 text-blue-300"><ShieldCheck className="h-5 w-5" /></span>
          <span><span className="block font-semibold tracking-tight text-white">I-ITSM</span><span className="hidden text-[10px] uppercase tracking-[0.16em] text-slate-500 sm:block">Service operations</span></span>
        </button>
        <nav className="no-scrollbar flex min-w-0 flex-1 items-center justify-start gap-1 overflow-x-auto px-2" aria-label="Main navigation">
          {links.map(({ id, label, icon: Icon }) => <button key={id} aria-label={label} onClick={() => setActivePage(id)} className={`flex shrink-0 items-center gap-2 rounded-lg px-2 py-2 text-xs font-medium transition lg:px-3 ${activePage === id ? 'bg-slate-800 text-white' : 'text-slate-400 hover:bg-slate-900 hover:text-white'}`}><Icon className="h-4 w-4" /><span className="hidden lg:inline">{label}</span></button>)}
        </nav>
        <div className="flex shrink-0 items-center gap-2">
          <span className={`hidden h-2 w-2 rounded-full sm:block ${healthStatus?.ok ? 'bg-emerald-400' : 'bg-amber-400'}`} title={healthStatus?.ok ? 'Backend online' : 'Backend unavailable'} />
          {session ? <>
            <div className="hidden text-right sm:block"><p className="max-w-36 truncate text-xs font-medium text-slate-200">{session.user.fullName}</p><p className="text-[10px] uppercase tracking-wider text-slate-500">{session.user.role}</p></div>
            <Button size="sm" variant="outline" onClick={onSignOut} className="gap-1.5"><LogOut className="h-3.5 w-3.5" /><span className="hidden sm:inline">Sign out</span></Button>
          </> : <Button size="sm" onClick={() => setActivePage('login')} className="gap-1.5"><LogIn className="h-3.5 w-3.5" /><span>Sign in</span></Button>}
        </div>
      </div>
    </header>
  );
}
