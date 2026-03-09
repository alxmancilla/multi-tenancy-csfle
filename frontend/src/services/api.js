import axios from 'axios';

// Use environment variable for API URL, fallback to localhost for development
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
  ? `${import.meta.env.VITE_API_BASE_URL}/api/v1`
  : 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const tenantApi = {
  getAllTenants: () => api.get('/tenants'),
};

export const customerApi = {
  getCustomers: (tenantId) => api.get(`/tenants/${tenantId}/customers`),
  createCustomer: (tenantId, data) => api.post(`/tenants/${tenantId}/customers`, data),
  searchByEmail: (tenantId, email) => api.get(`/tenants/${tenantId}/customers/search?email=${encodeURIComponent(email)}`),
};

export const orderApi = {
  getOrders: (tenantId) => api.get(`/tenants/${tenantId}/orders`),
  createOrder: (tenantId, data) => api.post(`/tenants/${tenantId}/orders`, data),
  getOrdersByCustomer: (tenantId, customerId) => api.get(`/tenants/${tenantId}/orders/customer/${customerId}`),
};

export const demoApi = {
  getRawDocuments: () => api.get('/demo/raw-documents'),
  attemptCrossTenantDecryption: (attackerTenant, victimTenant) =>
    api.post('/demo/cross-tenant-attempt', { attackerTenant, victimTenant }),
  demonstrateApplicationLevelAccessControl: (requestingTenant) =>
    api.post('/demo/application-level-access-control', { requestingTenant }),
};

export default api;

