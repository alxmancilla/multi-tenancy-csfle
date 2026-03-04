import React, { useState, useEffect } from 'react';
import { orderApi, customerApi } from '../services/api';

function OrdersPanel({ tenantId }) {
  const [orders, setOrders] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const [formData, setFormData] = useState({
    customerId: '',
    product: '',
    amount: '',
    status: 'pending',
  });

  useEffect(() => {
    loadOrders();
    loadCustomers();
  }, [tenantId]);

  const loadOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await orderApi.getOrders(tenantId);
      setOrders(response.data);
    } catch (err) {
      setError('Failed to load orders: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const response = await customerApi.getCustomers(tenantId);
      setCustomers(response.data);
      if (response.data.length > 0) {
        setFormData(prev => ({ ...prev, customerId: response.data[0].customerId }));
      }
    } catch (err) {
      console.error('Failed to load customers:', err);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await orderApi.createOrder(tenantId, {
        ...formData,
        amount: parseFloat(formData.amount),
      });
      setFormData({ customerId: customers[0]?.customerId || '', product: '', amount: '', status: 'pending' });
      loadOrders();
    } catch (err) {
      setError('Failed to create order: ' + err.message);
    }
  };

  return (
    <div className="panel">
      <h2>Orders for {tenantId}</h2>
      
      {error && <div className="error">{error}</div>}
      
      <div className="form">
        <h3>Create New Order</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Customer:</label>
            <select
              value={formData.customerId}
              onChange={(e) => setFormData({ ...formData, customerId: e.target.value })}
              required
            >
              {customers.map((customer) => (
                <option key={customer.customerId} value={customer.customerId}>
                  {customer.name} ({customer.email})
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Product:</label>
            <input
              type="text"
              value={formData.product}
              onChange={(e) => setFormData({ ...formData, product: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Amount ($):</label>
            <input
              type="number"
              step="0.01"
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Status:</label>
            <select
              value={formData.status}
              onChange={(e) => setFormData({ ...formData, status: e.target.value })}
            >
              <option value="pending">Pending</option>
              <option value="active">Active</option>
              <option value="completed">Completed</option>
              <option value="in_progress">In Progress</option>
            </select>
          </div>
          <button type="submit" className="btn" disabled={customers.length === 0}>
            Create Order
          </button>
        </form>
      </div>

      <h3>All Orders</h3>
      {loading ? (
        <div>Loading...</div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Customer ID</th>
              <th>Product</th>
              <th>Amount</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.orderId}>
                <td>{order.orderId}</td>
                <td>{order.customerId}</td>
                <td>{order.product}</td>
                <td>${order.amount.toFixed(2)}</td>
                <td>{order.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default OrdersPanel;

