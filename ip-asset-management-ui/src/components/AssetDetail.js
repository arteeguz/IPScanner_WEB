import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, Badge, Spinner, Alert, Button, Row, Col, ListGroup } from 'react-bootstrap';
import ApiService from '../services/api.service';

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
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Asset Details: {asset.hostname || asset.ipAddress}</h2>
        <Button as={Link} to="/assets" variant="secondary">
          Back to Assets
        </Button>
      </div>
      
      <Row>
        <Col md={8}>
          <Card className="mb-4">
            <Card.Header>
              <h4>Basic Information</h4>
            </Card.Header>
            <Card.Body>
              <Row>
                <Col md={6}>
                  <p><strong>IP Address:</strong> {asset.ipAddress}</p>
                  <p><strong>Hostname:</strong> {asset.hostname || 'Unknown'}</p>
                  <p>
                    <strong>Status:</strong>{' '}
                    {asset.online ? (
                      <Badge bg="success">Online</Badge>
                    ) : (
                      <Badge bg="danger">Offline</Badge>
                    )}
                  </p>
                  <p><strong>MAC Address:</strong> {asset.macAddress || 'Unknown'}</p>
                </Col>
                <Col md={6}>
                  <p>
                    <strong>Asset Type:</strong>{' '}
                    <Badge bg="info">{asset.assetType}</Badge>
                  </p>
                  <p><strong>Operating System:</strong> {asset.operatingSystem || 'Unknown'}</p>
                  <p><strong>OS Version:</strong> {asset.osVersion || 'Unknown'}</p>
                  <p><strong>Manufacturer:</strong> {asset.manufacturer || 'Unknown'}</p>
                  <p><strong>Model:</strong> {asset.model || 'Unknown'}</p>
                </Col>
              </Row>
            </Card.Body>
          </Card>

          {asset.additionalInfo && Object.keys(asset.additionalInfo).length > 0 && (
            <Card className="mb-4">
              <Card.Header>
                <h4>Additional Information</h4>
              </Card.Header>
              <ListGroup variant="flush">
                {Object.entries(asset.additionalInfo).map(([key, value]) => (
                  <ListGroup.Item key={key}>
                    <strong>{key}:</strong> {JSON.stringify(value)}
                  </ListGroup.Item>
                ))}
              </ListGroup>
            </Card>
          )}
        </Col>
        
        <Col md={4}>
          <Card className="mb-4">
            <Card.Header>
              <h4>Discovery Information</h4>
            </Card.Header>
            <Card.Body>
              <p><strong>First Discovered:</strong> {formatDateTime(asset.firstDiscovered)}</p>
              <p><strong>Last Seen:</strong> {formatDateTime(asset.lastSeen)}</p>
            </Card.Body>
          </Card>
          
          <Card>
            <Card.Header>
              <h4>Actions</h4>
            </Card.Header>
            <Card.Body>
              <div className="d-grid gap-2">
                <Button as={Link} to="/scans/create" variant="primary">
                  Scan Again
                </Button>
                
                <Button variant="outline-danger">
                  Delete Asset
                </Button>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AssetDetail;