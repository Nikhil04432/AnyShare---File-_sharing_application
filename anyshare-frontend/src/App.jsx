import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Sender from './pages/Sender';
import Receiver from './pages/Receiver';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Home - accessible to both guest and authenticated users */}
          <Route path="/" element={<Home />} />
          
          {/* Protected Routes - require authentication */}
          <Route 
            path="/sender" 
            element={
              <ProtectedRoute>
                <Sender />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/receiver" 
            element={
              <ProtectedRoute>
                <Receiver />
              </ProtectedRoute>
            } 
          />
          
          {/* Catch all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;