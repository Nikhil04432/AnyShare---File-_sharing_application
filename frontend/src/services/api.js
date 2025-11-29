import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';

// Create axios instance with defaults
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds
});

// Request interceptor (for adding auth tokens if needed)
api.interceptors.request.use(
  (config) => {
    // Add timestamp to prevent caching
    config.params = {
      ...config.params,
      _t: Date.now(),
    };
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor (for error handling)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Server responded with error
      const errorMessage = error.response.data?.errorMessage || error.message;
      console.error('API Error:', errorMessage);
      throw new Error(errorMessage);
    } else if (error.request) {
      // Request made but no response
      console.error('Network Error:', error.message);
      throw new Error('Network error. Please check your connection.');
    } else {
      // Something else happened
      console.error('Error:', error.message);
      throw error;
    }
  }
);

/**
 * Create a new session
 */
export const createSession = async (deviceType = 'DESKTOP') => {
  const response = await api.post('/sessions', {
    deviceType,
    userAgent: navigator.userAgent,
  });
  return response.data;
};

/**
 * Get session information
 */
export const getSessionInfo = async (roomCode) => {
  const response = await api.get(`/sessions/${roomCode}`);
  return response.data;
};

/**
 * Join an existing session
 */
export const joinSession = async (roomCode, deviceType = 'DESKTOP') => {
  const response = await api.post(`/sessions/${roomCode}/join`, {
    deviceType,
    userAgent: navigator.userAgent,
  });
  return response.data;
};

/**
 * Close a session
 */
export const closeSession = async (sessionId, token) => {
  const response = await api.delete(`/sessions/${sessionId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return response.data;
};

export default api;