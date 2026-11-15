import React, { useEffect, useState } from 'react';
import { AlertTriangle, ArrowRightLeft, Clock3, FileClock, MessageSquareText, UserRound, X } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { addActivity, assignRecommendedTechnician, checkDuplicateIncidents, fetchActivities, fetchAssignmentHistory, fetchIncident, fetchMyIncidentFeedback, fetchRankedRecommendations, fetchRelatedIncidents, fetchSlaHistory, fetchSlaStatus, submitIncidentFeedback, transitionIncident } from '../services/api';

const date = (value) => value ? new Date(value).toLocaleString() : '—';
const statusStyle = (status) => status === 'CLOSED' || status === 'RESOLVED' || status === 'VERIFIED' ? 'success' : status === 'IN_PROGRESS' || status === 'ACKNOWLEDGED' ? 'warning' : 'outline';
const priorityStyle = (priority) => priority === 'CRITICAL' ? 'destructive' : priority === 'HIGH' ? 'warning' : 'default';

export function IncidentDetailPanel({ incident, session, onClose, onEdit, onChanged, notify }) {
  const [activities, setActivities] = useState([]);
  const [sla, setSla] = useState(null);
  const [slaHistory, setSlaHistory] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [relatedIncidents, setRelatedIncidents] = useState([]);
  const [possibleDuplicates, setPossibleDuplicates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activityType, setActivityType] = useState(session.user.role === 'EMPLOYEE' ? 'EMPLOYEE_RESPONSE' : 'INVESTIGATION_NOTE');
  const [message, setMessage] = useState('');
  const [resolution, setResolution] = useState('');
  const [strategy, setStrategy] = useState('');
  const [recommendation, setRecommendation] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [feedbackRating, setFeedbackRating] = useState('5');
  const [feedbackComments, setFeedbackComments] = useState('');
  const [feedbackError, setFeedbackError] = useState('');
  const role = session.user.role;
  const isOwner = Number(session.user.id) === Number(incident.reporterId);
  const isAssigned = Number(session.user.id) === Number(incident.assigneeId);

  async function loadDetails() {
    setLoading(true); setError('');
    setPossibleDuplicates([]); setRelatedIncidents([]);
    const results = await Promise.allSettled([
      fetchActivities(incident.id), fetchSlaStatus(incident.id), fetchSlaHistory(incident.id),
      ['ADMIN', 'TECHNICIAN'].includes(role) ? fetchAssignmentHistory(incident.id) : Promise.resolve([]),
      ['ADMIN', 'TECHNICIAN'].includes(role) ? fetchRelatedIncidents(incident.id) : Promise.resolve([]),
      role === 'EMPLOYEE' && isOwner ? checkDuplicateIncidents({
        title: incident.title,
        description: incident.description,
        category: incident.category,
        affectedService: incident.affectedService,
      }) : Promise.resolve([]),
    ]);
    if (results[0].status === 'fulfilled') setActivities(results[0].value || []);
    if (results[1].status === 'fulfilled') setSla(results[1].value);
    if (results[2].status === 'fulfilled') setSlaHistory(results[2].value || []);
    if (results[3].status === 'fulfilled') setAssignments(results[3].value || []);
    if (results[4].status === 'fulfilled') setRelatedIncidents(results[4].value || []);
    if (results[5].status === 'fulfilled') {
      setPossibleDuplicates((results[5].value || []).filter((match) => Number(match.incidentId) !== Number(incident.id)));
    }
    const failed = results.find((result) => result.status === 'rejected');
    if (failed) setError(failed.reason?.message || 'Some incident details could not be loaded.');
    setLoading(false);
  }

  useEffect(() => { loadDetails(); }, [incident.id]);
  useEffect(() => {
    if (role !== 'EMPLOYEE' || !isOwner || !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)) return;
    fetchMyIncidentFeedback(incident.id).then(setFeedback).catch((err) => { if (err.status !== 404) setFeedbackError(err.message); });
  }, [incident.id, incident.status, role, isOwner]);

  async function act(action) {
    setBusy(true); setError('');
    try {
      const updated = await action();
      if (updated?.status) onChanged(updated);
      await loadDetails();
    } catch (requestError) { setError([requestError.message, ...requestError.errors].filter(Boolean).join(' ')); }
    finally { setBusy(false); }
  }

  const transitionButtons = [];
  if (role === 'TECHNICIAN' && isAssigned && incident.status === 'ASSIGNED') transitionButtons.push(['ACKNOWLEDGED', 'Acknowledge']);
  if (role === 'TECHNICIAN' && isAssigned && incident.status === 'ACKNOWLEDGED') transitionButtons.push(['IN_PROGRESS', 'Start investigation']);
  if (role === 'TECHNICIAN' && isAssigned && incident.status === 'IN_PROGRESS') transitionButtons.push(['RESOLVED', 'Resolve incident']);
  if (role === 'EMPLOYEE' && isOwner && incident.status === 'RESOLVED') transitionButtons.push(['VERIFIED', 'Verify resolution']);
  if (role === 'ADMIN' && incident.status === 'VERIFIED') transitionButtons.push(['CLOSED', 'Close incident']);

  async function performTransition(next) {
    if (next === 'RESOLVED' && !resolution.trim()) { setError('Add resolution details before resolving the incident.'); return; }
    if (next === 'CLOSED' && !window.confirm(`Close ${incident.ticketNumber}? This is the final lifecycle state.`)) return;
    await act(async () => {
      const updated = await transitionIncident(incident.id, next, next === 'RESOLVED' ? resolution : undefined);
      notify(`Incident moved to ${next}.`, 'success'); setResolution(''); return updated;
    });
  }

  async function submitActivity(event) {
    event.preventDefault(); if (!message.trim()) return;
    await act(async () => {
      await addActivity(incident.id, activityType, message.trim());
      setMessage(''); notify('Update added to the incident.', 'success');
    });
  }

  async function submitFeedback(event) {
    event.preventDefault(); setBusy(true); setFeedbackError('');
    try {
      const saved = await submitIncidentFeedback(incident.id, feedbackRating, feedbackComments.trim());
      setFeedback(saved); notify('Your feedback was submitted.', 'success');
    } catch (requestError) { setFeedbackError([requestError.message, ...requestError.errors].filter(Boolean).join(' ')); }
    finally { setBusy(false); }
  }

  async function getRecommendation() {
    setBusy(true); setError('');
    try { setRecommendation(await fetchRankedRecommendations(incident.id, strategy)); }
    catch (requestError) { setError(requestError.message); setRecommendation(null); }
    finally { setBusy(false); }
  }

  async function assign(technician) {
    if (!window.confirm(`Assign ${incident.ticketNumber} to ${technician.name}?`)) return;
    await act(async () => {
      const result = await assignRecommendedTechnician(incident.id, technician.technicianId, strategy);
      notify(`Incident assigned to ${result.name}.`, 'success');
      const refreshed = await fetchIncident(incident.id);
      return refreshed;
    });
  }

  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section role="dialog" aria-modal="true" aria-labelledby="incident-detail-title" className="flex max-h-[96vh] w-full max-w-5xl flex-col overflow-hidden rounded-t-2xl border border-slate-700 bg-slate-900 shadow-2xl sm:rounded-2xl">
      <header className="flex items-start justify-between gap-4 border-b border-slate-800 px-5 py-4 sm:px-7"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><span className="font-mono text-xs text-blue-300">{incident.ticketNumber}</span><Badge variant={priorityStyle(incident.priority)}>{incident.priority}</Badge><Badge variant={statusStyle(incident.status)}>{incident.status.replaceAll('_', ' ')}</Badge></div><h2 id="incident-detail-title" className="mt-2 text-xl font-semibold text-white">{incident.title}</h2></div><div className="flex shrink-0 gap-2">{(['ADMIN', 'TECHNICIAN'].includes(role) || isOwner) && <Button size="sm" variant="outline" onClick={() => onEdit(incident)}>Edit</Button>}<button onClick={onClose} aria-label="Close details" className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white"><X className="h-4 w-4" /></button></div></header>
      <div className="overflow-y-auto p-5 sm:p-7">
        {error && <div role="alert" className="mb-4 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-100">{error}</div>}
        <div className="grid gap-5 lg:grid-cols-[1.15fr_0.85fr]">
          <div className="space-y-5">
            <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4"><p className="whitespace-pre-wrap text-sm leading-6 text-slate-300">{incident.description}</p><div className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-800 pt-4 text-xs sm:grid-cols-3"><Info label="Category" value={incident.category} /><Info label="IT service" value={incident.serviceName || incident.affectedService} /><Info label="Reported by" value={incident.reporterName} /><Info label="Assigned to" value={incident.assigneeName || 'Unassigned'} /><Info label="Created" value={date(incident.createdAt)} /><Info label="Impact / urgency" value={`${incident.impact} / ${incident.urgency}`} /><Info label="Business impact" value={`${incident.businessCriticality}/5 · ${incident.affectedUserCount} users`} /></div></div>

            {role === 'EMPLOYEE' && isOwner && ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status) && <section className="space-y-3 rounded-xl border border-slate-800 p-4"><h3 className="text-sm font-semibold text-white">Service feedback</h3>{feedback ? <div className="rounded-lg bg-slate-950 p-3"><p className="text-sm text-amber-200">{'★'.repeat(feedback.rating)}{'☆'.repeat(5-feedback.rating)} <span className="text-slate-400">{feedback.rating}/5</span></p><p className="mt-2 whitespace-pre-wrap text-sm text-slate-300">{feedback.comments || 'No comments provided.'}</p><p className="mt-2 text-[11px] text-slate-500">Submitted {date(feedback.createdAt)}</p></div> : <form onSubmit={submitFeedback} className="space-y-3"><p className="text-xs text-slate-400">Rate the support you received. You can submit feedback once.</p><label className="form-label">Rating<select className="form-select" value={feedbackRating} onChange={(e) => setFeedbackRating(e.target.value)}>{[5,4,3,2,1].map((value) => <option key={value} value={value}>{value} / 5</option>)}</select></label><label className="form-label">Comments (optional)<textarea className="form-input min-h-20" maxLength={2000} value={feedbackComments} onChange={(e) => setFeedbackComments(e.target.value)} /></label>{feedbackError && <p role="alert" className="text-sm text-rose-300">{feedbackError}</p>}<Button type="submit" disabled={busy}>{busy ? 'Submitting…' : 'Submit feedback'}</Button></form>}</section>}

            <section className="rounded-xl border border-slate-800 p-4"><div className="mb-3 flex items-center gap-2 text-sm font-semibold text-white"><MessageSquareText className="h-4 w-4 text-blue-300" /> Activity timeline</div>{loading ? <p className="text-sm text-slate-500">Loading incident activity…</p> : activities.length === 0 ? <p className="text-sm text-slate-500">No activity has been added yet.</p> : <div className="space-y-3">{activities.map((item) => <div key={item.id} className="border-l border-slate-700 pl-3"><div className="flex flex-wrap items-center gap-2"><span className="text-xs font-medium text-slate-200">{item.actorName}</span><Badge variant="outline">{item.type.replaceAll('_', ' ')}</Badge><time className="text-[11px] text-slate-500">{date(item.createdAt)}</time></div><p className="mt-1 whitespace-pre-wrap text-sm text-slate-400">{item.message}</p></div>)}</div>}
              {(role === 'ADMIN' || (role === 'TECHNICIAN' && isAssigned) || (role === 'EMPLOYEE' && isOwner)) && <form onSubmit={submitActivity} className="mt-4 space-y-2 border-t border-slate-800 pt-4"><div className="flex flex-col gap-2 sm:flex-row"><select className="form-select sm:max-w-56" value={activityType} onChange={(e) => setActivityType(e.target.value)}>{role === 'EMPLOYEE' ? <option value="EMPLOYEE_RESPONSE">Employee response</option> : <><option value="INVESTIGATION_NOTE">Investigation note</option><option value="INFORMATION_REQUEST">Request information</option>{(isOwner || role === 'ADMIN') && <option value="EMPLOYEE_RESPONSE">Employee response</option>}</>}</select><input className="form-input" placeholder="Add an update…" value={message} onChange={(e) => setMessage(e.target.value)} maxLength={4000} required /></div><div className="flex justify-end"><Button size="sm" type="submit" disabled={busy || !message.trim()}>Add update</Button></div></form>}
            </section>
          </div>

          <div className="space-y-5">
            <section className="rounded-xl border border-slate-800 p-4"><div className="mb-3 flex items-center gap-2 text-sm font-semibold text-white"><FileClock className="h-4 w-4 text-blue-300" /> SLA targets</div>{sla ? <><div className="mb-3 flex flex-wrap items-center gap-2"><Badge variant={sla.status === 'BREACHED' ? 'destructive' : sla.status === 'AT_RISK' ? 'warning' : 'success'}>{sla.status?.replaceAll('_', ' ')}</Badge><span className="text-xs text-slate-500">{sla.policy}</span>{slaHistory.some((row) => row.action?.startsWith('SLA_ESCALATED_')) && <Badge variant="destructive">Escalated</Badge>}</div><div className="grid grid-cols-2 gap-3"><Deadline label="Response deadline" value={sla.responseDeadline} violated={sla.responseViolated} /><Deadline label="Resolution deadline" value={sla.resolutionDeadline} violated={sla.resolutionViolated} /></div>{sla.acknowledgedAt && <p className="mt-3 text-xs text-slate-500">Acknowledged {date(sla.acknowledgedAt)}</p>}</> : <p className="text-sm text-slate-500">{loading ? 'Loading SLA status…' : 'SLA details are unavailable for this incident.'}</p>}{slaHistory.length > 0 && <details className="mt-4 border-t border-slate-800 pt-3"><summary className="cursor-pointer text-xs text-slate-400">SLA history ({slaHistory.length})</summary><div className="mt-3 space-y-2">{slaHistory.map((row) => <div key={row.id} className="text-xs"><p className={row.action?.startsWith('SLA_ESCALATED_') ? 'font-medium text-rose-200' : 'text-slate-300'}>{row.action?.replaceAll('_', ' ')}</p><p className="text-slate-500">{row.description} · {date(row.timestamp)}</p></div>)}</div></details>}</section>

            {transitionButtons.length > 0 && <section className="space-y-3 rounded-xl border border-slate-800 p-4"><div className="flex items-center gap-2 text-sm font-semibold text-white"><ArrowRightLeft className="h-4 w-4 text-blue-300" /> Next step</div>{transitionButtons.map(([next, label]) => <React.Fragment key={next}>{next === 'RESOLVED' && <textarea className="form-input min-h-20" placeholder="Resolution details (required)" value={resolution} onChange={(e) => setResolution(e.target.value)} /> }<Button disabled={busy} className="w-full" onClick={() => performTransition(next)}>{busy ? 'Saving…' : label}</Button></React.Fragment>)}</section>}

            {role === 'ADMIN' && ['NEW', 'CLASSIFIED', 'OPEN'].includes(incident.status) && !incident.assigneeId && <section className="space-y-3 rounded-xl border border-slate-800 p-4"><div className="flex items-center gap-2 text-sm font-semibold text-white"><UserRound className="h-4 w-4 text-blue-300" /> Technician recommendations</div><div className="flex gap-2"><select aria-label="Assignment strategy" className="form-select" value={strategy} onChange={(e) => { setStrategy(e.target.value); setRecommendation(null); }}><option value="">Configured strategy</option><option value="WORKLOAD_BALANCED">Workload balanced</option><option value="SKILL_BASED">Skill based</option><option value="ROUND_ROBIN">Round robin</option></select><Button variant="outline" onClick={getRecommendation} disabled={busy}>Rank technicians</Button></div>{recommendation?.length === 0 && <p className="text-xs text-slate-500">No available technician meets the selected strategy.</p>}{recommendation?.map((technician) => <div key={technician.technicianId} className="rounded-lg bg-slate-950 p-3 text-xs"><div className="flex items-center justify-between gap-2"><p className="font-medium text-white">#{technician.rank} · {technician.name}</p><span className="text-slate-400">{technician.strategy}</span></div><p className="mt-1 text-slate-400">{technician.activeWorkload}/{technician.maxActiveWorkload} active incidents · {technician.experienceYears} years experience</p><ul className="mt-2 list-inside list-disc space-y-1 text-slate-500">{technician.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul><Button size="sm" className="mt-3 w-full" onClick={() => assign(technician)} disabled={busy}>Assign to {technician.name}</Button></div>)}</section>}

            {role === 'EMPLOYEE' && isOwner && <SimilaritySuggestions title="Possible duplicate incidents" matches={possibleDuplicates} emptyMessage="No similar open incidents were found." kind="similarity" />}

            {['ADMIN', 'TECHNICIAN'].includes(role) && <SimilaritySuggestions title="Related incidents" matches={relatedIncidents} emptyMessage="No related active incidents found in the configured time window." kind="related" />}

            {assignments.length > 0 && <section className="rounded-xl border border-slate-800 p-4"><h3 className="mb-3 text-sm font-semibold text-white">Assignment history</h3><div className="space-y-2">{assignments.map((row) => <div key={row.id} className="flex justify-between gap-3 text-xs"><span className="text-slate-300">{row.technicianName} · {row.strategy}</span><time className="shrink-0 text-slate-500">{date(row.assignedAt)}</time></div>)}</div></section>}

            {incident.priorityExplanation && <details className="rounded-xl border border-slate-800 p-4"><summary className="cursor-pointer text-xs font-medium text-slate-300">Priority calculation</summary><p className="mt-3 text-xs leading-5 text-slate-500">{incident.priorityExplanation}</p></details>}
          </div>
        </div>
      </div>
    </section>
  </div>;
}

function Info({ label, value }) { return <div><p className="text-[10px] uppercase tracking-wider text-slate-500">{label}</p><p className="mt-1 text-xs text-slate-300">{value || '—'}</p></div>; }
function Deadline({ label, value, violated }) { return <div className={`rounded-lg border p-3 ${violated ? 'border-rose-500/30 bg-rose-500/5' : 'border-slate-800 bg-slate-950/50'}`}><div className="flex items-center gap-1.5 text-[10px] uppercase tracking-wide text-slate-500">{violated ? <AlertTriangle className="h-3 w-3 text-rose-300" /> : <Clock3 className="h-3 w-3" />}{label}</div><p className={`mt-2 text-xs ${violated ? 'text-rose-200' : 'text-slate-200'}`}>{date(value)}</p><p className={`mt-1 text-[10px] ${violated ? 'text-rose-300' : 'text-slate-500'}`}>{violated ? 'Target breached' : 'Target deadline'}</p></div>; }
function SimilaritySuggestions({ title, matches, emptyMessage, kind }) {
  return <section className="rounded-xl border border-slate-800 p-4"><h3 className="mb-3 text-sm font-semibold text-white">{title}</h3>{matches.length ? <div className="space-y-2">{matches.map((match) => <div key={match.incidentId} className="rounded-lg bg-slate-950 p-3 text-xs"><div className="flex flex-wrap items-center justify-between gap-2"><span className="font-mono text-blue-200">{match.ticketNumber} · #{match.incidentId}</span><span className="text-slate-400">{match.status} · {Math.round(match.similarity * 100)}% {kind === 'related' ? 'related' : 'similar'}</span></div><p className="mt-1 text-sm text-slate-200">{match.title}</p>{kind === 'related' && <p className="mt-1 text-slate-500">{match.reasons.join(' · ')}</p>}</div>)}</div> : <p className="text-xs text-slate-500">{emptyMessage}</p>}</section>;
}
