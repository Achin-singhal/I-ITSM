import React, { useEffect, useState } from 'react';
import { AlertCircle, ArrowDownToLine, BriefcaseBusiness, CheckCircle2, CirclePlus, Clock3, Search, ShieldAlert } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { IncidentForm } from '../components/IncidentForm';
import { IncidentDetailPanel } from '../components/IncidentDetailPanel';
import { searchIncidents } from '../services/api';

const statusVariant = (status) => ['CLOSED', 'RESOLVED', 'VERIFIED'].includes(status) ? 'success' : ['IN_PROGRESS', 'ACKNOWLEDGED'].includes(status) ? 'warning' : 'outline';
const priorityVariant = (priority) => priority === 'CRITICAL' ? 'destructive' : priority === 'HIGH' ? 'warning' : 'default';
const prettyDate = (value) => value ? new Date(value).toLocaleDateString() : '—';
const exclusiveEnd = (date) => { if (!date) return ''; const end = new Date(`${date}T00:00:00`); end.setDate(end.getDate() + 1); return end.toISOString(); };

export function IncidentsPage({ session, notify }) {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [idFilter, setIdFilter] = useState('');
  const [status, setStatus] = useState('ALL');
  const [priority, setPriority] = useState('ALL');
  const [category, setCategory] = useState(''); const [assigneeId, setAssigneeId] = useState('');
  const [createdFrom, setCreatedFrom] = useState(''); const [createdTo, setCreatedTo] = useState(''); const [slaStatus, setSlaStatus] = useState('');
  const [page, setPage] = useState(0); const [totalElements, setTotalElements] = useState(0); const [totalPages, setTotalPages] = useState(0);
  const [selected, setSelected] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [reload, setReload] = useState(0);
  const { user } = session;
  const role = user.role;

  useEffect(() => {
    let mounted = true;
    setLoading(true); setError('');
    const params = { id: idFilter, query: search, status: status === 'ALL' ? '' : status, priority: priority === 'ALL' ? '' : priority, category, assigneeId, createdFrom: createdFrom ? new Date(`${createdFrom}T00:00:00`).toISOString() : '', createdTo: exclusiveEnd(createdTo), slaStatus, page, size: 25 };
    if (role === 'TECHNICIAN') params.assigneeId = user.id;
    searchIncidents(params).then((result) => { if (mounted) { setIncidents(result?.content || []); setTotalElements(result?.totalElements || 0); setTotalPages(result?.totalPages || 0); } })
      .catch((requestError) => { if (mounted) setError(requestError.message); })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [reload, idFilter, search, status, priority, category, assigneeId, createdFrom, createdTo, slaStatus, page, role, user.id]);

  const visible = incidents;

  function replaceIncident(updated) {
    if (updated?.id) {
      setIncidents((items) => items.map((item) => item.id === updated.id ? updated : item));
      setSelected((item) => item?.id === updated.id ? updated : item);
    }
  }

  const title = role === 'EMPLOYEE' ? 'My service requests' : role === 'TECHNICIAN' ? 'Assigned work' : 'Incident operations';
  const activeCount = visible.filter((item) => !['CLOSED', 'RESOLVED', 'VERIFIED'].includes(item.status)).length;
  const urgentCount = visible.filter((item) => ['CRITICAL', 'HIGH'].includes(item.priority)).length;

  return <div className="mx-auto max-w-[1440px] space-y-6 px-4 py-8 sm:px-6 lg:px-8">
    <section className="flex flex-col justify-between gap-5 lg:flex-row lg:items-end">
      <div><p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-300">{role.toLowerCase()} workspace</p><h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">{title}</h1><p className="mt-2 max-w-2xl text-sm text-slate-400">{role === 'TECHNICIAN' ? 'Incidents assigned to your technician account. Search and status filters run against the records returned by the backend.' : role === 'ADMIN' ? 'Monitor reported incidents and coordinate service response.' : 'Track requests you have reported and follow each update.'}</p></div>
      <Button onClick={() => { setEditing(null); setFormOpen(true); }} className="gap-2"><CirclePlus className="h-4 w-4" />Report incident</Button>
    </section>

    <div className="grid gap-3 sm:grid-cols-3">
      <Metric icon={BriefcaseBusiness} label="Visible incidents" value={visible.length} />
      <Metric icon={Clock3} label="Active work" value={activeCount} accent="text-amber-300" />
      <Metric icon={ShieldAlert} label="High priority" value={urgentCount} accent="text-rose-300" />
    </div>

    <Card className="overflow-hidden border-slate-800 bg-slate-900/70">
      <div className="space-y-3 border-b border-slate-800 p-4 sm:p-5">
        <div><h2 className="font-semibold text-white">Incident queue</h2><p className="mt-1 text-xs text-slate-500">{totalElements} matching records · filtered and paginated by the backend</p></div>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <label className="relative"><Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" /><input aria-label="Search incidents" className="form-input pl-9" placeholder="Search ID, title, description" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} /></label>
          <select aria-label="Filter by status" className="form-select" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}><option value="ALL">All statuses</option>{['NEW','OPEN','CLASSIFIED','ASSIGNED','ACKNOWLEDGED','IN_PROGRESS','RESOLVED','VERIFIED','CLOSED'].map((value) => <option key={value} value={value}>{value.replaceAll('_',' ')}</option>)}</select>
          <select aria-label="Filter by priority" className="form-select" value={priority} onChange={(e) => { setPriority(e.target.value); setPage(0); }}><option value="ALL">All priorities</option>{['CRITICAL','HIGH','MEDIUM','LOW'].map((value) => <option key={value}>{value}</option>)}</select>
          <input aria-label="Filter by incident ID" className="form-input" type="number" min="1" placeholder="Incident ID" value={idFilter} onChange={(e) => { setIdFilter(e.target.value); setPage(0); }} />
          <input aria-label="Filter by category" className="form-input" placeholder="Category" value={category} onChange={(e)=>{setCategory(e.target.value);setPage(0);}} />
          {role === 'ADMIN' && <input aria-label="Filter by technician ID" className="form-input" type="number" min="1" placeholder="Technician ID" value={assigneeId} onChange={(e)=>{setAssigneeId(e.target.value);setPage(0);}} />}
          <select aria-label="Filter by SLA status" className="form-select" value={slaStatus} onChange={(e)=>{setSlaStatus(e.target.value);setPage(0);}}><option value="">Any SLA state</option>{['HEALTHY','AT_RISK','BREACHED'].map(x=><option key={x}>{x}</option>)}</select>
          <label className="form-label">Created from<input className="form-input" type="date" value={createdFrom} onChange={e=>{setCreatedFrom(e.target.value);setPage(0);}} /></label><label className="form-label">Created to<input className="form-input" type="date" value={createdTo} onChange={e=>{setCreatedTo(e.target.value);setPage(0);}} /></label>
        </div>
      </div>
      {error && <div role="alert" className="m-4 flex items-start gap-2 rounded-lg border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-100"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{error}<button onClick={() => setReload((value) => value + 1)} className="ml-auto underline">Retry</button></div>}
      {loading ? <div className="space-y-3 p-5" aria-label="Loading incidents"><div className="h-8 animate-pulse rounded bg-slate-800" /><div className="h-12 animate-pulse rounded bg-slate-800/70" /><div className="h-12 animate-pulse rounded bg-slate-800/70" /></div> : visible.length === 0 ? <div className="flex flex-col items-center px-6 py-16 text-center"><span className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-slate-800 text-slate-400"><CheckCircle2 /></span><h3 className="font-medium text-white">{incidents.length ? 'No incidents match these filters' : 'No incidents yet'}</h3><p className="mt-1 max-w-sm text-sm text-slate-500">{incidents.length ? 'Try another status, priority, or search term.' : 'When an incident is reported, it will appear here.'}</p>{!incidents.length && <Button className="mt-4" onClick={() => setFormOpen(true)}>Report your first incident</Button>}</div> : <div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm"><thead className="bg-slate-950/60 text-[11px] uppercase tracking-wider text-slate-500"><tr><th className="px-5 py-3 font-medium">Ticket</th><th className="px-5 py-3 font-medium">Incident</th><th className="px-5 py-3 font-medium">Priority</th><th className="px-5 py-3 font-medium">Status</th><th className="px-5 py-3 font-medium">Assigned to</th><th className="px-5 py-3 font-medium">Reported</th></tr></thead><tbody className="divide-y divide-slate-800/80">{visible.map((item) => <tr key={item.id} tabIndex="0" role="button" onClick={() => setSelected(item)} onKeyDown={(e) => { if (e.key === 'Enter') setSelected(item); }} className="cursor-pointer outline-none transition hover:bg-slate-800/50 focus:bg-slate-800/50"><td className="px-5 py-4 font-mono text-xs text-blue-300">{item.ticketNumber}</td><td className="max-w-sm px-5 py-4"><p className="truncate font-medium text-slate-100">{item.title}</p><p className="mt-1 text-xs text-slate-500">{item.category} · {item.reporterName}</p></td><td className="px-5 py-4"><Badge variant={priorityVariant(item.priority)}>{item.priorityCode || item.priority}</Badge></td><td className="px-5 py-4"><Badge variant={statusVariant(item.status)}>{item.status.replaceAll('_',' ')}</Badge></td><td className="px-5 py-4 text-sm text-slate-300">{item.assigneeName || <span className="text-slate-500">Unassigned</span>}</td><td className="px-5 py-4 text-xs text-slate-400">{prettyDate(item.createdAt)}</td></tr>)}</tbody></table></div>}
      <div className="flex items-center justify-between border-t border-slate-800 px-4 py-3 text-[11px] text-slate-500 sm:px-5"><span>Page {totalPages ? page + 1 : 0} of {totalPages} · {totalElements} records</span><div className="flex items-center gap-3"><button disabled={page===0} onClick={()=>setPage(p=>p-1)} className="disabled:opacity-40">Previous</button><button disabled={page+1>=totalPages} onClick={()=>setPage(p=>p+1)} className="disabled:opacity-40">Next</button><button onClick={() => setReload((value) => value + 1)} className="inline-flex items-center gap-1.5 text-slate-400 hover:text-white"><ArrowDownToLine className="h-3.5 w-3.5" />Refresh</button></div></div>
    </Card>

    {formOpen && <IncidentForm incident={editing} onClose={() => setFormOpen(false)} onSaved={(saved) => { if (editing) replaceIncident(saved); else { setIncidents((items) => [saved, ...items]); setSelected(saved); } }} notify={notify} />}
    {selected && <IncidentDetailPanel incident={selected} session={session} onClose={() => setSelected(null)} onEdit={(item) => { setEditing(item); setFormOpen(true); }} onChanged={replaceIncident} notify={notify} />}
  </div>;
}

function Metric({ icon: Icon, label, value, accent = 'text-blue-300' }) { return <Card className="border-slate-800 bg-slate-900/65"><CardContent className="flex items-center gap-3 p-4"><span className={`flex h-10 w-10 items-center justify-center rounded-xl bg-slate-800 ${accent}`}><Icon className="h-5 w-5" /></span><div><p className="text-xs text-slate-500">{label}</p><p className="mt-0.5 text-xl font-semibold text-white">{value}</p></div></CardContent></Card>; }
