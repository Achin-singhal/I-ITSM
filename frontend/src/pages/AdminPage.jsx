import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Activity, ClipboardList, Settings2, UsersRound, ChartNoAxesCombined, Tag, MessageSquareText, Server } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { configureTechnician, createSlaPolicy, deactivateSlaPolicy, fetchAudit, fetchReportAnalytics, fetchSlaPolicies, fetchTechnicianDirectory, setAssignmentStrategy, updateSlaPolicy } from '../services/api';
import { ReportsPanel } from '../components/ReportsPanel';
import { AdminManagementPanel } from '../components/AdminManagementPanel';

const tabs = [
  { id: 'overview', label: 'Overview', icon: Activity },
  { id: 'reports', label: 'Reports', icon: ChartNoAxesCombined },
  { id: 'assignment', label: 'Assignment', icon: UsersRound },
  { id: 'sla', label: 'SLA policies', icon: Settings2 },
  { id: 'audit', label: 'Audit history', icon: ClipboardList },
  { id: 'users', label: 'Users', icon: UsersRound },
  { id: 'categories', label: 'Categories', icon: Tag },
  { id: 'feedback', label: 'Feedback', icon: MessageSquareText },
  { id: 'services', label: 'IT services', icon: Server },
];
const date = (value) => value ? new Date(value).toLocaleString() : '—';

