import React, { useState } from 'react';
import { demoApi } from '../services/api';

const ApplicationLevelAccessPanel = () => {
  const [selectedTenant, setSelectedTenant] = useState('tenant_alpha');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const tenants = [
    { id: 'tenant_alpha', name: 'Acme Corp' },
    { id: 'tenant_beta', name: 'Globex Inc' },
    { id: 'tenant_gamma', name: 'Initech LLC' },
  ];

  const handleDemonstrate = async () => {
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await demoApi.demonstrateApplicationLevelAccessControl(selectedTenant);
      console.log('Response:', response.data);
      setResult(response.data);
    } catch (err) {
      console.error('Error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to demonstrate access control');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <h2 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '20px', color: '#333' }}>
        🛡️ Application-Level Access Control Demo
      </h2>

      <div style={{
        marginBottom: '20px',
        padding: '15px',
        backgroundColor: '#e3f2fd',
        borderLeft: '4px solid #2196f3',
        borderRadius: '4px'
      }}>
        <h3 style={{ fontWeight: '600', color: '#1565c0', marginBottom: '10px' }}>
          Defense in Depth Strategy
        </h3>
        <p style={{ fontSize: '14px', color: '#1976d2' }}>
          This demo shows how <strong>application-level filtering</strong> complements <strong>cryptographic isolation</strong>.
          Even with separate master keys, your application must ALWAYS filter queries by tenantId to prevent data leakage.
        </p>
      </div>

      <div style={{ marginBottom: '20px' }}>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: '500', color: '#555', marginBottom: '8px' }}>
          Select Tenant:
        </label>
        <select
          value={selectedTenant}
          onChange={(e) => setSelectedTenant(e.target.value)}
          style={{
            width: '100%',
            padding: '10px',
            border: '1px solid #ccc',
            borderRadius: '4px',
            fontSize: '14px'
          }}
        >
          {tenants.map((tenant) => (
            <option key={tenant.id} value={tenant.id}>
              {tenant.name} ({tenant.id})
            </option>
          ))}
        </select>
      </div>

      <button
        onClick={handleDemonstrate}
        disabled={loading}
        style={{
          width: '100%',
          backgroundColor: loading ? '#999' : '#2196f3',
          color: 'white',
          padding: '12px 16px',
          borderRadius: '4px',
          border: 'none',
          fontSize: '16px',
          fontWeight: '500',
          cursor: loading ? 'not-allowed' : 'pointer'
        }}
      >
        {loading ? 'Running Demo...' : 'Demonstrate Access Control'}
      </button>

      {error && (
        <div style={{
          marginTop: '20px',
          padding: '20px',
          backgroundColor: '#fff3e0',
          border: '2px solid #ff9800',
          borderRadius: '8px'
        }}>
          <p style={{ color: '#e65100', fontWeight: '700', fontSize: '18px', marginBottom: '15px' }}>
            ⚠️ Backend Restart Required
          </p>
          <p style={{ color: '#ef6c00', fontSize: '15px', marginBottom: '15px', lineHeight: '1.6' }}>
            The backend code has been updated to fix the Application-Level Access Control demo,
            but the changes won't take effect until you restart the backend server.
          </p>

          <div style={{ backgroundColor: '#ffe0b2', padding: '15px', borderRadius: '6px', marginBottom: '15px' }}>
            <p style={{ color: '#e65100', fontSize: '14px', fontWeight: '600', marginBottom: '10px' }}>
              📋 Steps to Restart:
            </p>
            <ol style={{ color: '#ef6c00', fontSize: '14px', marginLeft: '20px', lineHeight: '1.8' }}>
              <li>Find the terminal or IDE where the backend is running</li>
              <li>Stop it (press <code style={{ backgroundColor: '#fff', padding: '2px 6px', borderRadius: '3px', fontWeight: '600' }}>Ctrl+C</code>)</li>
              <li>Restart with: <code style={{ backgroundColor: '#fff', padding: '4px 8px', borderRadius: '3px', fontWeight: '600', display: 'inline-block', marginTop: '4px' }}>mvn spring-boot:run</code></li>
              <li>Wait for "Started FledemoApplication" message</li>
              <li>Refresh this page and try again</li>
            </ol>
          </div>

          <div style={{ backgroundColor: '#ffccbc', padding: '12px', borderRadius: '6px' }}>
            <p style={{ color: '#bf360c', fontSize: '13px', fontWeight: '600', marginBottom: '5px' }}>
              Technical Error:
            </p>
            <p style={{ color: '#d84315', fontSize: '13px', fontFamily: 'monospace' }}>
              {error}
            </p>
          </div>

          <p style={{ color: '#ef6c00', fontSize: '13px', marginTop: '15px', fontStyle: 'italic' }}>
            💡 <strong>Why this happened:</strong> The running backend is still using old code that tries to decrypt data,
            causing HMAC validation errors. The new code uses a non-encrypted connection for this demo.
          </p>
        </div>
      )}

      {result && (
        <div style={{ marginTop: '30px' }}>
          {/* Scenario A: No Filter (Incorrect) */}
          <div style={{
            padding: '20px',
            backgroundColor: '#ffebee',
            border: '2px solid #ef5350',
            borderRadius: '8px',
            marginBottom: '20px'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#b71c1c', marginBottom: '10px' }}>
              ❌ Scenario A: Query WITHOUT tenantId Filter
            </h3>
            <div style={{ marginBottom: '15px' }}>
              <code style={{
                fontSize: '13px',
                backgroundColor: '#ffcdd2',
                padding: '4px 8px',
                borderRadius: '4px',
                color: '#c62828'
              }}>
                {result.scenarioA_NoFilter?.query || 'collection.find()'}
              </code>
            </div>
            <div style={{ marginBottom: '15px', padding: '12px', backgroundColor: '#ffcdd2', borderRadius: '4px' }}>
              <p style={{ fontSize: '14px', fontWeight: '600', color: '#b71c1c', marginBottom: '8px' }}>
                ⚠️ {result.scenarioA_NoFilter?.issue || 'Security violation'}
              </p>
              <div style={{ fontSize: '13px', color: '#c62828', marginTop: '8px', backgroundColor: '#ffebee', padding: '8px', borderRadius: '4px' }}>
                <p style={{ marginBottom: '4px' }}>
                  📊 Documents returned (decrypted): <strong>{result.scenarioA_NoFilter?.totalDocuments || 0}</strong>
                </p>
                <p>
                  📦 Total documents in database: <strong>{result.scenarioA_NoFilter?.totalDocumentsInDatabase || 0}</strong>
                </p>
              </div>
            </div>
            <div>
              {result.scenarioA_NoFilter?.customers?.map((customer, idx) => (
                <div key={idx} style={{
                  padding: '10px',
                  backgroundColor: 'white',
                  borderRadius: '4px',
                  border: '1px solid #ef9a9a',
                  marginBottom: '8px'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: '600',
                      backgroundColor: customer.tenantId === selectedTenant ? '#c8e6c9' : '#ffcdd2',
                      color: customer.tenantId === selectedTenant ? '#2e7d32' : '#b71c1c'
                    }}>
                      {customer.tenantId}
                    </span>
                    <span style={{
                      fontSize: '14px',
                      fontStyle: customer.nameEncrypted ? 'italic' : 'normal',
                      color: customer.nameEncrypted ? '#666' : '#000'
                    }}>
                      {customer.name}
                    </span>
                    <span style={{ fontSize: '14px', color: '#999' }}>-</span>
                    <span style={{
                      fontSize: '14px',
                      fontStyle: customer.emailEncrypted ? 'italic' : 'normal',
                      color: customer.emailEncrypted ? '#666' : '#000'
                    }}>
                      {customer.email}
                    </span>
                    {customer.nameEncrypted && (
                      <span style={{
                        fontSize: '11px',
                        padding: '2px 6px',
                        backgroundColor: '#757575',
                        color: 'white',
                        borderRadius: '3px',
                        fontWeight: '600'
                      }}>
                        🔒 ENCRYPTED
                      </span>
                    )}
                  </div>
                </div>
              )) || <p>No data</p>}
            </div>
          </div>

          {/* Scenario B: With Filter (Correct) */}
          <div style={{
            padding: '20px',
            backgroundColor: '#e8f5e9',
            border: '2px solid #66bb6a',
            borderRadius: '8px',
            marginBottom: '20px'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#1b5e20', marginBottom: '10px' }}>
              ✅ Scenario B: Query WITH tenantId Filter
            </h3>
            <div style={{ marginBottom: '15px' }}>
              <code style={{
                fontSize: '13px',
                backgroundColor: '#c8e6c9',
                padding: '4px 8px',
                borderRadius: '4px',
                color: '#2e7d32'
              }}>
                {result.scenarioB_WithFilter?.query || `collection.find({ tenantId: '${selectedTenant}' })`}
              </code>
            </div>
            <div style={{ marginBottom: '15px', padding: '12px', backgroundColor: '#c8e6c9', borderRadius: '4px' }}>
              <p style={{ fontSize: '14px', fontWeight: '600', color: '#1b5e20' }}>
                ✅ {result.scenarioB_WithFilter?.success || 'Correct approach'}
              </p>
              <p style={{ fontSize: '14px', color: '#2e7d32', marginTop: '5px' }}>
                Total documents returned: <strong>{result.scenarioB_WithFilter?.totalDocuments || 0}</strong>
              </p>
            </div>
            <div>
              {result.scenarioB_WithFilter?.customers?.map((customer, idx) => (
                <div key={idx} style={{
                  padding: '10px',
                  backgroundColor: 'white',
                  borderRadius: '4px',
                  border: '1px solid #81c784',
                  marginBottom: '8px'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: '600',
                      backgroundColor: '#c8e6c9',
                      color: '#2e7d32'
                    }}>
                      {customer.tenantId}
                    </span>
                    <span style={{ fontSize: '14px' }}>{customer.name} - {customer.email}</span>
                  </div>
                </div>
              )) || <p>No data</p>}
            </div>
          </div>

          {/* Defense in Depth Explanation */}
          <div style={{
            padding: '20px',
            backgroundColor: '#f3e5f5',
            border: '2px solid #ab47bc',
            borderRadius: '8px'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#4a148c', marginBottom: '15px' }}>
              🔒 Defense in Depth: Two Layers of Security
            </h3>
            <div>
              <div style={{ padding: '12px', backgroundColor: 'white', borderRadius: '4px', border: '1px solid #ce93d8', marginBottom: '10px' }}>
                <p style={{ fontSize: '14px', fontWeight: '600', color: '#4a148c' }}>Layer 1: Cryptographic Isolation</p>
                <p style={{ fontSize: '14px', color: '#6a1b9a' }}>{result.defenseInDepth?.layer1 || 'Separate master keys'}</p>
              </div>
              <div style={{ padding: '12px', backgroundColor: 'white', borderRadius: '4px', border: '1px solid #ce93d8', marginBottom: '10px' }}>
                <p style={{ fontSize: '14px', fontWeight: '600', color: '#4a148c' }}>Layer 2: Application-Level Filtering</p>
                <p style={{ fontSize: '14px', color: '#6a1b9a' }}>{result.defenseInDepth?.layer2 || 'Always filter by tenantId'}</p>
              </div>
              <div style={{ padding: '12px', backgroundColor: '#e1bee7', borderRadius: '4px' }}>
                <p style={{ fontSize: '14px', fontWeight: '600', color: '#4a148c' }}>{result.defenseInDepth?.combined || 'Both layers work together'}</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApplicationLevelAccessPanel;

