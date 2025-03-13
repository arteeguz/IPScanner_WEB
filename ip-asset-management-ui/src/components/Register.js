import React, { useState } from 'react';
import { Form, Button, Card, Alert } from 'react-bootstrap';
import AuthService from '../services/auth.service';

const Register = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [successful, setSuccessful] = useState(false);
  const [message, setMessage] = useState('');

  const handleRegister = (e) => {
    e.preventDefault();
    
    setMessage('');
    setSuccessful(false);

    if (!username || !email || !password) {
      setMessage('Please fill in all fields');
      return;
    }

    AuthService.register(username, email, password)
      .then(response => {
        setMessage(response.data.message);
        setSuccessful(true);
      })
      .catch(error => {
        const resMessage = 
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();
        
        setMessage(resMessage);
        setSuccessful(false);
      });
  };

  return (
    <div className="col-md-6 offset-md-3">
      <Card className="card-container">
        <Card.Body>
          <h3 className="text-center mb-4">Register</h3>
          
          {!successful && (
            <Form onSubmit={handleRegister}>
              <Form.Group className="mb-3">
                <Form.Label>Username</Form.Label>
                <Form.Control
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  minLength="3"
                  maxLength="20"
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Email</Form.Label>
                <Form.Control
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
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
                  minLength="6"
                  maxLength="40"
                />
              </Form.Group>

              <div className="d-grid gap-2">
                <Button variant="primary" type="submit">
                  Register
                </Button>
              </div>
            </Form>
          )}

          {message && (
            <Alert variant={successful ? 'success' : 'danger'} className="mt-3">
              {message}
            </Alert>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default Register;