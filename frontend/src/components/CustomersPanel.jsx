import React, { useState, useEffect } from 'react';
import { customerApi } from '../services/api';

function CustomersPanel({ tenantId }) {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchEmail, setSearchEmail] = useState('');
  const [searchResult, setSearchResult] = useState(null);
  
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    address: '',
  });

  useEffect(() => {
    loadCustomers();
  }, [tenantId]);

  const loadCustomers = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await customerApi.getCustomers(tenantId);
      setCustomers(response.data);
    } catch (err) {
      setError('Failed to load customers: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await customerApi.createCustomer(tenantId, formData);
      setFormData({ name: '', email: '', phone: '', address: '' });
      loadCustomers();
    } catch (err) {
      setError('Failed to create customer: ' + err.message);
    }
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    setError(null);
    setSearchResult(null);
    try {
      const response = await customerApi.searchByEmail(tenantId, searchEmail);
      setSearchResult(response.data);
    } catch (err) {
      if (err.response && err.response.status === 404) {
        setSearchResult({ notFound: true });
      } else {
        setError('Search failed: ' + err.message);
      }
    }
  };

  return (
    <div className="panel">
      <h2>Customers for {tenantId}</h2>
      
      {error && <div className="error">{error}</div>}
      
      <div className="form">
        <h3>Add New Customer</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Name:</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Email:</label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Phone:</label>
            <input
              type="text"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Address:</label>
            <input
              type="text"
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              required
            />
          </div>
          <button type="submit" className="btn">Create Customer</button>
        </form>
      </div>

      <div className="form">
        <h3>Search by Email (Deterministic Encryption)</h3>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '10px', alignItems: 'flex-end' }}>
          <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
            <label>Email:</label>
            <input
              type="email"
              value={searchEmail}
              onChange={(e) => setSearchEmail(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn">Search</button>
        </form>
        {searchResult && (
          <div style={{ marginTop: '15px' }}>
            {searchResult.notFound ? (
              <div className="info">No customer found with that email</div>
            ) : (
              <div className="success">
                <strong>Found:</strong> {searchResult.name} - {searchResult.email}
              </div>
            )}
          </div>
        )}
      </div>

      <h3>All Customers</h3>
      {loading ? (
        <div>Loading...</div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Customer ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Address</th>
            </tr>
          </thead>
          <tbody>
            {customers.map((customer) => (
              <tr key={customer.customerId}>
                <td>{customer.customerId}</td>
                <td>{customer.name}</td>
                <td>{customer.email}</td>
                <td>{customer.phone}</td>
                <td>{customer.address}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default CustomersPanel;

