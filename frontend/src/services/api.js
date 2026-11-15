const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '');
const SESSION_KEY = 'iitsm.session';

let unauthorizedHandler = () => {};

export class ApiError extends Error {
  constructor(message, status, errors = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errors = errors;
  }
}

export function readSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null');
    if (!session?.token || !session?.user || Date.now() >= session.expiresAt) {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(auth) {
  const session = {
    token: auth.token,
    user: { id: auth.userId, email: auth.email, fullName: auth.fullName, role: auth.role },
    expiresAt: Date.now() + Number(auth.expiresInMs || 0),
  };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler || (() => {});
}

export async function apiRequest(path, options = {}) {
  const { noAuth = false, ...requestOptions } = options;
  const headers = new Headers(requestOptions.headers || {});
  headers.set('Accept', 'application/json');
  if (requestOptions.body && !(requestOptions.body instanceof FormData)) headers.set('Content-Type', 'application/json');

  const session = !noAuth ? readSession() : null;
  if (!noAuth && session?.token) headers.set('Authorization', `Bearer ${session.token}`);

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...requestOptions, headers });
  } catch {
    throw new ApiError('Could not reach the I-ITSM backend. Check that the backend is running.', 0);
  }

  const text = await response.text();
  let payload = null;
  if (text) {
    try { payload = JSON.parse(text); } catch { payload = { message: text }; }
  }
  if (response.status === 401 && !noAuth) {
    clearSession();
    unauthorizedHandler();
  }
  if (!response.ok || payload?.success === false) {
    const details = Array.isArray(payload?.errors) ? payload.errors : [];
    throw new ApiError(payload?.message || `Request failed (${response.status})`, response.status, details);
  }
  return payload?.data ?? payload;
}

export async function checkBackendHealth() {
  try { return { ok: true, data: await apiRequest('/health', { noAuth: true }) }; }
  catch (error) { return { ok: false, error: error.message }; }
}

export const loginUser = (email, password) => apiRequest('/auth/login', {
  method: 'POST', noAuth: true, body: JSON.stringify({ email, password }),
});

export const registerUser = ({ email, password, fullName }) => apiRequest('/auth/register', {
  method: 'POST', noAuth: true, body: JSON.stringify({ email, password, fullName }),
});

export const fetchIncidents = () => apiRequest('/incidents');
export const searchIncidents = (params = {}) => {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  return apiRequest(`/incidents/search${query.size ? `?${query}` : ''}`);
};
export const fetchIncident = (id) => apiRequest(`/incidents/${id}`);
export const createIncident = (body) => apiRequest('/incidents', { method: 'POST', body: JSON.stringify(body) });
export const checkDuplicateIncidents = (body) => apiRequest('/intelligence/duplicates', { method: 'POST', body: JSON.stringify(body) });
export const fetchRelatedIncidents = (id) => apiRequest(`/intelligence/incidents/${id}/related`);
export const updateIncident = (id, body) => apiRequest(`/incidents/${id}`, { method: 'PUT', body: JSON.stringify(body) });
export const transitionIncident = (id, status, details) => apiRequest(`/incidents/${id}/status`, {
  method: 'PATCH', body: JSON.stringify({ status, details }),
});
export const fetchActivities = (id) => apiRequest(`/incidents/${id}/activities`);
export const addActivity = (id, activityType, message) => apiRequest(`/incidents/${id}/activities`, {
  method: 'POST', body: JSON.stringify({ activityType, message }),
});
export const fetchAssignmentHistory = (id) => apiRequest(`/assignments/${id}/history`);
export const fetchRecommendation = (id, strategy) => apiRequest(`/assignments/recommendations/${id}${strategy ? `?strategy=${encodeURIComponent(strategy)}` : ''}`);
export const fetchRankedRecommendations = (id, strategy) => apiRequest(`/assignments/recommendations/${id}/ranked${strategy ? `?strategy=${encodeURIComponent(strategy)}` : ''}`);
export const assignRecommendedTechnician = (id, technicianId, strategy) => apiRequest(`/assignments/${id}/assign${strategy ? `?strategy=${encodeURIComponent(strategy)}` : ''}`, { method: 'POST', body: JSON.stringify({ technicianId }) });
export const autoRouteIncident = (id, strategy) => apiRequest(`/assignments/auto-route/${id}${strategy ? `?strategy=${encodeURIComponent(strategy)}` : ''}`, { method: 'POST' });
export const setAssignmentStrategy = (strategy) => apiRequest('/assignments/strategy', { method: 'PUT', body: JSON.stringify({ strategy }) });
export const configureTechnician = (userId, body) => apiRequest(`/assignments/technicians/${userId}/profile`, { method: 'PUT', body: JSON.stringify(body) });
export const fetchSlaStatus = (id) => apiRequest(`/sla/incidents/${id}/status`);
export const fetchSlaHistory = (id) => apiRequest(`/sla/incidents/${id}/history`);
export const fetchSlaPolicies = () => apiRequest('/sla/policies');
export const createSlaPolicy = (body) => apiRequest('/sla/policies', { method: 'POST', body: JSON.stringify(body) });
export const updateSlaPolicy = (id, body) => apiRequest(`/sla/policies/${id}`, { method: 'PUT', body: JSON.stringify(body) });
export const deactivateSlaPolicy = (id) => apiRequest(`/sla/policies/${id}`, { method: 'DELETE' });
export const fetchNotifications = () => apiRequest('/notifications');
export const markNotificationRead = (id) => apiRequest(`/notifications/${id}/read`, { method: 'PATCH' });
export const fetchAudit = (params = {}) => {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  return apiRequest(`/audit${query.size ? `?${query}` : ''}`);
};

