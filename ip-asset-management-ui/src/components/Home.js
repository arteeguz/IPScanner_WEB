import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Card, Row, Col, Button, Spinner, Container } from 'react-bootstrap';
import { Pie, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title } from 'chart.js';
import ApiService from '../services/api.service';
import AuthService from '../services/auth.service';
import ResourceMonitor from './ResourceMonitor';

// Register ChartJS components
ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title);

const Home = () => {
  const [stats, setStats] = useState({
    totalAssets: 0,
    onlineAssets: 0,
    scanJobs: 0,
    assetTypes: {}
  });
  const [loading, setLoading] = useState(true);
  const [recentScans, setRecentScans] = useState([]);
  const currentUser = AuthService.getCurrentUser();

  useEffect(() => {
    if (currentUser) {
      loadData();
    } else {
      setLoading(false);
    }
  }, []);

  const loadData = () => {
    setLoading(true);
    
    // Load assets count and types
    Promise.all([
      ApiService.getAllAssets(),
      ApiService.getAssetsByOnlineStatus(true),
      ApiService.getAllScanJobs()
    ])
      .then(([allAssets, onlineAssets, scanJobs]) => {
        // Calculate asset type distribution
        const assetTypes = {};
        allAssets.data.forEach(asset => {
          const type = asset.assetType || 'UNKNOWN';
          assetTypes[type] = (assetTypes[type] || 0) + 1;
        });
        
        setStats({
          totalAssets: allAssets.data.length,
          onlineAssets: onlineAssets.data.length,
          scanJobs: scanJobs.data.length,
          assetTypes
        });
        
        // Get 5 most recent scan jobs
        const sortedJobs = scanJobs.data.sort((a, b) => 
          new Date(b.lastRunAt || b.createdAt) - new Date(a.lastRunAt || a.createdAt)
        ).slice(0, 5);
        
        setRecentScans(sortedJobs);
        setLoading(false);
      })
      .catch(err => {
        console.error("Error loading stats:", err);
        setLoading(false);
      });
  };
  
  // Prepare chart data
  const assetTypeData = {
    labels: Object.keys(stats.assetTypes).map(key => key.replace('_', ' ')),
    datasets: [
      {
        data: Object.values(stats.assetTypes),
        backgroundColor: [
          'rgba(255, 99, 132, 0.6)',
          'rgba(54, 162, 235, 0.6)',
          'rgba(255, 206, 86, 0.6)',
          'rgba(75, 192, 192, 0.6)',
          'rgba(153, 102, 255, 0.6)',
          'rgba(255, 159, 64, 0.6)',
        ],
        borderWidth: 1,
      },
    ],
  };
  
  const onlineStatusData = {
    labels: ['Online', 'Offline'],
    datasets: [
      {
        label: 'Asset Status',
        data: [stats.onlineAssets, stats.totalAssets - stats.onlineAssets],
        backgroundColor: [
          'rgba(40, 167, 69, 0.6)',
          'rgba(220, 53, 69, 0.6)',
        ],
      },
    ],
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
    <Container>
      <div className="text-center mb-5">
        <h1>IP Asset Management System</h1>
        <p className="lead">Discover, monitor, and manage network assets in your organization</p>
      </div>
      
      {currentUser ? (
        <>
          <Row className="mb-4">
            <Col md={4}>
              <Card className="text-center h-100 shadow-sm dashboard-card">
                <Card.Body>
                  <i className="bi bi-hdd-network text-primary" style={{ fontSize: '2rem' }}></i>
                  <Card.Title className="mt-2">Total Assets</Card.Title>
                  <h1>{stats.totalAssets}</h1>
                  <Button as={Link} to="/assets" variant="primary">View Assets</Button>
                </Card.Body>
              </Card>
            </Col>
            
            <Col md={4}>
              <Card className="text-center h-100 shadow-sm dashboard-card">
                <Card.Body>
                  <i className="bi bi-wifi text-success" style={{ fontSize: '2rem' }}></i>
                  <Card.Title className="mt-2">Online Assets</Card.Title>
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
              <Card className="text-center h-100 shadow-sm dashboard-card">
                <Card.Body>
                  <i className="bi bi-search text-info" style={{ fontSize: '2rem' }}></i>
                  <Card.Title className="mt-2">Scan Jobs</Card.Title>
                  <h1>{stats.scanJobs}</h1>
                  <Button as={Link} to="/scans" variant="info">View Scan Jobs</Button>
                </Card.Body>
              </Card>
            </Col>
          </Row>
          
          <Row className="mb-4">
            <Col md={6}>
              <Card className="h-100 shadow-sm">
                <Card.Header className="bg-primary text-white">
                  <h5 className="mb-0">Asset Type Distribution</h5>
                </Card.Header>
                <Card.Body className="d-flex align-items-center justify-content-center">
                  {Object.keys(stats.assetTypes).length > 0 ? (
                    <div style={{ height: '250px', width: '100%' }}>
                      <Pie data={assetTypeData} options={{ maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }} />
                    </div>
                  ) : (
                    <div className="text-center text-muted">
                      <p>No asset data available</p>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
            
            <Col md={6}>
              <Card className="h-100 shadow-sm">
                <Card.Header className="bg-success text-white">
                  <h5 className="mb-0">Online Status</h5>
                </Card.Header>
                <Card.Body className="d-flex align-items-center justify-content-center">
                  {stats.totalAssets > 0 ? (
                    <div style={{ height: '250px', width: '100%' }}>
                      <Bar 
                        data={onlineStatusData}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          scales: {
                            y: {
                              beginAtZero: true,
                              title: {
                                display: true,
                                text: 'Number of Assets'
                              }
                            }
                          }
                        }}
                      />
                    </div>
                  ) : (
                    <div className="text-center text-muted">
                      <p>No status data available</p>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          </Row>
          
          <Row className="mb-4">
            <Col md={7}>
              <Card className="shadow-sm">
                <Card.Header className="bg-info text-white">
                  <h5 className="mb-0">Recent Scans</h5>
                </Card.Header>
                <div className="list-group list-group-flush">
                  {recentScans.length > 0 ? (
                    recentScans.map(scan => (
                      <Link 
                        to={`/scans/${scan.id}/results`} 
                        className="list-group-item list-group-item-action" 
                        key={scan.id}
                      >
                        <div className="d-flex w-100 justify-content-between">
                          <h6 className="mb-1">{scan.name}</h6>
                          <span className={`badge bg-${
                            scan.status === 'COMPLETED' ? 'success' : 
                            scan.status === 'RUNNING' ? 'primary' : 
                            scan.status === 'FAILED' ? 'danger' : 'secondary'
                          }`}>
                            {scan.status}
                          </span>
                        </div>
                        <p className="mb-1">{scan.description || 'No description'}</p>
                        <small>
                          {scan.lastRunAt ? 
                            `Last run: ${new Date(scan.lastRunAt).toLocaleString()}` : 
                            `Created: ${new Date(scan.createdAt).toLocaleString()}`
                          }
                        </small>
                      </Link>
                    ))
                  ) : (
                    <div className="list-group-item text-center text-muted">
                      <p>No scan jobs have been created yet</p>
                    </div>
                  )}
                </div>
                <Card.Footer>
                  <Button as={Link} to="/scans" variant="outline-info" className="w-100">
                    View All Scan Jobs
                  </Button>
                </Card.Footer>
              </Card>
            </Col>
            
            <Col md={5}>
              <ResourceMonitor />
            </Col>
          </Row>
          
          <Card className="shadow-sm mb-4">
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
            <Card className="shadow-sm">
              <Card.Body className="text-center">
                <i className="bi bi-person-check" style={{ fontSize: '3rem', color: '#0d6efd' }}></i>
                <h3 className="mt-3">Already have an account?</h3>
                <p>Log in to access your IP asset management dashboard</p>
                <Button as={Link} to="/login" variant="primary" size="lg">
                  Login
                </Button>
              </Card.Body>
            </Card>
          </Col>
          
          <Col md={6}>
            <Card className="shadow-sm">
              <Card.Body className="text-center">
                <i className="bi bi-person-plus" style={{ fontSize: '3rem', color: '#198754' }}></i>
                <h3 className="mt-3">New User?</h3>
                <p>Create an account to start managing your network assets</p>
                <Button as={Link} to="/register" variant="success" size="lg">
                  Register
                </Button>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </Container>
  );
};

export default Home;