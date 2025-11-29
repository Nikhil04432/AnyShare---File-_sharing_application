import React, { createContext, useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const navigate = useNavigate();

  // Check if user is authenticated on mount
  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
      
      // Set authorization header for all requests
      api.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
    }

    setLoading(false);
  }, []);

  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const { token, refreshToken, userId, email: userEmail, displayName } = response.data;

      // Store tokens and user info
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);
      
      const userData = { userId, email: userEmail, displayName };
      localStorage.setItem('user', JSON.stringify(userData));

      // Update state
      setToken(token);
      setUser(userData);

      // Set authorization header
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      return { success: true };
    } catch (error) {
      console.error('Login error:', error);
      return { 
        success: false, 
        error: error.response?.data?.errorMessage || 'Login failed' 
      };
    }
  };

  const register = async (email, password, displayName) => {
    try {
      const response = await api.post('/auth/register', { 
        email, 
        password, 
        displayName 
      });
      const { token, refreshToken, userId, email: userEmail, displayName: name } = response.data;

      // Store tokens and user info
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);
      
      const userData = { userId, email: userEmail, displayName: name };
      localStorage.setItem('user', JSON.stringify(userData));

      // Update state
      setToken(token);
      setUser(userData);

      // Set authorization header
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      return { success: true };
    } catch (error) {
      console.error('Registration error:', error);
      return { 
        success: false, 
        error: error.response?.data?.errorMessage || 'Registration failed' 
      };
    }
  };

  const logout = () => {
    // Clear storage
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');

    // Clear state
    setToken(null);
    setUser(null);

    // Remove authorization header
    delete api.defaults.headers.common['Authorization'];

    // Redirect to login
    navigate('/login');
  };

  const refreshToken = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      const response = await api.post('/auth/refresh', { refreshToken });
      const { token: newToken } = response.data;

      // Update stored token
      localStorage.setItem('token', newToken);
      setToken(newToken);

      // Update authorization header
      api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;

      return newToken;
    } catch (error) {
      console.error('Token refresh error:', error);
      logout(); // Force logout if refresh fails
      return null;
    }
  };

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!token,
    login,
    register,
    logout,
    refreshToken,
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};