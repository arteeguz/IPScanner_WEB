import React from 'react';
import { Card, Row, Col, ListGroup, Badge } from 'react-bootstrap';

const SystemInfoDisplay = ({ assetInfo }) => {
  if (!assetInfo || !assetInfo.additionalInfo) {
    return null;
  }
  
  const { additionalInfo } = assetInfo;
  
  // Get the CPU information
  const cpuInfo = {
    model: additionalInfo.cpuModel || 'Unknown',
    cores: additionalInfo.cpuCores || 'Unknown',
    threads: additionalInfo.cpuThreads || 'Unknown'
  };
  
  // Get the RAM information
  const ramInfo = additionalInfo.ramSize || 'Unknown';
  
  // Get the OS information
  const osInfo = {
    name: assetInfo.operatingSystem || 'Unknown',
    version: assetInfo.osVersion || 'Unknown',
    architecture: additionalInfo.osArchitecture || 'Unknown'
  };
  
  // Get the GPU information
  const gpuInfo = additionalInfo.gpuName || 'Unknown';
  
  // Get the last logged user
  const lastUser = additionalInfo.lastLoggedUser || 'Unknown';
  
  // Get network information
  const openPorts = additionalInfo.openPorts || {};
  
  return (
    <div className="system-info-display">
      <Row className="mb-4">
        <Col md={6}>
          <Card className="h-100 shadow-sm">
            <Card.Header className="bg-primary text-white">
              <h5 className="mb-0">CPU Information</h5>
            </Card.Header>
            <ListGroup variant="flush">
              <ListGroup.Item>
                <strong>Model:</strong> {cpuInfo.model}
              </ListGroup.Item>
              <ListGroup.Item>
                <strong>Cores:</strong> {cpuInfo.cores}
              </ListGroup.Item>
              <ListGroup.Item>
                <strong>Threads:</strong> {cpuInfo.threads}
              </ListGroup.Item>
            </ListGroup>
          </Card>
        </Col>
        
        <Col md={6}>
          <Card className="h-100 shadow-sm">
            <Card.Header className="bg-success text-white">
              <h5 className="mb-0">Memory Information</h5>
            </Card.Header>
            <Card.Body className="d-flex align-items-center justify-content-center">
              <div className="text-center">
                <h2>{ramInfo}</h2>
                <p className="mb-0 text-muted">Total RAM</p>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
      
      <Row className="mb-4">
        <Col md={6}>
          <Card className="h-100 shadow-sm">
            <Card.Header className="bg-info text-white">
              <h5 className="mb-0">Operating System</h5>
            </Card.Header>
            <ListGroup variant="flush">
              <ListGroup.Item>
                <strong>Name:</strong> {osInfo.name}
              </ListGroup.Item>
              <ListGroup.Item>
                <strong>Version:</strong> {osInfo.version}
              </ListGroup.Item>
              <ListGroup.Item>
                <strong>Architecture:</strong> {osInfo.architecture}
              </ListGroup.Item>
            </ListGroup>
          </Card>
        </Col>
        
        <Col md={6}>
          <Card className="h-100 shadow-sm">
            <Card.Header className="bg-warning text-dark">
              <h5 className="mb-0">User & Graphics</h5>
            </Card.Header>
            <ListGroup variant="flush">
              <ListGroup.Item>
                <strong>Last Logged User:</strong> {lastUser}
              </ListGroup.Item>
              <ListGroup.Item>
                <strong>Graphics:</strong> {gpuInfo}
              </ListGroup.Item>
            </ListGroup>
          </Card>
        </Col>
      </Row>
      
      <Card className="shadow-sm mb-4">
        <Card.Header className="bg-dark text-white">
          <h5 className="mb-0">Network Information</h5>
        </Card.Header>
        <Card.Body>
          <h6>Open Ports:</h6>
          <div className="d-flex flex-wrap gap-2 mt-2">
            {Object.entries(openPorts).map(([portName, isOpen]) => (
              isOpen ? (
                <Badge key={portName} bg="success" className="p-2">
                  {portName}
                </Badge>
              ) : null
            ))}
            {Object.values(openPorts).filter(Boolean).length === 0 && (
              <span className="text-muted">No open ports detected</span>
            )}
          </div>
        </Card.Body>
      </Card>
    </div>
  );
};

export default SystemInfoDisplay;