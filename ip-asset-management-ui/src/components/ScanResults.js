import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Table, Card, Badge, Spinner, Alert, Row, Col, Button } from 'react-bootstrap';
import ApiService from '../services/api.service';

const ScanResults = () => {
  const { id } = useParams();
  const [scanJob, setScanJob] = useState(null);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadData();
    
    // If job is running, poll for updates
    const interval = setInterval(() => {
      if (scanJob && scanJob.status === 'RUNNING') {
        loadData(false);
      }
    }, 5000);
    
    return () => clearInterval(interval);
  }, [id, scanJob]);

  const loadData = (showLoading = true) => {
    if (showLoading) {
      setLoading(true);
    }
    
    // Load scan job details
    ApiService.getScanJobById(id)
      .then(response => {
        setScanJob(response.data);
        
        // Load scan results
        return ApiService.getScanResults(id);
      })
      .then(response => {
        setResults(response.data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load scan results. Please try again later.');
        setLoading(false);
      });
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

  if (loading && !scanJob) {
    return (
      <div className="text-center my-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <div>
      {error && <Alert variant="danger">{error}</Alert>}
      
      {scanJob && (
        <>
          <div className="d-flex justify-content-between align-items-center mb-4">
            <h2>Results: {scanJob.name}</h2>
            <Button as={Link} to="/scans" variant="secondary">
              Back to Scan Jobs
            </Button>
          </div>
          
          <Card className="mb-4">
            <Card.Body>
              <Row>
                <Col md={4}>
                  <strong>Status:</strong> {getStatusBadge(scanJob.status)}
                </Col>
                <Col md={4}>
                  <strong>Created:</strong> {formatDateTime(scanJob.createdAt)}
                </Col>
                <Col md={4}>
                  <strong>Last Run:</strong> {formatDateTime(scanJob.lastRunAt)}
                </Col>
              </Row>
              <Row className="mt-3">
                <Col md={4}>
                  <strong>Total Targets:</strong> {scanJob.totalTargets}
                </Col>
                <Col md={4}>
                  <strong>Successful:</strong> {scanJob.successfulTargets}
                </Col>
                <Col md={4}>
                  <strong>Failed:</strong> {scanJob.failedTargets}
                </Col>
              </Row>
              {scanJob.description && (
                <Row className="mt-3">
                  <Col>
                    <strong>Description:</strong> {scanJob.description}
                  </Col>
                </Row>
              )}
            </Card.Body>
          </Card>
          
          {scanJob.status === 'RUNNING' && (
            <Alert variant="info">
              <Spinner animation="border" size="sm" className="me-2" />
              Scan is currently running. Results will update automatically.
            </Alert>
          )}
          
          <h3>Discovered Assets</h3>
          
          {results.length > 0 ? (
            <Card>
              <Card.Body>
                <Table responsive striped hover>
                  <thead>
                    <tr>
                      <th>IP Address</th>
                      <th>Hostname</th>
                      <th>Status</th>
                      <th>Scan Time</th>
                      <th>Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map(result => (
                      <tr key={result.id}>
                        <td>{result.ipAddress}</td>
                        <td>{result.hostname || 'Unknown'}</td>
                        <td>
                          {result.successful ? (
                            <Badge bg="success">Success</Badge>
                          ) : (
                            <Badge bg="danger">Failed</Badge>
                          )}
                        </td>
                        <td>{formatDateTime(result.scanTime)}</td>
                        <td>
                          {result.assetId ? (
                            <Button 
                              as={Link} 
                              to={`/assets/${result.assetId}`} 
                              variant="info" 
                              size="sm"
                            >
                              View Asset
                            </Button>
                          ) : (
                            'No asset created'
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </Card.Body>
            </Card>
          ) : (
            <Alert variant="info">
              No scan results available yet.
              {scanJob.status === 'CREATED' && (
                <div className="mt-2">
                  Return to the scan jobs page to run this scan.
                </div>
              )}
            </Alert>
          )}
        </>
      )}
    </div>
  );
};

export default ScanResults;