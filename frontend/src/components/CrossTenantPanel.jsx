import React, { useState } from 'react';
import { demoApi } from '../services/api';
import ReactJson from '@microlink/react-json-view';

function CrossTenantPanel({ tenants }) {
  const [attackerTenant, setAttackerTenant] = useState(tenants[0]?.id || '');
  const [victimTenant, setVictimTenant] = useState(tenants[1]?.id || '');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const attemptAttack = async () => {
    if (attackerTenant === victimTenant) {
      setError('Please select different tenants for attacker and victim');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await demoApi.attemptCrossTenantDecryption(attackerTenant, victimTenant);
      setResult(response.data);
    } catch (err) {
      setError('Attack simulation failed: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="panel">
      <h2>Cross-Tenant Attack Simulation</h2>
      
      <div className="warning">
        <strong>🔒 Demo Scenario:</strong> This simulates a scenario where one tenant
        attempts to decrypt another tenant's data. Each tenant has their own master key,
        ensuring true cryptographic isolation - even if an attacker gains access to another
        tenant's encrypted data, they cannot decrypt it without that tenant's master key.
      </div>

      <div className="form">
        <h3>Configure Attack Scenario</h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <div className="form-group">
            <label>Attacker Tenant:</label>
            <select
              value={attackerTenant}
              onChange={(e) => setAttackerTenant(e.target.value)}
            >
              {tenants.map((tenant) => (
                <option key={tenant.id} value={tenant.id}>
                  {tenant.id} ({tenant.name})
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Victim Tenant:</label>
            <select
              value={victimTenant}
              onChange={(e) => setVictimTenant(e.target.value)}
            >
              {tenants.map((tenant) => (
                <option key={tenant.id} value={tenant.id}>
                  {tenant.id} ({tenant.name})
                </option>
              ))}
            </select>
          </div>
        </div>
        <button 
          onClick={attemptAttack} 
          className="btn"
          disabled={loading || attackerTenant === victimTenant}
        >
          {loading ? 'Attempting...' : 'Attempt Cross-Tenant Decryption'}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {result && (
        <div style={{ marginTop: '20px' }}>
          <div className="info">
            <strong>Attack Scenario:</strong> {attackerTenant} attempting to decrypt {victimTenant}'s data
          </div>

          {result.error && (
            <div className="success" style={{ marginTop: '15px' }}>
              <strong>✅ Cryptographic Isolation Enforced!</strong>
              <p style={{ marginTop: '10px' }}>
                The decryption attempt failed with error: <code>{result.error}</code>
              </p>
              <p style={{ marginTop: '10px' }}>
                This demonstrates true multi-tenant cryptographic isolation! Even though the
                attacker can retrieve the encrypted document from MongoDB, they cannot decrypt
                it because:
              </p>
              <ul style={{ marginTop: '10px', marginLeft: '20px' }}>
                <li>Each tenant has their own master key</li>
                <li>Each tenant's DEK is encrypted with their own master key</li>
                <li>The attacker's client only has access to their own master key</li>
                <li>Without the victim's master key, the attacker cannot decrypt the victim's DEK</li>
                <li>Without the victim's DEK, the attacker cannot decrypt the victim's data</li>
              </ul>
            </div>
          )}

          {result.rawDocument && (
            <div style={{ marginTop: '20px' }}>
              <h3>Victim's Raw Document (Encrypted)</h3>
              <div style={{ 
                background: '#1e1e1e', 
                padding: '15px', 
                borderRadius: '8px',
                marginTop: '10px',
                maxHeight: '300px',
                overflow: 'auto'
              }}>
                <ReactJson
                  src={result.rawDocument}
                  theme="monokai"
                  collapsed={1}
                  displayDataTypes={false}
                  displayObjectSize={false}
                  enableClipboard={true}
                  name="rawDocument"
                />
              </div>
            </div>
          )}

          {result.attemptedDecryption && (
            <div style={{ marginTop: '20px' }}>
              <h3>Attacker's Decryption Attempt Result</h3>

              {result.decryptionResult && (
                <div className={result.fieldsStillEncrypted ? "success" : "warning"} style={{ marginTop: '15px' }}>
                  <strong>{result.decryptionResult}</strong>
                  {result.explanation && (
                    <p style={{ marginTop: '10px', fontSize: '14px', whiteSpace: 'pre-line' }}>
                      {result.explanation}
                    </p>
                  )}
                  {result.securityNote && (
                    <div style={{
                      marginTop: '15px',
                      padding: '10px',
                      background: '#fff3cd',
                      border: '1px solid #ffc107',
                      borderRadius: '4px',
                      color: '#856404'
                    }}>
                      <strong>🔒 Security Note:</strong> {result.securityNote}
                    </div>
                  )}
                </div>
              )}

              <div style={{
                background: '#1e1e1e',
                padding: '15px',
                borderRadius: '8px',
                marginTop: '10px',
                maxHeight: '300px',
                overflow: 'auto'
              }}>
                <ReactJson
                  src={result.attemptedDecryption}
                  theme="monokai"
                  collapsed={1}
                  displayDataTypes={false}
                  displayObjectSize={false}
                  enableClipboard={true}
                  name="attemptedDecryption"
                />
              </div>
            </div>
          )}

          <div className="info" style={{ marginTop: '20px' }}>
            <strong>🔐 Key Takeaways:</strong>
            <ul style={{ marginTop: '10px', marginLeft: '20px' }}>
              <li><strong>✅ Each tenant has its own Master Key</strong> - true cryptographic isolation</li>
              <li><strong>✅ Each tenant has its own Data Encryption Key (DEK)</strong> - encrypted with their master key</li>
              <li><strong>✅ Documents are encrypted with the tenant's specific DEK</strong> - ensuring data isolation</li>
              <li><strong>✅ Cross-tenant decryption is cryptographically impossible</strong> - without the victim's master key</li>
              <li><strong>🔒 CSFLE protects data at rest and in transit</strong> - encrypted before leaving the application</li>
              <li><strong>🔒 Defense in depth</strong> - combine CSFLE with application-level access control</li>
              <li><strong>💡 Production recommendation</strong> - use cloud KMS (AWS KMS, Azure Key Vault, GCP Cloud KMS) with per-tenant keys</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}

export default CrossTenantPanel;

