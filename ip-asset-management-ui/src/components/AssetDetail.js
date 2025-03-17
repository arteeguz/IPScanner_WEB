import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, Badge, Spinner, Alert, Button, Row, Col, ListGroup, Container } from 'react-bootstrap';
import ApiService from '../services/api.service';
import SystemInfoDisplay from './SystemInfoDisplay';

const AssetDetail = () => {
  const { id } = useParams();
  const [asset, setAsset] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadAsset();
  }, [id]);

  const loadAsset = () => {
    setLoading(true);
    
    ApiService.getAssetById(id)
      .then(response => {
        setAsset(response.data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load asset details. Please try again later.');
        setLoading(false);
      });
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return 'N/A';
    const date = new Date(dateTimeStr);
    return date.toLocaleString();
  };

  if (loading) {
    return (
      <div className="text-center my-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger">{error}</Alert>;
  }

  if (!asset) {
    return <Alert variant="warning">Asset not found.</Alert>;
  }

  return (
    <Container>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          {asset.hostname || asset.ipAddress}
          <Badge bg={asset.online ? 'success' : 'danger'} className="ms-2">
            {asset.online ? 'Online' : 'Offline'}
          </Badge>
        </h2>
        <Button as={Link} to="/assets" variant="secondary">
          Back to Assets
        </Button>
      </div>
      
      <Row className="mb-4">
        <Col md={4}>
          <Card className="shadow-sm h-100">
            <Card.Header className="bg-primary text-white">
              <h5 className="mb-0">Basic Information</h5>
            </Card.Header>
            <ListGroup variant="flush">
              <ListGroup.Item>
                <i className="bi bi-hdd-network me-2"></i>
                <strong>IP Address:</strong> {asset.ipAddress}
              </ListGroup.Item>
              <ListGroup.Item>
                <i className="bi bi-pc-display me-2"></i>
                <strong>Hostname:</strong> {asset.hostname || 'Unknown'}
              </ListGroup.Item>
              <ListGroup.Item>
                <i className="bi bi-tag me-2"></i>
                <strong>Asset Type:</strong>{' '}
                <Badge bg="info">{asset.assetType}</Badge>
              </ListGroup.Item>
              <ListGroup.Item>
                <i className="bi bi-cpu me-2"></i>
                <strong>Operating System:</strong> {asset.operatingSystem || 'Unknown'}
              </ListGroup.Item>
              <ListGroup.Item>
                <i className="bi bi-info-circle me-2"></i>
                <strong>OS Version:</strong> {asset.osVersion || 'Unknown'}
              </ListGroup.Item>
              <ListGroup.Item>
                <i className="bi bi-building me-2"></i>
                <strong>Manufacturer:</strong> {asset.manufacturer || 'Unknown'}
              </ListGroup.Item>
              <ListGroup.Item>
                <i className="bi bi-cpu-fill me-2"></i>
                <strong>Model:</strong> {asset.model || 'Unknown'}
              </ListGroup.Item>
            </ListGroup>
          </Card>
        </Col>
        
        <Col md={4}>
          <Card className="shadow-sm h-100">
            <Card.Header className="bg-info text-white">
              <h5 className="mb-0">Discovery Information</h5>
            </Card.Header>
            <Card.Body>
              <ListGroup variant="flush">
                <ListGroup.Item>
                  <i className="bi bi-calendar-check me-2"></i>
                  <strong>First Discovered:</strong> {formatDateTime(asset.firstDiscovered)}
                </ListGroup.Item>
                <ListGroup.Item>
                  <i className="bi bi-calendar-event me-2"></i>
                  <strong>Last Seen:</strong> {formatDateTime(asset.lastSeen)}
                </ListGroup.Item>
                <ListGroup.Item>
                  <i className="bi bi-ethernet me-2"></i>
                  <strong>MAC Address:</strong> {asset.macAddress || 'Unknown'}
                </ListGroup.Item>
                {asset.lastScanId && (
                  <ListGroup.Item>
                    <i className="bi bi-search me-2"></i>
                    <strong>Last Scan:</strong>{' '}
                    <Link to={`/scans/${asset.lastScanId}/results`}>
                      View Details
                    </Link>
                  </ListGroup.Item>
                )}
              </ListGroup>
            </Card.Body>
          </Card>
        </Col>
        
        <Col md={4}>
          <Card className="shadow-sm h-100">
            <Card.Header className="bg-success text-white">
              <h5 className="mb-0">Actions</h5>
            </Card.Header>
            <Card.Body className="d-flex flex-column justify-content-between">
              <p>
                What would you like to do with this asset?
              </p>
              
              <div className="d-grid gap-2">
                <Button as={Link} to="/scans/create" variant="primary">
                  <i className="bi bi-radar me-2"></i>
                  Scan Again
                </Button>
                
                <Button as={Link} to={`/scans?ip=${asset.ipAddress}`} variant="info">
                  <i className="bi bi-clock-history me-2"></i>
                  View Scan History
                </Button>
                
                <Button variant="outline-danger">
                  <i className="bi bi-trash me-2"></i>
                  Delete Asset
                </Button>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
      
      <Card className="shadow-sm mb-4">
        <Card.Header className="bg-dark text-white">
          <h4 className="mb-0">System Information</h4>
        </Card.Header>
        <Card.Body>
          <SystemInfoDisplay assetInfo={asset} />
        </Card.Body>
      </Card>
    </Container>
  );
};

export default AssetDetail;