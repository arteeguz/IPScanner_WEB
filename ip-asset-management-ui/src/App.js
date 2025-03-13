import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

// Components
import Login from './components/Login';
import Register from './components/Register';
import Home from './components/Home';
import Profile from './components/Profile';
import Navigation from './components/Navigation';
import AssetList from './components/AssetList';
import AssetDetail from './components/AssetDetail';
import ScanJobs from './components/ScanJobs';
import CreateScanJob from './components/CreateScanJob';
import ScanResults from './components/ScanResults';

// Services
import AuthService from './services/auth.service';

function App() {
  const [currentUser, setCurrentUser] = useState(undefined);
  
  useEffect(() => {
    const user = AuthService.getCurrentUser();
    if (user) {
      setCurrentUser(user);
    }
  }, []);

  const logOut = () => {
    AuthService.logout();
    setCurrentUser(undefined);
  };

  return (
    <Router>
      <div>
        <Navigation currentUser={currentUser} logOut={logOut} />
        <div className="container mt-3">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route 
              path="/profile" 
              element={currentUser ? <Profile /> : <Navigate to="/login" />} 
            />
            <Route 
              path="/assets" 
              element={currentUser ? <AssetList /> : <Navigate to="/login" />} 
            />
            <Route 
              path="/assets/:id" 
              element={currentUser ? <AssetDetail /> : <Navigate to="/login" />} 
            />
            <Route 
              path="/scans" 
              element={currentUser ? <ScanJobs /> : <Navigate to="/login" />} 
            />
            <Route 
              path="/scans/create" 
              element={currentUser ? <CreateScanJob /> : <Navigate to="/login" />} 
            />
            <Route 
              path="/scans/:id/results" 
              element={currentUser ? <ScanResults /> : <Navigate to="/login" />} 
            />
          </Routes>
        </div>
      </div>
    </Router>
  );
}

export default App;