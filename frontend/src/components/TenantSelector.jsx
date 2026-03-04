import React from 'react';

function TenantSelector({ tenants, selectedTenant, onSelectTenant }) {
  return (
    <div style={styles.container}>
      <h3 style={styles.title}>Select Tenant:</h3>
      <div style={styles.buttonGroup}>
        {tenants.map((tenant) => (
          <button
            key={tenant.id}
            style={{
              ...styles.button,
              ...(selectedTenant === tenant.id ? styles.buttonActive : {}),
            }}
            onClick={() => onSelectTenant(tenant.id)}
          >
            <div style={styles.tenantId}>{tenant.id}</div>
            <div style={styles.tenantName}>{tenant.name}</div>
          </button>
        ))}
      </div>
    </div>
  );
}

const styles = {
  container: {
    background: 'white',
    padding: '20px',
    borderRadius: '10px',
    marginBottom: '20px',
    boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
  },
  title: {
    marginBottom: '15px',
    color: '#333',
  },
  buttonGroup: {
    display: 'flex',
    gap: '15px',
    flexWrap: 'wrap',
  },
  button: {
    padding: '15px 25px',
    background: '#f5f5f5',
    border: '2px solid #ddd',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'all 0.3s',
    textAlign: 'left',
  },
  buttonActive: {
    background: '#00684a',
    borderColor: '#00684a',
    color: 'white',
  },
  tenantId: {
    fontSize: '0.9rem',
    fontWeight: '600',
    marginBottom: '5px',
  },
  tenantName: {
    fontSize: '0.85rem',
    opacity: 0.8,
  },
};

export default TenantSelector;

