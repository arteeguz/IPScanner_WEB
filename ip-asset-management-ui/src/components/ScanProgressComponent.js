import React, { useState, useEffect } from 'react';
import { ProgressBar, Card, Badge, ListGroup, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';

const ScanProgressComponent = ({ scanJobId, onComplete }) => {
  const [scanJob, setScanJob] = useState(null);
  const [progress, setProgress] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await ApiService.getScanJob(scanJobId);
        setScanJob(response.data);
        
        if (response.data.totalTargets > 0) {
          const completedPercentage = Math.floor((response.data.completedTargets / response.data.totalTargets) * 100);
          setProgress(completedPercentage);
        }
        
        setLoading(false);
        
        // Clear any previous errors on successful fetch
        if (error) setError('');
        
        // Check if scan is complete
        if (response.data.status === 'COMPLETED' || response.data.status === 'FAILED' || response.data.status === 'CANCELLED') {
          if (onComplete) {
            onComplete(response.data);
          }
        }
      } catch (err) {
        console.error("Scan progress fetch error:", err);
        
        // Check if it's an auth error
        if (err.response && err.response.status === 401) {
          setError('Authentication error. Please log out and log back in.');
        } else {
          setError('Failed to fetch scan progress. Will retry automatically...');
        }
        
        // We don't set loading to false on error so the spinner keeps showing
        // But we do continue polling
      }
    };
    
    // Initial fetch
    fetchData();
    
    // Set up polling interval (every 2 seconds)
    const interval = setInterval(fetchData, 2000);
    
    // Clean up interval on unmount
    return () => clearInterval(interval);
  }, [scanJobId, onComplete, error]);
  
  const getStatusVariant = (status) => {
    switch (status) {
      case 'RUNNING': return 'primary';
      case 'COMPLETED': return 'success';
      case 'FAILED': return 'danger';
      case 'CANCELLED': return 'warning';
      default: return 'secondary';
    }
  };

  if (loading && !scanJob) {
    return (
      <Card className="mb-4">
        <Card.Body className="text-center">
          <div className="d-flex justify-content-center">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
          <p className="mt-3">Loading scan information...</p>
        </Card.Body>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="mb-4">
        <Card.Body>
          <Alert variant="danger">{error}</Alert>
          <div className="d-flex justify-content-center mt-3">
            <div className="spinner-border spinner-border-sm text-primary me-2" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <span>Retrying...</span>
          </div>
        </Card.Body>
      </Card>
    );
  }

  if (!scanJob) {
    return <Alert variant="warning">No scan job found</Alert>;
  }

  return (
    <Card className="mb-4 shadow-sm">
      <Card.Header className="d-flex justify-content-between align-items-center">
        <h5 className="mb-0">Scan Progress</h5>
        <Badge bg={getStatusVariant(scanJob.status)}>{scanJob.status}</Badge>
      </Card.Header>
      <Card.Body>
        <div className="mb-3">
          <div className="d-flex justify-content-between mb-1">
            <span>Progress</span>
            <span>{progress}%</span>
          </div>
          <ProgressBar 
            now={progress} 
            variant={scanJob.status === 'FAILED' ? 'danger' : 'primary'} 
            animated={scanJob.status === 'RUNNING'} 
            className="mb-3" 
          />
        </div>
        
        <ListGroup className="mb-3">
          <ListGroup.Item className="d-flex justify-content-between align-items-center">
            Total Targets
            <Badge bg="info" pill>{scanJob.totalTargets}</Badge>
          </ListGroup.Item>
          <ListGroup.Item className="d-flex justify-content-between align-items-center">
            Completed
            <Badge bg="primary" pill>{scanJob.completedTargets}</Badge>
          </ListGroup.Item>
          <ListGroup.Item className="d-flex justify-content-between align-items-center">
            Successful
            <Badge bg="success" pill>{scanJob.successfulTargets}</Badge>
          </ListGroup.Item>
          <ListGroup.Item className="d-flex justify-content-between align-items-center">
            Failed
            <Badge bg="danger" pill>{scanJob.failedTargets}</Badge>
          </ListGroup.Item>
        </ListGroup>
        
        {scanJob.status === 'RUNNING' && (
          <div className="text-center">
            <div className="spinner-border spinner-border-sm text-primary me-2" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <span>Scan in progress...</span>
          </div>
        )}
        
        {scanJob.status === 'COMPLETED' && (
          <div className="text-center text-success">
            <i className="bi bi-check-circle-fill me-2"></i>
            <span>Scan completed successfully</span>
          </div>
        )}
        
        {(scanJob.status === 'FAILED' || scanJob.status === 'CANCELLED') && (
          <Alert variant={scanJob.status === 'FAILED' ? 'danger' : 'warning'}>
            {scanJob.status === 'FAILED' ? 'Scan failed to complete' : 'Scan was cancelled'}
          </Alert>
        )}
      </Card.Body>
    </Card>
  );
};

export default ScanProgressComponent;