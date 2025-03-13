import React from 'react';
import { Card, ListGroup } from 'react-bootstrap';
import AuthService from '../services/auth.service';

const Profile = () => {
  const currentUser = AuthService.getCurrentUser();

  return (
    <div className="container">
      <h3 className="mb-4">Profile</h3>
      
      {currentUser ? (
        <Card>
          <Card.Header as="h5">User Information</Card.Header>
          <ListGroup variant="flush">
            <ListGroup.Item>
              <strong>Token:</strong>{' '}
              {currentUser.token.substring(0, 20)}...{' '}
              {currentUser.token.substring(currentUser.token.length - 20)}
            </ListGroup.Item>
            <ListGroup.Item>
              <strong>ID:</strong> {currentUser.id}
            </ListGroup.Item>
            <ListGroup.Item>
              <strong>Username:</strong> {currentUser.username}
            </ListGroup.Item>
            <ListGroup.Item>
              <strong>Email:</strong> {currentUser.email}
            </ListGroup.Item>
            <ListGroup.Item>
              <strong>Authorities:</strong>
              <ul className="mb-0">
                {currentUser.roles &&
                  currentUser.roles.map((role, index) => <li key={index}>{role}</li>)}
              </ul>
            </ListGroup.Item>
          </ListGroup>
        </Card>
      ) : (
        <Card>
          <Card.Body>
            <Card.Text>Please login to view your profile.</Card.Text>
          </Card.Body>
        </Card>
      )}
    </div>
  );
};

export default Profile;