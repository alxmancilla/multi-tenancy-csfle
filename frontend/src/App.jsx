import React, { useState, useEffect } from 'react';
import { tenantApi } from './services/api';
import TenantSelector from './components/TenantSelector';
import CustomersPanel from './components/CustomersPanel';
import OrdersPanel from './components/OrdersPanel';
import DbaViewPanel from './components/DbaViewPanel';
import CrossTenantPanel from './components/CrossTenantPanel';
import ApplicationLevelAccessPanel from './components/ApplicationLevelAccessPanel';
import './App.css';

function App() {
  const [tenants, setTenants] = useState([]);
  const [selectedTenant, setSelectedTenant] = useState(null);
  const [activeTab, setActiveTab] = useState('customers');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTenants();
  }, []);

  const loadTenants = async () => {
    try {
      const response = await tenantApi.getAllTenants();
      setTenants(response.data);
      if (response.data.length > 0) {
        setSelectedTenant(response.data[0].id);
      }
      setLoading(false);
    } catch (error) {
      console.error('Error loading tenants:', error);
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>🔐 MongoDB CSFLE Multi-Tenancy Demo</h1>
        <p className="subtitle">Client-Side Field Level Encryption with Per-Tenant Data Encryption Keys</p>
      </header>

      <TenantSelector
        tenants={tenants}
        selectedTenant={selectedTenant}
        onSelectTenant={setSelectedTenant}
      />

      <div className="tabs">
        <button
          className={activeTab === 'customers' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('customers')}
        >
          Customers
        </button>
        <button
          className={activeTab === 'orders' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('orders')}
        >
          Orders
        </button>
        <button
          className={activeTab === 'dba-view' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('dba-view')}
        >
          DBA View (Raw Data)
        </button>
        <button
          className={activeTab === 'cross-tenant' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('cross-tenant')}
        >
          Cross-Tenant Attack
        </button>
        <button
          className={activeTab === 'app-level-access' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('app-level-access')}
        >
          Application-Level Access Control
        </button>
      </div>

      <div className="content">
        {activeTab === 'customers' && selectedTenant && (
          <CustomersPanel tenantId={selectedTenant} />
        )}
        {activeTab === 'orders' && selectedTenant && (
          <OrdersPanel tenantId={selectedTenant} />
        )}
        {activeTab === 'dba-view' && (
          <DbaViewPanel />
        )}
        {activeTab === 'cross-tenant' && (
          <CrossTenantPanel tenants={tenants} />
        )}
        {activeTab === 'app-level-access' && (
          <ApplicationLevelAccessPanel />
        )}
      </div>
    </div>
  );
}

export default App;

