import React, { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { Button } from './ui/button';
import { checkDuplicateIncidents, createIncident, fetchActiveItServices, fetchIncidentCategories, updateIncident } from '../services/api';

const blank = { title: '', description: '', category: 'APPLICATION', impact: 'MODERATE', urgency: 'NORMAL', businessCriticality: 3, affectedUserCount: 1, affectedService: 'General IT Service', serviceCriticality: 3 };
const impacts = ['VERY_LOW', 'LOW', 'MODERATE', 'HIGH', 'EXTENSIVE'];
const urgencies = ['LOW', 'NORMAL', 'HIGH', 'IMMEDIATE', 'CRITICAL'];

export function IncidentForm({ incident, onClose, onSaved, notify }) {
  const [form, setForm] = useState(blank);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [possibleDuplicates, setPossibleDuplicates] = useState([]);
  const [duplicateCheckFailed, setDuplicateCheckFailed] = useState(false);
  const [duplicateChecked, setDuplicateChecked] = useState(false);
  const [categories, setCategories] = useState([]);
  const [services, setServices] = useState([]);
  useEffect(() => {
    setForm(incident ? { ...blank, ...incident, serviceId: incident.serviceId ? String(incident.serviceId) : '' } : blank);
    fetchIncidentCategories(true).then(setCategories).catch((err) => setError(err.message));
    fetchActiveItServices().then((values) => {
      const activeServices = values || [];
      setServices(activeServices);
      if (!incident) setForm((current) => {
        if (current.serviceId) return current;
        const matching = activeServices.find((service) => service.name.toLowerCase() === (current.affectedService || '').toLowerCase());
        const selected = matching || activeServices[0];
        return selected ? { ...current, serviceId: String(selected.id), affectedService: selected.name } : current;
      });
    }).catch((err) => setError(err.message));
  }, [incident]);
  const field = (name, value) => {
    setForm((current) => ({ ...current, [name]: value }));
    setDuplicateChecked(false); setPossibleDuplicates([]); setDuplicateCheckFailed(false);
  };

  async function submit(event, continueAnyway = false) {
    event?.preventDefault(); setBusy(true); setError('');
    const selectedService = services.find((service) => String(service.id) === String(form.serviceId));
    const body = { ...form, serviceId: selectedService ? selectedService.id : null,
      affectedService: selectedService?.name || form.affectedService,
      businessCriticality: Number(form.businessCriticality), affectedUserCount: Number(form.affectedUserCount), serviceCriticality: Number(form.serviceCriticality) };
    try {
      if (!incident && !continueAnyway && !duplicateChecked) {
        const matches = await checkDuplicateIncidents(body);
        setDuplicateChecked(true); setPossibleDuplicates(matches || []);
        if (matches?.length) { setBusy(false); return; }
      }
      if (!incident && duplicateCheckFailed && !continueAnyway) return;
      const saved = incident ? await updateIncident(incident.id, body) : await createIncident(body);
      notify(incident ? 'Incident details updated.' : 'Incident reported successfully.', 'success');
      onSaved(saved); onClose();
    } catch (requestError) {
      if (!incident && !duplicateChecked && !continueAnyway) { setDuplicateCheckFailed(true); setError(`Duplicate check could not complete: ${requestError.message}. You can retry or explicitly continue without the check.`); }
      else setError([requestError.message, ...requestError.errors].filter(Boolean).join(' '));
    } finally { setBusy(false); }
  }

  return <div className="fixed inset-0 z-[60] flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <div role="dialog" aria-modal="true" aria-labelledby="incident-form-title" className="max-h-[94vh] w-full max-w-3xl overflow-y-auto rounded-t-2xl border border-slate-700 bg-slate-900 shadow-2xl sm:rounded-2xl">
      <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4 sm:px-6"><div><h2 id="incident-form-title" className="text-lg font-semibold text-white">{incident ? 'Edit incident' : 'Report an incident'}</h2><p className="mt-1 text-xs text-slate-400">Priority is calculated by the service from impact and urgency details.</p></div><button onClick={onClose} aria-label="Close" className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white"><X className="h-4 w-4" /></button></div>
      <form onSubmit={submit} className="space-y-5 p-5 sm:p-6">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="form-label sm:col-span-2">Title<input className="form-input" value={form.title} onChange={(e) => field('title', e.target.value)} required maxLength={200} /></label>
          <label className="form-label sm:col-span-2">Description<textarea className="form-input min-h-24" value={form.description} onChange={(e) => field('description', e.target.value)} required rows={3} /></label>
          <label className="form-label">Category<select className="form-select" value={form.category} onChange={(e) => field('category', e.target.value)} required><option value="" disabled>Select a category</option>{form.category && !categories.some((category) => category.name === form.category) && <option value={form.category}>{form.category} (historical)</option>}{categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}</select></label>
          <label className="form-label">IT service<select className="form-select" value={incident && !services.some((service) => String(service.id) === String(incident.serviceId)) ? 'historical' : (form.serviceId || (incident ? 'historical' : ''))} onChange={(event) => {
            const service = services.find((entry) => String(entry.id) === event.target.value);
            setForm((current) => ({ ...current, serviceId: service ? String(service.id) : '', affectedService: service?.name || current.affectedService }));
            setDuplicateChecked(false); setPossibleDuplicates([]); setDuplicateCheckFailed(false);
          }} required={!incident}>
            {!incident && <option value="" disabled>Select an active IT service</option>}
            {incident && !incident.serviceId && <option value="historical">Historical service: {incident.affectedService}</option>}
            {incident && incident.serviceId && !services.some((service) => String(service.id) === String(incident.serviceId)) && <option value="historical">Inactive service: {incident.serviceName || incident.affectedService}</option>}
            {services.map((service) => <option key={service.id} value={String(service.id)}>{service.name}</option>)}
          </select></label>
          <label className="form-label">Impact<select className="form-select" value={form.impact} onChange={(e) => field('impact', e.target.value)}>{impacts.map((value) => <option key={value}>{value}</option>)}</select></label>
          <label className="form-label">Urgency<select className="form-select" value={form.urgency} onChange={(e) => field('urgency', e.target.value)}>{urgencies.map((value) => <option key={value}>{value}</option>)}</select></label>
          <label className="form-label">Business criticality (1–5)<input type="number" className="form-input" min="1" max="5" value={form.businessCriticality} onChange={(e) => field('businessCriticality', e.target.value)} required /></label>
          <label className="form-label">Affected users<input type="number" className="form-input" min="0" max="1000000" value={form.affectedUserCount} onChange={(e) => field('affectedUserCount', e.target.value)} required /></label>
          <label className="form-label">Service criticality (1–5)<input type="number" className="form-input" min="1" max="5" value={form.serviceCriticality} onChange={(e) => field('serviceCriticality', e.target.value)} required /></label>
        </div>
        {!incident && possibleDuplicates.length > 0 && <section className="space-y-3 rounded-xl border border-amber-500/30 bg-amber-500/5 p-4" aria-live="polite"><div><h3 className="font-medium text-amber-100">Possible duplicate incidents</h3><p className="mt-1 text-xs text-amber-200/70">Review these matches. This is a suggestion only; no incident will be merged or removed.</p></div><div className="space-y-2">{possibleDuplicates.map((match) => <div key={match.incidentId} className="rounded-lg bg-slate-950/70 p-3 text-sm"><div className="flex flex-wrap items-center justify-between gap-2"><span className="font-mono text-blue-200">{match.ticketNumber} · #{match.incidentId}</span><span className="text-slate-300">{match.status}</span><span className="text-amber-200">{Math.round(match.similarity * 100)}% similarity</span></div><p className="mt-1 text-slate-200">{match.title}</p></div>)}</div><Button type="button" onClick={(event) => submit(event, true)} disabled={busy}>{busy ? 'Creating…' : 'Continue and create a separate incident'}</Button></section>}
        {!incident && duplicateCheckFailed && <Button type="button" variant="outline" onClick={(event) => submit(event, true)} disabled={busy}>{busy ? 'Creating…' : 'Continue without duplicate check'}</Button>}
        {error && <p role="alert" className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{error}</p>}
        <div className="flex justify-end gap-2 border-t border-slate-800 pt-4"><Button type="button" variant="outline" onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? 'Saving…' : incident ? 'Save changes' : 'Submit incident'}</Button></div>
      </form>
    </div>
  </div>;
}