export const fetchReportAnalytics = (params = {}) => {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  return apiRequest(`/reports/analytics${query.size ? `?${query}` : ''}`);
};
export const fetchTechnicianReports = (params = {}) => {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  return apiRequest(`/reports/technicians${query.size ? `?${query}` : ''}`);
};
export const fetchTechnicianDirectory = () => apiRequest('/users/technicians');
export const fetchAdminUsers = (params = {}) => {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  return apiRequest(`/admin/users${query.size ? `?${query}` : ''}`);
};
export const fetchAdminUser = (id) => apiRequest(`/admin/users/${id}`);
export const setUserActive = (id, active) => apiRequest(`/admin/users/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) });
export const setUserRole = (id, role) => apiRequest(`/admin/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) });
export const fetchIncidentCategories = (activeOnly = false) => apiRequest(`/categories?activeOnly=${activeOnly}`);
export const fetchItServices = () => apiRequest('/services');
export const fetchActiveItServices = () => apiRequest('/services/active');
export const createItService = (body) => apiRequest('/services', { method: 'POST', body: JSON.stringify(body) });
export const updateItService = (id, body) => apiRequest(`/services/${id}`, { method: 'PUT', body: JSON.stringify(body) });
export const setItServiceActive = (id, active) => apiRequest(`/services/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) });
export const createIncidentCategory = (name) => apiRequest('/categories', { method: 'POST', body: JSON.stringify({ name }) });
export const updateIncidentCategory = (id, name) => apiRequest(`/categories/${id}`, { method: 'PUT', body: JSON.stringify({ name }) });
export const setIncidentCategoryActive = (id, active) => apiRequest(`/categories/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) });
export const submitIncidentFeedback = (id, rating, comments) => apiRequest(`/incidents/${id}/feedback`, { method: 'POST', body: JSON.stringify({ rating: Number(rating), comments }) });
export const fetchMyIncidentFeedback = (id) => apiRequest(`/incidents/${id}/feedback`);
export const fetchAdminFeedback = (params = {}) => {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  return apiRequest(`/admin/feedback${query.size ? `?${query}` : ''}`);
};

export async function downloadCsv(path, filename) {
  const session = readSession();
  const headers = new Headers({ Accept: 'text/csv' });
  if (session?.token) headers.set('Authorization', `Bearer ${session.token}`);
  let response;
  try { response = await fetch(`${API_BASE}${path}`, { headers }); }
  catch { throw new ApiError('Could not reach the I-ITSM backend.', 0); }
  if (!response.ok) {
    const text = await response.text();
    let payload; try { payload = JSON.parse(text); } catch { payload = null; }
    throw new ApiError(payload?.message || `Export failed (${response.status})`, response.status, payload?.errors || []);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a'); link.href = url; link.download = filename;
  document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url);
}
