import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Button, Card, Alert, Row, Col } from 'react-bootstrap';
import ApiService from '../services/api.service';

const CreateScanJob = () => {
  const [scanJob, setScanJob] = useState({
    name: '',
    description: '',
    ipAddresses: '',
    ipSegments: '',
    recurring: false,
    schedule: '',
    settings: [
      { name: 'Ping scan', enabled: true },
      { name: 'OS detection', enabled: false }
    ]
  });
  
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState(false);
  
  const navigate = useNavigate();

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setScanJob({
      ...scanJob,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSettingChange = (index, checked) => {
    const updatedSettings = [...scanJob.settings];
    updatedSettings[index].enabled = checked;
    setScanJob({
      ...scanJob,
      settings: updatedSettings
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    setMessage('');
    setError(false);
    setLoading(true);

    // Parse IP addresses and segments
    const ipAddressesArray = scanJob.ipAddresses
      ? scanJob.ipAddresses.split(',').map(ip => ip.trim())
      : [];
    
    const ipSegmentsArray = scanJob.ipSegments
      ? scanJob.ipSegments.split(',').map(segment => segment.trim())
      : [];

    const scanJobData = {
      name: scanJob.name,
      description: scanJob.description,
      ipAddresses: ipAddressesArray,
      ipSegments: ipSegmentsArray,
      recurring: scanJob.recurring,
      schedule: scanJob.schedule,
      settings: scanJob.settings
    };

    ApiService.createScanJob(scanJobData)
      .then(response => {
        setLoading(false);
        setMessage('Scan job created successfully!');
        
        // Navigate to scan jobs list after a delay
        setTimeout(() => {
          navigate('/scans');
        }, 1500);
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

  return (
    <div>
      <h2 className="text-center mb-4">Create New Scan Job</h2>
      
      <Card>
        <Card.Body>
          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Job Name</Form.Label>
              <Form.Control
                type="text"
                name="name"
                value={scanJob.name}
                onChange={handleInputChange}
                required
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                name="description"
                value={scanJob.description}
                onChange={handleInputChange}
              />
            </Form.Group>

            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>IP Addresses (comma separated)</Form.Label>
                  <Form.Control
                    type="text"
                    name="ipAddresses"
                    value={scanJob.ipAddresses}
                    onChange={handleInputChange}
                    placeholder="192.168.1.1, 10.0.0.5"
                  />
                  <Form.Text className="text-muted">
                    Enter individual IP addresses separated by commas
                  </Form.Text>
                </Form.Group>
              </Col>
              
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>IP Segments (comma separated)</Form.Label>
                  <Form.Control
                    type="text"
                    name="ipSegments"
                    value={scanJob.ipSegments}
                    onChange={handleInputChange}
                    placeholder="192.168.1.0/24, 10.0.0.1-10"
                  />
                  <Form.Text className="text-muted">
                    Enter IP ranges in CIDR notation (e.g., 192.168.1.0/24) or ranges (e.g., 10.0.0.1-10)
                  </Form.Text>
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-3">
              <Form.Check
                type="checkbox"
                name="recurring"
                label="Schedule recurring scan"
                checked={scanJob.recurring}
                onChange={handleInputChange}
              />
            </Form.Group>

            {scanJob.recurring && (
              <Form.Group className="mb-3">
                <Form.Label>Schedule (simple for demo)</Form.Label>
                <Form.Select
                  name="schedule"
                  value={scanJob.schedule}
                  onChange={handleInputChange}
                >
                  <option value="">Select schedule</option>
                  <option value="daily">Daily</option>
                  <option value="weekly">Weekly</option>
                  <option value="monthly">Monthly</option>
                </Form.Select>
              </Form.Group>
            )}

            <Form.Group className="mb-3">
              <Form.Label>Scan Settings</Form.Label>
              {scanJob.settings.map((setting, index) => (
                <Form.Check
                  key={index}
                  type="checkbox"
                  label={setting.name}
                  checked={setting.enabled}
                  onChange={(e) => handleSettingChange(index, e.target.checked)}
                  className="mb-2"
                />
              ))}
            </Form.Group>

            <div className="d-grid gap-2">
              <Button variant="primary" type="submit" disabled={loading}>
                {loading ? 'Creating...' : 'Create Scan Job'}
              </Button>
            </div>
          </Form>

          {message && (
            <Alert variant={error ? 'danger' : 'success'} className="mt-3">
              {message}
            </Alert>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default CreateScanJob;