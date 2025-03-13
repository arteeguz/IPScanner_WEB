import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Table, Button, Badge, Spinner, Alert, Card } from 'react-bootstrap';
import ApiService from '../services/api.service';

const ScanJobs = () => {
  const [scanJobs, setScanJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [runningJobs, setRunningJobs] = useState({});
  
  const navigate = useNavigate();

  useEffect(() => {
    loadScanJobs();
    
    // Set up polling for updating job status
    const interval = setInterval(() => {
      loadScanJobs(false);
    }, 5000); // Poll every 5 seconds
    
    return () => clearInterval(interval);
  }, []);

  const loadScanJobs = (showLoading = true) => {
    if (showLoading) {
      setLoading(true);
    }
    
    ApiService.getAllScanJobs()
      .then(response => {
        setScanJobs(response.data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load scan jobs. Please try again later.');
        setLoading(false);
      });
  };

  const handleRunJob = (id) => {
    setRunningJobs({
      ...runningJobs,
      [id]: true
    });
    
    ApiService.runScanJob(id)
      .then(() => {
        // Update the job status after a slight delay
        setTimeout(() => {
          loadScanJobs(false);
          setRunningJobs({
            ...runningJobs,
            [id]: false
          });
        }, 1000);
      })
      .catch(err => {
        setError('Failed to run scan job. Please try again.');
        setRunningJobs({
          ...runningJobs,
          [id]: false
        });
      });
  };

  const handleDeleteJob = (id) => {
    if (window.confirm('Are you sure you want to delete this scan job?')) {
      ApiService.deleteScanJob(id)
        .then(() => {
          loadScanJobs();
        })
        .catch(err => {
          setError('Failed to delete scan job. Please try again.');
        });
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CREATED':
        return <Badge bg="secondary">Created</Badge>;
      case 'SCHEDULED':
        return <Badge bg="info">Scheduled</Badge>;
      case 'RUNNING':
        return <Badge bg="warning">Running</Badge>;
      case 'COMPLETED':
        return <Badge bg="success">Completed</Badge>;
      case 'FAILED':
        return <Badge bg="danger">Failed</Badge>;
      case 'CANCELLED':
        return <Badge bg="dark">Cancelled</Badge>;
      default:
        return <Badge bg="secondary">{status}</Badge>;
    }
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return 'N/A';
    const date = new Date(dateTimeStr);
    return date.toLocaleString();
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Scan Jobs</h2>
        <Button variant="primary" as={Link} to="/scans/create">
          Create New Scan
        </Button>
      </div>
      
      {error && <Alert variant="danger">{error}</Alert>}
      
      {loading ? (
        <div className="text-center my-5">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Loading...</span>
          </Spinner>
        </div>
      ) : scanJobs.length > 0 ? (
        <Card>
          <Card.Body>
            <Table responsive striped hover>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Last Run</th>
                  <th>Progress</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {scanJobs.map(job => (
                  <tr key={job.id}>
                    <td>{job.name}</td>
                    <td>{getStatusBadge(job.status)}</td>
                    <td>{formatDateTime(job.createdAt)}</td>
                    <td>{formatDateTime(job.lastRunAt)}</td>
                    <td>
                      {job.status === 'RUNNING' ? (
                        `${job.completedTargets || 0}/${job.totalTargets || 0} targets`
                      ) : job.status === 'COMPLETED' ? (
                        `${job.successfulTargets || 0} successful, ${job.failedTargets || 0} failed`
                      ) : 'N/A'}
                    </td>
                    <td>
                      <Button 
                        variant="success" 
                        size="sm" 
                        className="me-2"
                        disabled={job.status === 'RUNNING' || runningJobs[job.id]}
                        onClick={() => handleRunJob(job.id)}
                      >
                        {runningJobs[job.id] ? (
                          <>
                            <Spinner animation="border" size="sm" /> Running...
                          </>
                        ) : (
                          'Run'
                        )}
                      </Button>
                      
                      <Button 
                        variant="info" 
                        size="sm" 
                        className="me-2"
                        onClick={() => navigate(`/scans/${job.id}/results`)}
                      >
                        Results
                      </Button>
                      
                      <Button 
                        variant="danger" 
                        size="sm"
                        onClick={() => handleDeleteJob(job.id)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      ) : (
        <Alert variant="info">
          No scan jobs found. Click "Create New Scan" to get started.
        </Alert>
      )}
    </div>
  );
};

export default ScanJobs;