export function AdminPage({ notify }) {
  const [tab, setTab] = useState('overview');
  const [analytics, setAnalytics] = useState(null);
  const [technicians, setTechnicians] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [audit, setAudit] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  const [technicianRefreshVersion, setTechnicianRefreshVersion] = useState(0);
  const technicianRequestSequence = useRef(0);
  const [strategy, setStrategy] = useState('WORKLOAD_BALANCED');
  const [savedStrategy, setSavedStrategy] = useState('');
  const [userId, setUserId] = useState('');
  const [profile, setProfile] = useState({ available: true, experienceYears: 0, maxActiveWorkload: 10, skills: '' });
  const [profileResult, setProfileResult] = useState(null);
  const [policyForm, setPolicyForm] = useState(null);
  const [auditIncident, setAuditIncident] = useState('');
  const [auditAction, setAuditAction] = useState('');
  const [auditTargetEntity, setAuditTargetEntity] = useState('');
  const [auditTargetId, setAuditTargetId] = useState('');
  const [auditReload, setAuditReload] = useState(0);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true; setLoading(true); setError('');
    const analyticsRequest = fetchReportAnalytics().then((report) => {
      if (active) setAnalytics(report);
    }).catch((err) => {
      if (active) setError(err.message);
    });
    const policiesRequest = fetchSlaPolicies().then((slaPolicies) => {
      if (active) setPolicies(slaPolicies || []);
    }).catch((err) => {
      if (active) setError(err.message);
    });
    Promise.all([analyticsRequest, policiesRequest]).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);

  const refreshTechnicianDirectory = useCallback(() => {
    setTechnicianRefreshVersion((version) => version + 1);
  }, []);

  useEffect(() => {
    if (tab !== 'assignment') return undefined;
    let active = true;
    const requestSequence = ++technicianRequestSequence.current;
    fetchTechnicianDirectory().then((directory) => {
      if (active && requestSequence === technicianRequestSequence.current) {
        setTechnicians(directory || []);
      }
    }).catch((err) => {
      if (active && requestSequence === technicianRequestSequence.current) setError(err.message);
    });
    return () => { active = false; };
  }, [tab, technicianRefreshVersion]);

  useEffect(() => {
    if (tab !== 'audit') return;
    let active = true;
    fetchAudit({ incidentId: auditIncident, action: auditAction, targetEntity: auditTargetEntity, targetId: auditTargetId }).then((rows) => { if (active) setAudit(rows || []); }).catch((err) => { if (active) setError(err.message); });
    return () => { active = false; };
  }, [tab, auditIncident, auditAction, auditTargetEntity, auditTargetId, auditReload]);


  async function saveStrategy(event) {
    event.preventDefault(); setBusy(true);
    try { const configured = await setAssignmentStrategy(strategy); setSavedStrategy(configured); notify(`Assignment strategy set to ${configured}.`, 'success'); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function saveProfile(event) {
    event.preventDefault(); setBusy(true); setError(''); setProfileResult(null);
    const skills = profile.skills.split(',').map((skill) => skill.trim()).filter(Boolean);
    try {
      const result = await configureTechnician(userId, { available: profile.available, experienceYears: Number(profile.experienceYears), maxActiveWorkload: Number(profile.maxActiveWorkload), skills });
      setProfileResult(result); notify('Technician profile updated.', 'success');
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function submitPolicy(event) {
    event.preventDefault(); setBusy(true); setError('');
    const body = { ...policyForm, responseTimeMinutes: Number(policyForm.responseTimeMinutes), resolutionTimeMinutes: Number(policyForm.resolutionTimeMinutes) };
    try {
      if (policyForm.id) await updateSlaPolicy(policyForm.id, body); else await createSlaPolicy(body);
      setPolicyForm(null); setReload((value) => value + 1); notify('SLA policy saved.', 'success');
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function deactivate(policy) {
    if (!window.confirm(`Deactivate the ${policy.name} policy? Existing SLA history will be retained.`)) return;
    try { await deactivateSlaPolicy(policy.id); setReload((value) => value + 1); notify('SLA policy deactivated.', 'success'); }
    catch (err) { setError(err.message); }
  }

  return <div className="mx-auto max-w-[1440px] space-y-6 px-4 py-8 sm:px-6 lg:px-8">
    <div><p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-300">Control center</p><h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">Administration</h1><p className="mt-2 text-sm text-slate-400">Manage users, IT services, incident categories, feedback, assignment, SLA targets, reports, and audit review.</p></div>
    <div className="flex gap-1 overflow-x-auto rounded-xl border border-slate-800 bg-slate-900/60 p-1">{tabs.map(({ id, label, icon: Icon }) => <button key={id} onClick={() => { setTab(id); setError(''); }} className={`flex shrink-0 items-center gap-2 rounded-lg px-3 py-2.5 text-xs font-medium ${tab === id ? 'bg-slate-800 text-white' : 'text-slate-400 hover:text-white'}`}><Icon className="h-4 w-4" />{label}</button>)}</div>
    {error && <div role="alert" className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-100">{error}</div>}
    {loading ? <div className="h-32 animate-pulse rounded-xl bg-slate-900" /> : <>
      {tab === 'overview' && <section className="space-y-5"><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><Stat label="All incidents" value={analytics?.summary?.totalIncidents ?? '—'} /><Stat label="Open" value={analytics?.summary?.openIncidents ?? '—'} /><Stat label="In progress" value={analytics?.summary?.inProgressIncidents ?? '—'} /><Stat label="Critical / high" value={analytics?.summary?.criticalHighIncidents ?? '—'} /><Stat label="Resolved" value={analytics?.summary?.resolvedIncidents ?? '—'} /><Stat label="Closed" value={analytics?.summary?.closedIncidents ?? '—'} /><Stat label="SLA warnings" value={analytics?.summary?.slaWarnings ?? '—'} /><Stat label="SLA breaches" value={analytics?.summary?.slaBreaches ?? '—'} /></div><div className="grid gap-4 md:grid-cols-2"><Card className="p-5"><p className="text-xs text-slate-500">SLA compliance</p><p className="mt-2 text-2xl font-semibold text-white">{analytics?.summary?.slaCompliancePercentage == null ? '—' : `${analytics.summary.slaCompliancePercentage.toFixed(1)}%`}</p></Card><Card className="p-5"><p className="text-xs text-slate-500">Average resolution time</p><p className="mt-2 text-2xl font-semibold text-white">{analytics?.summary?.averageResolutionSeconds == null ? '—' : `${(analytics.summary.averageResolutionSeconds / 3600).toFixed(1)} hours`}</p></Card></div><Button variant="outline" onClick={() => setReload((value) => value + 1)}>Refresh dashboard data</Button></section>}
      {tab === 'reports' && <ReportsPanel />}
      {['users', 'categories', 'feedback', 'services'].includes(tab) && <AdminManagementPanel type={tab} notify={notify} onTechnicianDirectoryChanged={refreshTechnicianDirectory} />}

      {tab === 'assignment' && <section className="grid gap-5 lg:grid-cols-2"><Card className="p-5"><h2 className="font-semibold text-white">Routing strategy</h2><p className="mt-1 text-xs leading-5 text-slate-500">Select a strategy and save it to update the current configuration.</p><form onSubmit={saveStrategy} className="mt-5 space-y-4"><label className="form-label">Assignment strategy<select className="form-select" value={strategy} onChange={(e) => setStrategy(e.target.value)}><option value="WORKLOAD_BALANCED">Workload balanced</option><option value="SKILL_BASED">Skill based</option><option value="ROUND_ROBIN">Round robin</option></select></label><Button type="submit" disabled={busy}>Save strategy</Button>{savedStrategy && <Badge variant="success">Configured: {savedStrategy}</Badge>}</form></Card><Card className="p-5"><h2 className="font-semibold text-white">Technician profile</h2><p className="mt-1 text-xs leading-5 text-slate-500">Choose a technician from the authorized directory.</p><form onSubmit={saveProfile} className="mt-5 space-y-4"><label className="form-label">Technician<select className="form-select" value={userId} onChange={(e) => setUserId(e.target.value)} required><option value="">Select technician</option>{technicians.map((t) => <option key={t.userId} value={t.userId}>{t.name} · #{t.userId}</option>)}</select></label><label className="flex items-center gap-2 text-sm text-slate-300"><input type="checkbox" checked={profile.available} onChange={(e) => setProfile({ ...profile, available: e.target.checked })} className="accent-blue-500" /> Available for assignments</label><div className="grid gap-3 sm:grid-cols-2"><label className="form-label">Experience (years)<input className="form-input" type="number" min="0" value={profile.experienceYears} onChange={(e) => setProfile({ ...profile, experienceYears: e.target.value })} required /></label><label className="form-label">Max active workload<input className="form-input" type="number" min="1" value={profile.maxActiveWorkload} onChange={(e) => setProfile({ ...profile, maxActiveWorkload: e.target.value })} required /></label></div><label className="form-label">Skills, comma separated<input className="form-input" value={profile.skills} onChange={(e) => setProfile({ ...profile, skills: e.target.value })} placeholder="APPLICATION, NETWORK" required /></label><Button type="submit" disabled={busy}>Save technician profile</Button></form>{profileResult && <div className="mt-4 rounded-lg bg-slate-950 p-3 text-xs text-slate-300">Saved profile for {profileResult.name} · workload {profileResult.activeWorkload}/{profileResult.maxActiveWorkload}</div>}</Card></section>}

      {tab === 'sla' && <section className="space-y-4"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-semibold text-white">Response and resolution targets</h2><p className="mt-1 text-xs text-slate-500">Targets are measured in elapsed minutes and apply by incident priority.</p></div><Button onClick={() => setPolicyForm({ name: '', priority: 'HIGH', responseTimeMinutes: 15, resolutionTimeMinutes: 120, active: true })}>Add SLA policy</Button></div><div className="grid gap-3 md:grid-cols-2">{policies.map((policy) => <Card key={policy.id} className="p-5"><div className="flex items-start justify-between gap-3"><div><div className="flex items-center gap-2"><h3 className="font-semibold text-white">{policy.name}</h3><Badge variant={policy.active ? 'success' : 'outline'}>{policy.active ? 'Active' : 'Inactive'}</Badge></div><p className="mt-1 text-xs text-slate-500">Priority {policy.priority}</p></div><button onClick={() => setPolicyForm({ ...policy })} className="text-xs text-blue-300 hover:text-blue-200">Edit</button></div><div className="mt-5 grid grid-cols-2 gap-3"><Target label="Response" minutes={policy.responseTimeMinutes} /><Target label="Resolution" minutes={policy.resolutionTimeMinutes} /></div>{policy.active && <Button variant="ghost" size="sm" className="mt-4 text-rose-300" onClick={() => deactivate(policy)}>Deactivate policy</Button>}</Card>)}</div></section>}

      {tab === 'audit' && <section className="space-y-4"><div><h2 className="font-semibold text-white">Audit history</h2><p className="mt-1 text-xs text-slate-500">Filter the actual audit records available to administrators.</p></div><div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-5"><input aria-label="Incident ID filter" className="form-input" type="number" min="1" placeholder="Incident ID" value={auditIncident} onChange={(e) => setAuditIncident(e.target.value)} /><input aria-label="Audit action filter" className="form-input" placeholder="Action (e.g. ASSIGNED)" value={auditAction} onChange={(e) => setAuditAction(e.target.value)} /><input aria-label="Target entity filter" className="form-input" placeholder="Target (e.g. USER)" value={auditTargetEntity} onChange={(e) => setAuditTargetEntity(e.target.value)} /><input aria-label="Target ID filter" className="form-input" type="number" min="1" placeholder="Target ID" value={auditTargetId} onChange={(e) => setAuditTargetId(e.target.value)} /><Button variant="outline" onClick={() => setAuditReload((value) => value + 1)}>Refresh audit</Button></div>{audit.length === 0 ? <Card className="p-10 text-center text-sm text-slate-500">No audit entries match these filters.</Card> : <div className="overflow-x-auto rounded-xl border border-slate-800"><table className="w-full min-w-[860px] text-left text-xs"><thead className="bg-slate-900 text-slate-500"><tr>{['When','Action','Incident','Target','Actor','Description'].map((heading) => <th key={heading} className="px-4 py-3 font-medium">{heading}</th>)}</tr></thead><tbody className="divide-y divide-slate-800">{audit.map((row) => <tr key={row.id}><td className="px-4 py-3 text-slate-400">{date(row.timestamp)}</td><td className="px-4 py-3"><Badge variant="outline">{row.action}</Badge></td><td className="px-4 py-3 text-slate-300">{row.incidentId ?? '—'}</td><td className="px-4 py-3 text-slate-300">{row.targetEntity ? `${row.targetEntity} #${row.targetId}` : '—'}</td><td className="px-4 py-3 text-slate-300">{row.actorEmail || 'SYSTEM'}</td><td className="max-w-sm px-4 py-3 text-slate-400">{row.description}</td></tr>)}</tbody></table></div>}</section>}
    </>}

    {policyForm && <PolicyDialog policy={policyForm} setPolicy={setPolicyForm} onCancel={() => setPolicyForm(null)} onSubmit={submitPolicy} busy={busy} />}
  </div>;
}

function Stat({ label, value }) { return <Card className="border-slate-800 bg-slate-900/70 p-4"><p className="text-xs text-slate-500">{label}</p><p className="mt-2 text-2xl font-semibold text-white">{value}</p></Card>; }
function Target({ label, minutes }) { return <div className="rounded-lg bg-slate-950/70 p-3"><p className="text-[10px] uppercase tracking-wider text-slate-500">{label} target</p><p className="mt-1 text-lg font-semibold text-white">{minutes} <span className="text-xs font-normal text-slate-500">min</span></p></div>; }
function PolicyDialog({ policy, setPolicy, onCancel, onSubmit, busy }) { return <div className="fixed inset-0 z-[60] flex items-end justify-center bg-black/70 p-0 sm:items-center sm:p-4"><form onSubmit={onSubmit} className="w-full max-w-lg space-y-4 rounded-t-2xl border border-slate-700 bg-slate-900 p-5 sm:rounded-2xl sm:p-6"><h2 className="text-lg font-semibold text-white">{policy.id ? 'Edit SLA policy' : 'New SLA policy'}</h2><label className="form-label">Policy name<input className="form-input" value={policy.name} onChange={(e) => setPolicy({ ...policy, name: e.target.value })} required maxLength={100} /></label><label className="form-label">Priority<select className="form-select" value={policy.priority} onChange={(e) => setPolicy({ ...policy, priority: e.target.value })}>{['CRITICAL','HIGH','MEDIUM','LOW'].map((value) => <option key={value}>{value}</option>)}</select></label><div className="grid grid-cols-2 gap-3"><label className="form-label">Response (minutes)<input className="form-input" type="number" min="1" value={policy.responseTimeMinutes} onChange={(e) => setPolicy({ ...policy, responseTimeMinutes: e.target.value })} required /></label><label className="form-label">Resolution (minutes)<input className="form-input" type="number" min="1" value={policy.resolutionTimeMinutes} onChange={(e) => setPolicy({ ...policy, resolutionTimeMinutes: e.target.value })} required /></label></div><label className="flex items-center gap-2 text-sm text-slate-300"><input type="checkbox" checked={policy.active} onChange={(e) => setPolicy({ ...policy, active: e.target.checked })} className="accent-blue-500" /> Policy active</label><div className="flex justify-end gap-2 border-t border-slate-800 pt-4"><Button type="button" variant="outline" onClick={onCancel}>Cancel</Button><Button disabled={busy}>{busy ? 'Saving…' : 'Save policy'}</Button></div></form></div>; }
