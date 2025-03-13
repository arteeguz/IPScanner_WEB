import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Card, Row, Col, Button, Spinner } from 'react-bootstrap';
import ApiService from '../services/api.service';
import AuthService from '../services/auth.service';

const Home = () => {
  const [stats, setStats] = useState({
    totalAssets: 0,
    onlineAssets: 0,
    scanJobs: 0
  });
  const [loading, setLoading] = useState(true);
  const currentUser = AuthService.getCurrentUser();

  useEffect(() => {
    if (currentUser) {
      loadStats();
    } else {
      setLoading(false);
    }
  }, []);

  const loadStats = () => {
    setLoading(true);
    
    // Load assets count
    Promise.all([
      ApiService.getAllAssets(),
      ApiService.getAssetsByOnlineStatus(true),
      ApiService.getAllScanJobs()
    ])
      .then(([allAssets, onlineAssets, scanJobs]) => {
        setStats({
          totalAssets: allAssets.data.length,
          onlineAssets: onlineAssets.data.length,
          scanJobs: scanJobs.data.length
        });
        setLoading(false);
      })
      .catch(err => {
        console.error("Error loading stats:", err);
        setLoading(false);
      });
  };

  if (loading && currentUser) {
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
      <div className="text-center mb-5">
        <h1>IP Asset Management System</h1>
        <p className="lead">Discover, monitor, and manage network assets in your organization</p>
      </div>
      
      {currentUser ? (
        <>
          <Row className="mb-4">
            <Col md={4}>
              <Card className="text-center h-100">
                <Card.Body>
                  <Card.Title>Total Assets</Card.Title>
                  <h1>{stats.totalAssets}</h1>
                  <Button as={Link} to="/assets" variant="primary">View Assets</Button>
                </Card.Body>
              </Card>
            </Col>
            
            <Col md={4}>
              <Card className="text-center h-100">
                <Card.Body>
                  <Card.Title>Online Assets</Card.Title>
                  <h1>{stats.onlineAssets}</h1>
                  <Button 
                    as={Link} 
                    to="/assets" 
                    variant="success"
                  >
                    View Online Assets
                  </Button>
                </Card.Body>
              </Card>
            </Col>
            
            <Col md={4}>
              <Card className="text-center h-100">
                <Card.Body>
                  <Card.Title>Scan Jobs</Card.Title>
                  <h1>{stats.scanJobs}</h1>
                  <Button as={Link} to="/scans" variant="info">View Scan Jobs</Button>
                </Card.Body>
              </Card>
            </Col>
          </Row>
          
          <Card>
            <Card.Body className="text-center">
              <h3>Start a New Network Scan</h3>
              <p>Discover new assets on your network or update existing ones</p>
              <Button as={Link} to="/scans/create" variant="primary" size="lg">
                Create Scan Job
              </Button>
            </Card.Body>
          </Card>
        </>
      ) : (
        <Row>
          <Col md={6} className="mb-4">
            <Card>
              <Card.Body className="text-center">
                <h3>Already have an account?</h3>
                <p>Log in to access your IP asset management dashboard</p>
                <Button as={Link} to="/login" variant="primary" size="lg">
                  Login
                </Button>
              </Card.Body>
            </Card>
          </Col>
          
          <Col md={6}>
            <Card>
              <Card.Body className="text-center">
                <h3>New User?</h3>
                <p>Create an account to start managing your network assets</p>
                <Button as={Link} to="/register" variant="success" size="lg">
                  Register
                </Button>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
};

export default Home;