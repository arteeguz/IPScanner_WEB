import React, { useState } from 'react';
import { Form, Button, Card, Row, Col, InputGroup, OverlayTrigger, Tooltip } from 'react-bootstrap';

const AdvancedScanForm = ({ onSubmit, loading }) => {
  const [scanJob, setScanJob] = useState({
    name: '',
    description: '',
    ipAddresses: '',
    ipSegments: '',
    recurring: false,
    schedule: '',
    settings: [
      { name: 'Ping scan', enabled: true },
      { name: 'OS detection', enabled: true },
      { name: 'Service detection', enabled: true },
      { name: 'Hardware info collection', enabled: true },
      { name: 'Software inventory', enabled: false } // More resource intensive
    ]
  });
  
  const [validated, setValidated] = useState(false);

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
    
    const form = e.currentTarget;
    if (form.checkValidity() === false) {
      e.stopPropagation();
      setValidated(true);
      return;
    }
    
    // Parse IP addresses and segments
    const ipAddressesArray = scanJob.ipAddresses
      ? scanJob.ipAddresses.split(',').map(ip => ip.trim())
      : [];
    
    const ipSegmentsArray = scanJob.ipSegments
      ? scanJob.ipSegments.split(',').map(segment => segment.trim())
      : [];
    
    // Validate that at least one IP or segment is provided
    if (ipAddressesArray.length === 0 && ipSegmentsArray.length === 0) {
      alert('Please enter at least one IP address or IP segment.');
      return;
    }

    const scanJobData = {
      name: scanJob.name,
      description: scanJob.description,
      ipAddresses: ipAddressesArray,
      ipSegments: ipSegmentsArray,
      recurring: scanJob.recurring,
      schedule: scanJob.schedule,
      settings: scanJob.settings
    };

    onSubmit(scanJobData);
  };
  
  // Function to estimate scan duration based on input
  const estimateScanDuration = () => {
    let totalIPs = 0;
    
    // Count individual IPs
    if (scanJob.ipAddresses) {
      totalIPs += scanJob.ipAddresses.split(',').length;
    }
    
    // Estimate IPs from segments
    if (scanJob.ipSegments) {
      const segments = scanJob.ipSegments.split(',');
      segments.forEach(segment => {
        if (segment.includes('/24')) {
          totalIPs += 254; // A typical /24 network
        } else if (segment.includes('/16')) {
          totalIPs += 100; // Just an estimate for UI purposes
        } else if (segment.includes('-')) {
          // Range like 192.168.1.1-10
          const parts = segment.split('-');
          if (parts.length === 2) {
            const rangeEnd = parseInt(parts[1]);
            const rangeStart = parseInt(parts[0].split('.').pop());
            if (!isNaN(rangeStart) && !isNaN(rangeEnd)) {
              totalIPs += (rangeEnd - rangeStart) + 1;
            } else {
              totalIPs += 10; // Default estimate
            }
          } else {
            totalIPs += 1;
          }
        } else {
          totalIPs += 1; // Single IP segment
        }
      });
    }
    
    // Calculate time estimate (very rough)
    const enabledSettings = scanJob.settings.filter(s => s.enabled).length;
    const baseTimePerIP = 2; // Base seconds per IP
    const settingMultiplier = 1 + (enabledSettings * 0.2); // More settings = more time
    
    const totalTimeSeconds = totalIPs * baseTimePerIP * settingMultiplier;
    
    if (totalTimeSeconds < 60) {
      return `~${Math.ceil(totalTimeSeconds)} seconds`;
    } else if (totalTimeSeconds < 3600) {
      return `~${Math.ceil(totalTimeSeconds / 60)} minutes`;
    } else {
      const hours = Math.floor(totalTimeSeconds / 3600);
      const minutes = Math.ceil((totalTimeSeconds % 3600) / 60);
      return `~${hours} hour${hours > 1 ? 's' : ''} ${minutes > 0 ? `${minutes} minute${minutes > 1 ? 's' : ''}` : ''}`;
    }
  };

  return (
    <Card className="shadow-sm">
      <Card.Header className="bg-primary text-white">
        <h5 className="mb-0">Configure New Scan</h5>
      </Card.Header>
      <Card.Body>
        <Form noValidate validated={validated} onSubmit={handleSubmit}>
          <Row>
            <Col md={6}>
              <Form.Group className="mb-3">
                <Form.Label>Scan Name</Form.Label>
                <Form.Control
                  type="text"
                  name="name"
                  value={scanJob.name}
                  onChange={handleInputChange}
                  placeholder="e.g., Weekly Network Scan"
                  required
                />
                <Form.Control.Feedback type="invalid">
                  Please provide a name for this scan.
                </Form.Control.Feedback>
              </Form.Group>
            </Col>
            
            <Col md={6}>
              <Form.Group className="mb-3">
                <Form.Label>Description</Form.Label>
                <Form.Control
                  type="text"
                  name="description"
                  value={scanJob.description}
                  onChange={handleInputChange}
                  placeholder="Short description for this scan job"
                />
              </Form.Group>
            </Col>
          </Row>

          <Row className="mb-3">
            <Col md={6}>
              <Form.Group>
                <Form.Label>
                  IP Addresses
                  <OverlayTrigger
                    placement="top"
                    overlay={<Tooltip>Enter individual IP addresses separated by commas</Tooltip>}
                  >
                    <span className="ms-1 text-info">ⓘ</span>
                  </OverlayTrigger>
                </Form.Label>
                <Form.Control
                  type="text"
                  name="ipAddresses"
                  value={scanJob.ipAddresses}
                  onChange={handleInputChange}
                  placeholder="e.g., 192.168.1.1, 10.0.0.5"
                />
                <Form.Text className="text-muted">
                  Individual addresses, comma separated
                </Form.Text>
              </Form.Group>
            </Col>
            
            <Col md={6}>
              <Form.Group>
                <Form.Label>
                  IP Segments
                  <OverlayTrigger
                    placement="top"
                    overlay={<Tooltip>Enter IP ranges in CIDR notation or ranges</Tooltip>}
                  >
                    <span className="ms-1 text-info">ⓘ</span>
                  </OverlayTrigger>
                </Form.Label>
                <Form.Control
                  type="text"
                  name="ipSegments"
                  value={scanJob.ipSegments}
                  onChange={handleInputChange}
                  placeholder="e.g., 192.168.1.0/24, 10.0.0.1-10"
                />
                <Form.Text className="text-muted">
                  CIDR notation or ranges (e.g., 192.168.1.0/24)
                </Form.Text>
              </Form.Group>
            </Col>
          </Row>

          <div className="mb-4">
            <label className="form-label">Scan Settings</label>
            <Row>
              {scanJob.settings.map((setting, index) => (
                <Col md={6} key={index}>
                  <Form.Check
                    type="switch"
                    id={`setting-${index}`}
                    label={setting.name}
                    checked={setting.enabled}
                    onChange={(e) => handleSettingChange(index, e.target.checked)}
                    className="mb-2"
                  />
                </Col>
              ))}
            </Row>
          </div>

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
              <Form.Label>Schedule</Form.Label>
              <Form.Select
                name="schedule"
                value={scanJob.schedule}
                onChange={handleInputChange}
                required={scanJob.recurring}
              >
                <option value="">Select schedule</option>
                <option value="daily">Daily</option>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
              </Form.Select>
              <Form.Control.Feedback type="invalid">
                Please select a schedule for recurring scans.
              </Form.Control.Feedback>
            </Form.Group>
          )}
          
          <Card className="bg-light mb-4">
            <Card.Body>
              <h6 className="card-title">Scan Estimate</h6>
              <Row>
                <Col xs={6}>
                  <div className="d-flex align-items-center">
                    <div className="me-2">
                      <i className="bi bi-clock text-primary"></i>
                    </div>
                    <div>
                      <div className="small text-muted">Estimated Time</div>
                      <div>{estimateScanDuration()}</div>
                    </div>
                  </div>
                </Col>
                <Col xs={6}>
                  <div className="d-flex align-items-center">
                    <div className="me-2">
                      <i className="bi bi-cpu text-primary"></i>
                    </div>
                    <div>
                      <div className="small text-muted">Resource Usage</div>
                      <div>
                        {scanJob.settings.filter(s => s.enabled).length <= 2 ? 'Low' : 
                         scanJob.settings.filter(s => s.enabled).length <= 4 ? 'Medium' : 'High'}
                      </div>
                    </div>
                  </div>
                </Col>
              </Row>
            </Card.Body>
          </Card>

          <div className="d-grid gap-2">
            <Button 
              variant="primary" 
              type="submit" 
              disabled={loading}
              className="btn-lg"
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Creating Scan...
                </>
              ) : (
                'Start Scan'
              )}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
};

export default AdvancedScanForm;