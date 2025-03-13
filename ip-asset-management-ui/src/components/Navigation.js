import React from 'react';
import { Link } from 'react-router-dom';
import { Navbar, Nav, Container } from 'react-bootstrap';

const Navigation = ({ currentUser, logOut }) => {
  return (
    <Navbar bg="dark" variant="dark" expand="lg">
      <Container>
        <Navbar.Brand as={Link} to="/">IP Asset Management</Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link as={Link} to="/">Home</Nav.Link>
            {currentUser && (
              <>
                <Nav.Link as={Link} to="/assets">Assets</Nav.Link>
                <Nav.Link as={Link} to="/scans">Scan Jobs</Nav.Link>
              </>
            )}
          </Nav>
          
          <Nav>
            {currentUser ? (
              <>
                <Nav.Link as={Link} to="/profile">{currentUser.username}</Nav.Link>
                <Nav.Link onClick={logOut}>Logout</Nav.Link>
              </>
            ) : (
              <>
                <Nav.Link as={Link} to="/login">Login</Nav.Link>
                <Nav.Link as={Link} to="/register">Register</Nav.Link>
              </>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default Navigation;