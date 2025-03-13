import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Button, Card, Alert } from 'react-bootstrap';
import AuthService from '../services/auth.service';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  
  const navigate = useNavigate();

  const handleLogin = (e) => {
    e.preventDefault();
    
    setMessage('');
    setLoading(true);

    if (!username || !password) {
      setMessage('Please fill in all fields');
      setLoading(false);
      return;
    }

    AuthService.login(username, password)
      .then(() => {
        navigate('/profile');
        window.location.reload();
      })
      .catch(error => {
        const resMessage = 
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();
        
        setLoading(false);
        setMessage(resMessage);
      });
  };

  return (
    <div className="col-md-6 offset-md-3">
      <Card className="card-container">
        <Card.Body>
          <h3 className="text-center mb-4">Login</h3>
          
          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-3">
              <Form.Label>Username</Form.Label>
              <Form.Control
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </Form.Group>

            <div className="d-grid gap-2">
              <Button variant="primary" type="submit" disabled={loading}>
                {loading ? 'Loading...' : 'Login'}
              </Button>
            </div>
          </Form>

          {message && (
            <Alert variant="danger" className="mt-3">
              {message}
            </Alert>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default Login;