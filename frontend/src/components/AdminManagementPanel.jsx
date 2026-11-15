import React, { useEffect, useState } from 'react';
import { Card } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { createIncidentCategory, createItService, fetchAdminFeedback, fetchAdminUser, fetchAdminUsers, fetchIncidentCategories, fetchItServices, setIncidentCategoryActive, setItServiceActive, setUserActive, setUserRole, updateIncidentCategory, updateItService } from '../services/api';

const date = (value) => value ? new Date(value).toLocaleString() : '—';

export function AdminManagementPanel({ type, notify, onTechnicianDirectoryChanged }) {
  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [categoryName, setCategoryName] = useState('');
  const [editingCategory, setEditingCategory] = useState(null);
  const [serviceForm, setServiceForm] = useState({ name: '', description: '' });
  const [editingService, setEditingService] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let active = true; setLoading(true); setError('');
    const request = type === 'users' ? fetchAdminUsers({ query, page, size: 25 })
      : type === 'categories' ? fetchIncidentCategories(false)
        : type === 'services' ? fetchItServices()
          : fetchAdminFeedback({ page, size: 25 });
    request.then((result) => {
      if (!active) return;
      if (type === 'categories' || type === 'services') { setRows(result || []); setTotalPages(1); }
      else { setRows(result?.content || []); setTotalPages(result?.totalPages || 0); }
    }).catch((err) => { if (active) setError(err.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [type, query, page, reload]);

  async function viewUser(id) {
    setError('');
    try { setSelectedUser(await fetchAdminUser(id)); }
    catch (err) { setError(err.message); }
  }

  async function changeUserActive(user) {
    setBusyId(user.id); setError('');
    try { await setUserActive(user.id, !user.active); notify(`Account ${user.active ? 'deactivated' : 'activated'}.`, 'success'); setReload((n) => n + 1); }
    catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  async function changeUserRole(user, role) {
    if (role === user.role) return;
    setBusyId(user.id); setError('');
    try {
      await setUserRole(user.id, role);
      notify('User role updated.', 'success');
      setReload((n) => n + 1);
      if (user.role === 'TECHNICIAN' || role === 'TECHNICIAN') onTechnicianDirectoryChanged?.();
      if (selectedUser?.id === user.id) setSelectedUser(await fetchAdminUser(user.id));
    }
    catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  async function saveCategory(event) {
    event.preventDefault(); setError(''); setBusyId('category');
    try {
      if (editingCategory) await updateIncidentCategory(editingCategory.id, categoryName);
      else await createIncidentCategory(categoryName);
      notify(editingCategory ? 'Category updated.' : 'Category created.', 'success');
      setCategoryName(''); setEditingCategory(null); setReload((n) => n + 1);
    } catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  async function toggleCategory(category) {
    setBusyId(category.id); setError('');
    try { await setIncidentCategoryActive(category.id, !category.active); notify('Category status updated.', 'success'); setReload((n) => n + 1); }
    catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  async function saveService(event) {
    event.preventDefault(); setError(''); setBusyId('service');
    try {
      if (editingService) await updateItService(editingService.id, serviceForm);
      else await createItService(serviceForm);
      notify(editingService ? 'IT service updated.' : 'IT service created.', 'success');
      setServiceForm({ name: '', description: '' }); setEditingService(null); setReload((n) => n + 1);
    } catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  async function toggleService(service) {
    setBusyId(service.id); setError('');
    try { await setItServiceActive(service.id, !service.active); notify('IT service status updated.', 'success'); setReload((n) => n + 1); }
    catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  if (loading) return <div className="h-32 animate-pulse rounded-xl bg-slate-900" aria-label="Loading administration data" />;
  return <section className="space-y-4">
    {error && <div role="alert" className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-100">{error}</div>}
    {type === 'users' && <>
      <div><h2 className="font-semibold text-white">User management</h2><p className="mt-1 text-xs text-slate-500">Search accounts, inspect safe user details, and manage account access and roles.</p></div>
      <input aria-label="Search users" className="form-input max-w-md" placeholder="Search by name or email" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} />
      <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-xs"><thead className="bg-slate-950 text-slate-500"><tr>{['User','Role','Status','Registered','Actions'].map((heading) => <th key={heading} className="px-4 py-3">{heading}</th>)}</tr></thead><tbody className="divide-y divide-slate-800">{rows.map((user) => <tr key={user.id}><td className="px-4 py-3 text-slate-200">{user.fullName}<span className="block text-slate-500">{user.email}</span></td><td className="px-4 py-3"><select aria-label={`Role for ${user.email}`} className="form-select max-w-40" value={user.role} disabled={busyId === user.id} onChange={(event) => changeUserRole(user, event.target.value)}>{['EMPLOYEE','TECHNICIAN','ADMIN'].map((role) => <option key={role}>{role}</option>)}</select></td><td className="px-4 py-3"><Badge variant={user.active ? 'success' : 'outline'}>{user.active ? 'Active' : 'Inactive'}</Badge></td><td className="px-4 py-3 text-slate-400">{date(user.createdAt)}</td><td className="px-4 py-3"><div className="flex gap-2"><button className="text-blue-300 hover:text-blue-200" onClick={() => viewUser(user.id)}>Details</button><button disabled={busyId === user.id} className="text-slate-300 hover:text-white disabled:opacity-50" onClick={() => changeUserActive(user)}>{user.active ? 'Deactivate' : 'Activate'}</button></div></td></tr>)}</tbody></table></div>{rows.length === 0 && <p className="p-8 text-center text-sm text-slate-500">No users found.</p>}</Card>
      <div className="flex items-center justify-between text-xs text-slate-500"><span>Page {totalPages ? page + 1 : 0} of {totalPages}</span><div className="flex gap-4"><button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button><button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Next</button></div></div>
      {selectedUser && <Card className="space-y-3 p-4"><div className="flex items-center justify-between"><h3 className="font-semibold text-white">User details</h3><button className="text-xs text-slate-400" onClick={() => setSelectedUser(null)}>Close</button></div><p className="text-sm text-slate-300">{selectedUser.fullName} · {selectedUser.email}</p><p className="text-xs text-slate-500">ID {selectedUser.id} · {selectedUser.role} · {selectedUser.active ? 'Active' : 'Inactive'} · registered {date(selectedUser.createdAt)}</p></Card>}
    </>}

    {type === 'categories' && <>
      <div><h2 className="font-semibold text-white">Incident categories</h2><p className="mt-1 text-xs text-slate-500">Existing incidents retain their historical category label. Deactivate categories to prevent new use.</p></div>
      <Card className="p-4"><form onSubmit={saveCategory} className="flex flex-col gap-2 sm:flex-row"><input aria-label="Category name" className="form-input" placeholder="Category name" value={categoryName} onChange={(event) => setCategoryName(event.target.value)} maxLength={64} required /><Button type="submit" disabled={busyId === 'category'}>{editingCategory ? 'Save changes' : 'Create category'}</Button>{editingCategory && <Button type="button" variant="outline" onClick={() => { setEditingCategory(null); setCategoryName(''); }}>Cancel</Button>}</form></Card>
      <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[560px] text-left text-xs"><thead className="bg-slate-950 text-slate-500"><tr><th className="px-4 py-3">Category</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Actions</th></tr></thead><tbody className="divide-y divide-slate-800">{rows.map((category) => <tr key={category.id}><td className="px-4 py-3 text-slate-200">{category.name}</td><td className="px-4 py-3"><Badge variant={category.active ? 'success' : 'outline'}>{category.active ? 'Active' : 'Inactive'}</Badge></td><td className="px-4 py-3"><div className="flex gap-3"><button className="text-blue-300" onClick={() => { setEditingCategory(category); setCategoryName(category.name); }}>Rename</button><button disabled={busyId === category.id} className="text-slate-300" onClick={() => toggleCategory(category)}>{category.active ? 'Deactivate' : 'Activate'}</button></div></td></tr>)}</tbody></table></div>{rows.length === 0 && <p className="p-8 text-center text-sm text-slate-500">No categories configured.</p>}</Card>
    </>}

    {type === 'services' && <>
      <div><h2 className="font-semibold text-white">IT service catalog</h2><p className="mt-1 text-xs text-slate-500">Manage selectable services. Deactivating a service prevents it from being selected for new or changed incidents while preserving historical links.</p></div>
      <Card className="p-4"><form onSubmit={saveService} className="space-y-3"><label className="form-label">Service name<input aria-label="Service name" className="form-input" value={serviceForm.name} onChange={(event) => setServiceForm({ ...serviceForm, name: event.target.value })} maxLength={120} required /></label><label className="form-label">Description<textarea aria-label="Service description" className="form-input min-h-20" value={serviceForm.description} onChange={(event) => setServiceForm({ ...serviceForm, description: event.target.value })} maxLength={1000} /></label><div className="flex flex-wrap gap-2"><Button type="submit" disabled={busyId === 'service'}>{busyId === 'service' ? 'Saving…' : editingService ? 'Save service' : 'Add service'}</Button>{editingService && <Button type="button" variant="outline" onClick={() => { setEditingService(null); setServiceForm({ name: '', description: '' }); }}>Cancel</Button>}</div></form></Card>
      <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-xs"><thead className="bg-slate-950 text-slate-500"><tr>{['Service','Description','Status','Actions'].map((heading) => <th key={heading} className="px-4 py-3">{heading}</th>)}</tr></thead><tbody className="divide-y divide-slate-800">{rows.map((service) => <tr key={service.id}><td className="px-4 py-3 font-medium text-slate-200">{service.name}</td><td className="max-w-md whitespace-pre-wrap px-4 py-3 text-slate-400">{service.description || '—'}</td><td className="px-4 py-3"><Badge variant={service.active ? 'success' : 'outline'}>{service.active ? 'Active' : 'Inactive'}</Badge></td><td className="px-4 py-3"><div className="flex gap-3"><button className="text-blue-300" onClick={() => { setEditingService(service); setServiceForm({ name: service.name, description: service.description || '' }); }}>Edit</button><button disabled={busyId === service.id} className="text-slate-300 disabled:opacity-50" onClick={() => toggleService(service)}>{service.active ? 'Deactivate' : 'Activate'}</button></div></td></tr>)}</tbody></table></div>{rows.length === 0 && <p className="p-8 text-center text-sm text-slate-500">No IT services configured.</p>}</Card>
    </>}

    {type === 'feedback' && <>
      <div><h2 className="font-semibold text-white">Employee incident feedback</h2><p className="mt-1 text-xs text-slate-500">Feedback is visible to administrators and is associated with its incident and reporting employee.</p></div>
      <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-xs"><thead className="bg-slate-950 text-slate-500"><tr>{['Incident','Employee','Rating','Comments','Submitted'].map((heading) => <th key={heading} className="px-4 py-3">{heading}</th>)}</tr></thead><tbody className="divide-y divide-slate-800">{rows.map((item) => <tr key={item.id}><td className="px-4 py-3 text-blue-200">{item.ticketNumber}<span className="block text-slate-500">{item.incidentTitle}</span></td><td className="px-4 py-3 text-slate-300">{item.employeeName}<span className="block text-slate-500">{item.employeeEmail}</span></td><td className="px-4 py-3 text-amber-200">{item.rating}/5</td><td className="max-w-sm whitespace-pre-wrap px-4 py-3 text-slate-400">{item.comments || '—'}</td><td className="px-4 py-3 text-slate-500">{date(item.createdAt)}</td></tr>)}</tbody></table></div>{rows.length === 0 && <p className="p-8 text-center text-sm text-slate-500">No employee feedback has been submitted.</p>}</Card>
      <div className="flex items-center justify-between text-xs text-slate-500"><span>Page {totalPages ? page + 1 : 0} of {totalPages}</span><div className="flex gap-4"><button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button><button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Next</button></div></div>
    </>}
  </section>;
}
