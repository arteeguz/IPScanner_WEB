import axios from 'axios';
import AuthService from './auth.service';

// Intercept all requests
axios.interceptors.request.use(
  config => {
    // Get user from localStorage
    const user = JSON.parse(localStorage.getItem('user'));
    
    // If token exists, add to headers
    if (user && user.token) {
      config.headers['Authorization'] = 'Bearer ' + user.token;
    }
    
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Intercept responses to handle 401 errors
axios.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    const originalRequest = error.config;
    
    // If receiving 401 and not already trying to refresh
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      // Force logout and redirect to login
      console.log('Session expired. Redirecting to login...');
      AuthService.logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }
    
    return Promise.reject(error);
  }
);

export default {
  setup() {
    // Just a placeholder function to import this file
    console.log('Token interceptor set up');
  }
};