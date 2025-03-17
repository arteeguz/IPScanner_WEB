import React, { useState, useEffect } from 'react';
import { Card, ProgressBar, Row, Col } from 'react-bootstrap';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';
import { Line } from 'react-chartjs-2';

// Register ChartJS components
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const ResourceMonitor = () => {
  const [resourceData, setResourceData] = useState({
    cpuUsage: 0,
    memoryUsage: 0,
    threadCount: 0,
    historyData: {
      labels: [],
      cpu: [],
      memory: []
    }
  });
  
  // Simulate resource data updating
  // In a real implementation, this would come from a WebSocket or polling API endpoint
  useEffect(() => {
    const simulateResourceUpdates = () => {
      // Simulated CPU usage between 10-90%
      const cpuUsage = Math.floor(Math.random() * 40) + 10;
      
      // Simulated memory usage between 20-70%
      const memoryUsage = Math.floor(Math.random() * 30) + 20;
      
      // Simulated thread count between 2-8
      const threadCount = Math.floor(Math.random() * 6) + 2;
      
      // Add to history data (keep last 10 data points)
      const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      
      setResourceData(prevState => {
        const newLabels = [...prevState.historyData.labels, currentTime].slice(-10);
        const newCpu = [...prevState.historyData.cpu, cpuUsage].slice(-10);
        const newMemory = [...prevState.historyData.memory, memoryUsage].slice(-10);
        
        return {
          cpuUsage,
          memoryUsage,
          threadCount,
          historyData: {
            labels: newLabels,
            cpu: newCpu,
            memory: newMemory
          }
        };
      });
    };
    
    // Simulate updates every 3 seconds
    const interval = setInterval(simulateResourceUpdates, 3000);
    simulateResourceUpdates(); // Initial call
    
    return () => clearInterval(interval);
  }, []);
  
  // Chart data
  const chartData = {
    labels: resourceData.historyData.labels,
    datasets: [
      {
        label: 'CPU Usage',
        data: resourceData.historyData.cpu,
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4
      },
      {
        label: 'Memory Usage',
        data: resourceData.historyData.memory,
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        tension: 0.4
      }
    ]
  };
  
  // Chart options
  const chartOptions = {
    responsive: true,
    scales: {
      y: {
        min: 0,
        max: 100,
        ticks: {
          callback: function(value) {
            return value + '%';
          }
        }
      }
    },
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: 'System Resource History'
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            return context.dataset.label + ': ' + context.parsed.y + '%';
          }
        }
      }
    }
  };
  
  return (
    <Card className="shadow-sm mb-4">
      <Card.Header className="bg-primary text-white">
        <h5 className="mb-0">System Resource Monitor</h5>
      </Card.Header>
      <Card.Body>
        <Row className="mb-4">
          <Col md={4}>
            <div className="text-center mb-3">
              <h6>CPU Usage</h6>
              <div className="d-flex justify-content-between mb-1">
                <small>Low</small>
                <small>{resourceData.cpuUsage}%</small>
                <small>High</small>
              </div>
              <ProgressBar 
                now={resourceData.cpuUsage} 
                variant={resourceData.cpuUsage > 70 ? 'danger' : resourceData.cpuUsage > 40 ? 'warning' : 'success'}
              />
            </div>
          </Col>
          
          <Col md={4}>
            <div className="text-center mb-3">
              <h6>Memory Usage</h6>
              <div className="d-flex justify-content-between mb-1">
                <small>Low</small>
                <small>{resourceData.memoryUsage}%</small>
                <small>High</small>
              </div>
              <ProgressBar 
                now={resourceData.memoryUsage} 
                variant={resourceData.memoryUsage > 80 ? 'danger' : resourceData.memoryUsage > 60 ? 'warning' : 'success'}
              />
            </div>
          </Col>
          
          <Col md={4}>
            <div className="text-center mb-3">
              <h6>Active Threads</h6>
              <div className="thread-indicator">
                <div className="d-flex justify-content-center">
                  {[...Array(resourceData.threadCount)].map((_, i) => (
                    <div key={i} className="thread-dot active mx-1"></div>
                  ))}
                  {[...Array(8 - resourceData.threadCount)].map((_, i) => (
                    <div key={i + resourceData.threadCount} className="thread-dot mx-1"></div>
                  ))}
                </div>
                <div className="mt-2">
                  <span className="badge bg-info">{resourceData.threadCount} active threads</span>
                </div>
              </div>
            </div>
          </Col>
        </Row>
        
        <div className="chart-container" style={{ height: '250px' }}>
          <Line data={chartData} options={chartOptions} />
        </div>
        
        <div className="mt-3 text-center text-muted small">
          <p className="mb-0">The system automatically adjusts resource usage based on your computer's capabilities</p>
        </div>
      </Card.Body>
    </Card>
  );
};

export default ResourceMonitor;