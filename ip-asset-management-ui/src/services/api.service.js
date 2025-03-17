import axios from 'axios';
import authHeader from './auth-header';

const API_URL = 'http://localhost:8080/api/';

class ApiService {
  // Assets
  getAllAssets() {
    return axios.get(API_URL + 'assets', { headers: authHeader() });
  }

  getAssetById(id) {
    return axios.get(API_URL + `assets/${id}`, { headers: authHeader() });
  }

  getAssetsByType(type) {
    return axios.get(API_URL + `assets/type/${type}`, { headers: authHeader() });
  }

  getAssetsByOnlineStatus(status) {
    return axios.get(API_URL + `assets/online/${status}`, { headers: authHeader() });
  }

  // Scan Jobs
  getAllScanJobs() {
    return axios.get(API_URL + 'scan/jobs', { headers: authHeader() });
  }

  getScanJobById(id) {
    return axios.get(API_URL + `scan/jobs/${id}`, { headers: authHeader() });
  }

  createScanJob(scanJobData) {
    return axios.post(API_URL + 'scan/create', scanJobData, { headers: authHeader() });
  }

  runScanJob(id) {
    return axios.post(API_URL + `scan/run/${id}`, {}, { headers: authHeader() });
  }

  deleteScanJob(id) {
    return axios.delete(API_URL + `scan/jobs/${id}`, { headers: authHeader() });
  }

  // Scan Results
  getScanResults(jobId) {
    return axios.get(API_URL + `scan/results/${jobId}`, { headers: authHeader() });
  }
  
  // System monitoring (placeholder - would be implemented on the backend)
  getSystemResources() {
    return axios.get(API_URL + 'system/resources', { headers: authHeader() });
  }
}

export default new ApiService();