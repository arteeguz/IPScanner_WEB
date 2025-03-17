import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Container, Row, Col } from 'react-bootstrap';
import ApiService from '../services/api.service';
import AdvancedScanForm from './AdvancedScanForm';
import ResourceMonitor from './ResourceMonitor';
import ScanProgressComponent from './ScanProgressComponent';

const CreateScanJob = () => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState(false);
  const [activeScanId, setActiveScanId] = useState(null);
  const navigate = useNavigate();

  const handleSubmit = (scanJobData) => {
    setMessage('');
    setError(false);
    setLoading(true);

    ApiService.createScanJob(scanJobData)
      .then(response => {
        const scanId = response.data.id;
        
        // Run the scan job
        return ApiService.runScanJob(scanId)
          .then(() => {
            setActiveScanId(scanId);
            setMessage('Scan job started successfully!');
            setLoading(false);
          });
      })
      .catch(error => {
        const resMessage = 
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();
        
        setLoading(false);
        setError(true);
        setMessage(resMessage);
      });
  };
  
  const handleScanComplete = (completedScan) => {
    // Do something when scan completes
    console.log('Scan completed:', completedScan);
  };

  return (
    <Container>
      <h2 className="text-center mb-4">Network Scan</h2>
      
      <Row>
        <Col lg={8}>
          {activeScanId ? (
            <ScanProgressComponent 
              scanJobId={activeScanId} 
              onComplete={handleScanComplete}
            />
          ) : (
            <AdvancedScanForm onSubmit={handleSubmit} loading={loading} />
          )}
          
          {message && (
            <Alert variant={error ? 'danger' : 'success'} className="mt-3">
              {message}
            </Alert>
          )}
        </Col>
        
        <Col lg={4}>
          <ResourceMonitor />
          
          {activeScanId && (
            <div className="d-grid gap-2 mt-3">
              <button 
                className="btn btn-outline-primary" 
                onClick={() => navigate('/scans/' + activeScanId + '/results')}
              >
                View Detailed Results
              </button>
              
              <button 
                className="btn btn-outline-secondary" 
                onClick={() => setActiveScanId(null)}
              >
                Start New Scan
              </button>
            </div>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default CreateScanJob;