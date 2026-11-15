import React, { useEffect, useState } from 'react';
import { Bell, CheckCheck, CircleAlert, RefreshCw } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card } from '../components/ui/card';
import { fetchNotifications, markNotificationRead } from '../services/api';

const date = (value) => value ? new Date(value).toLocaleString() : '';

export function NotificationsPage({ notify }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let active = true; setLoading(true); setError('');
    fetchNotifications().then((result) => { if (active) setItems(result || []); }).catch((err) => { if (active) setError(err.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);

  async function markRead(id) {
    setBusyId(id);
    try { const updated = await markNotificationRead(id); setItems((current) => current.map((item) => item.id === id ? updated : item)); notify('Notification marked as read.', 'success'); }
    catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }
  const unread = items.filter((item) => !item.read).length;

  return <div className="mx-auto max-w-5xl space-y-6 px-4 py-8 sm:px-6 lg:px-8">
    <div className="flex items-end justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-300">Your activity</p><h1 className="mt-2 text-3xl font-semibold text-white">Notifications</h1><p className="mt-2 text-sm text-slate-400">Incident updates delivered to your account.</p></div><Button variant="outline" size="sm" onClick={() => setReload((value) => value + 1)} className="gap-2"><RefreshCw className="h-3.5 w-3.5" />Refresh</Button></div>
    <div className="flex gap-3 text-xs"><Badge variant="outline">{items.length} total</Badge><Badge variant={unread ? 'warning' : 'success'}>{unread} unread</Badge></div>
    {error && <div role="alert" className="flex gap-2 rounded-lg border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-100"><CircleAlert className="h-4 w-4 shrink-0" />{error}</div>}
    {loading ? <div className="h-28 animate-pulse rounded-xl bg-slate-900" /> : items.length === 0 ? <Card className="flex flex-col items-center p-12 text-center"><Bell className="h-8 w-8 text-slate-600" /><h2 className="mt-3 font-medium text-white">You’re all caught up</h2><p className="mt-1 text-sm text-slate-500">New incident notifications will appear here.</p></Card> : <div className="space-y-3">{items.map((item) => <Card key={item.id} className={`flex flex-col gap-4 p-4 sm:flex-row sm:items-start sm:justify-between ${item.read ? 'border-slate-800 bg-slate-900/50' : 'border-blue-500/30 bg-blue-950/20'}`}><div className="flex gap-3"><span className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${item.read ? 'bg-slate-800 text-slate-400' : 'bg-blue-500/10 text-blue-300'}`}><Bell className="h-4 w-4" /></span><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-sm font-semibold text-white">{item.title}</h2>{!item.read && <Badge variant="warning">Unread</Badge>}</div><p className="mt-1 text-sm leading-6 text-slate-300">{item.message}</p><p className="mt-2 text-xs text-slate-500">{item.incidentId ? `Incident #${item.incidentId} · ` : ''}{item.channel} · {date(item.createdAt)}</p></div></div>{!item.read && <Button size="sm" variant="outline" disabled={busyId === item.id} onClick={() => markRead(item.id)} className="shrink-0 gap-2"><CheckCheck className="h-3.5 w-3.5" />{busyId === item.id ? 'Saving…' : 'Mark read'}</Button>}</Card>)}</div>}
  </div>;
}
