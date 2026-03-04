import React, { useState } from 'react';
import { demoApi } from '../services/api';
import ReactJson from '@microlink/react-json-view';

function DbaViewPanel() {
  const [rawData, setRawData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadRawDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await demoApi.getRawDocuments();
      setRawData(response.data);
    } catch (err) {
      setError('Failed to load raw documents: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="panel">
      <h2>DBA View - Raw MongoDB Documents</h2>
      
      <div className="warning">
        <strong>⚠️ Demo Scenario:</strong> This view simulates what a database administrator 
        or attacker with direct MongoDB access would see. Notice that sensitive fields appear 
        as <code>BinData</code> (encrypted binary blobs) - not human-readable plaintext.
      </div>

      <button 
        onClick={loadRawDocuments} 
        className="btn"
        disabled={loading}
      >
        {loading ? 'Loading...' : 'View Raw MongoDB Documents'}
      </button>

      {error && <div className="error">{error}</div>}

      {rawData && (
        <div style={{ marginTop: '20px' }}>
          <div className="info">
            <strong>ℹ️ {rawData.message}</strong>
          </div>

          <div style={{ marginTop: '20px' }}>
            <h3>Raw Customers Collection (First 5 Documents)</h3>
            <div style={{ 
              background: '#1e1e1e', 
              padding: '15px', 
              borderRadius: '8px',
              marginTop: '10px',
              maxHeight: '400px',
              overflow: 'auto'
            }}>
              <ReactJson
                src={rawData.customers}
                theme="monokai"
                collapsed={1}
                displayDataTypes={false}
                displayObjectSize={false}
                enableClipboard={true}
                name="customers"
              />
            </div>
          </div>

          <div style={{ marginTop: '30px' }}>
            <h3>Raw Orders Collection (First 5 Documents)</h3>
            <div style={{ 
              background: '#1e1e1e', 
              padding: '15px', 
              borderRadius: '8px',
              marginTop: '10px',
              maxHeight: '400px',
              overflow: 'auto'
            }}>
              <ReactJson
                src={rawData.orders}
                theme="monokai"
                collapsed={1}
                displayDataTypes={false}
                displayObjectSize={false}
                enableClipboard={true}
                name="orders"
              />
            </div>
          </div>

          <div className="info" style={{ marginTop: '20px' }}>
            <strong>🔐 Key Observations:</strong>
            <ul style={{ marginTop: '10px', marginLeft: '20px' }}>
              <li><code>tenantId</code>, <code>customerId</code>, <code>orderId</code> are plaintext (needed for routing)</li>
              <li><code>name</code>, <code>email</code>, <code>phone</code>, <code>address</code>, <code>product</code>, <code>amount</code>, <code>status</code> appear as <code>$binary</code> objects</li>
              <li>Even with full database access, the sensitive data is cryptographically protected</li>
              <li>Only clients with the correct Data Encryption Key (DEK) can decrypt the data</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}

export default DbaViewPanel;

