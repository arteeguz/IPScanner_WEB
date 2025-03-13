import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Table, Card, Badge, Spinner, Alert, Button, Form, Row, Col } from 'react-bootstrap';
import ApiService from '../services/api.service';

const AssetList = () => {
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState({
    type: '',
    online: ''
  });

  useEffect(() => {
    loadAssets();
  }, [filter.type, filter.online]);

  const loadAssets = () => {
    setLoading(true);
    
    let apiCall;
    
    if (filter.type) {
      apiCall = ApiService.getAssetsByType(filter.type);
    } else if (filter.online !== '') {
      apiCall = ApiService.getAssetsByOnlineStatus(filter.online === 'true');
    } else {
      apiCall = ApiService.getAllAssets();
    }
    
    apiCall
      .then(response => {
        setAssets(response.data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load assets. Please try again later.');
        setLoading(false);
      });
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return 'N/A';
    const date = new Date(dateTimeStr);
    return date.toLocaleString();
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilter({
      ...filter,
      [name]: value
    });
  };

  const resetFilters = () => {
    setFilter({
      type: '',
      online: ''
    });
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Network Assets</h2>
        <Link to="/scans/create">
          <Button variant="primary">Scan for New Assets</Button>
        </Link>
      </div>
      
      {error && <Alert variant="danger">{error}</Alert>}
      
      <Card className="mb-4">
        <Card.Body>
          <h5>Filters</h5>
          <Row>
            <Col md={4}>
              <Form.Group className="mb-3">
                <Form.Label>Asset Type</Form.Label>
                <Form.Select
                  name="type"
                  value={filter.type}
                  onChange={handleFilterChange}
                >
                  <option value="">All Types</option>
                  <option value="WINDOWS">Windows</option>
                  <option value="LINUX">Linux</option>
                  <option value="MAC">Mac</option>
                  <option value="NETWORK_DEVICE">Network Device</option>
                  <option value="OTHER">Other</option>
                  <option value="UNKNOWN">Unknown</option>
                </Form.Select>
              </Form.Group>
            </Col>
            
            <Col md={4}>
              <Form.Group className="mb-3">
                <Form.Label>Status</Form.Label>
                <Form.Select
                  name="online"
                  value={filter.online}
                  onChange={handleFilterChange}
                >
                  <option value="">All Statuses</option>
                  <option value="true">Online</option>
                  <option value="false">Offline</option>
                </Form.Select>
              </Form.Group>
            </Col>
            
            <Col md={4} className="d-flex align-items-end">
              <Button variant="secondary" onClick={resetFilters} className="mb-3">
                Reset Filters
              </Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>
      
      {loading ? (
        <div className="text-center my-5">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Loading...</span>
          </Spinner>
        </div>
      ) : assets.length > 0 ? (
        <Card>
          <Card.Body>
            <Table responsive striped hover>
              <thead>
                <tr>
                  <th>IP Address</th>
                  <th>Hostname</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>First Discovered</th>
                  <th>Last Seen</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {assets.map(asset => (
                  <tr key={asset.id}>
                    <td>{asset.ipAddress}</td>
                    <td>{asset.hostname || 'Unknown'}</td>
                    <td>
                      <Badge bg="info">{asset.assetType}</Badge>
                    </td>
                    <td>
                      {asset.online ? (
                        <Badge bg="success">Online</Badge>
                      ) : (
                        <Badge bg="danger">Offline</Badge>
                      )}
                    </td>
                    <td>{formatDateTime(asset.firstDiscovered)}</td>
                    <td>{formatDateTime(asset.lastSeen)}</td>
                    <td>
                      <Button
                        as={Link}
                        to={`/assets/${asset.id}`}
                        variant="info"
                        size="sm"
                      >
                        Details
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
          No assets found. Use "Scan for New Assets" to discover devices on your network.
        </Alert>
      )}
    </div>
  );
};

export default AssetList;