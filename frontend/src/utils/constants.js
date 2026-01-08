// API Configuration
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/signal';

// File Transfer Configuration
// Adaptive chunk size - larger chunks = fewer operations = faster
export const CHUNK_SIZE = 64 * 1024; // Increase from 64KB to 256KB

// For very fast networks, you can go even higher:
//export const CHUNK_SIZE = 1024 * 1024; // 1MB chunks

// WebRTC Configuration
// NEW (STUN + TURN - works everywhere!)
export const ICE_SERVERS = [
  // STUN servers (for NAT discovery)
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },

  // FREE TURN servers (for relay when P2P fails)
  {
    urls: 'turn:openrelay.metered.ca:80',
    username: 'openrelayproject',
    credential: 'openrelayproject'
  },
  {
    urls: 'turn:openrelay.metered.ca:443',
    username: 'openrelayproject',
    credential: 'openrelayproject'
  },
  {
    urls: 'turn:openrelay.metered.ca:443?transport=tcp',
    username: 'openrelayproject',
    credential: 'openrelayproject'
  }
];

// Session Configuration
export const SESSION_EXPIRY_MINUTES = 5;

// Message Types
export const MESSAGE_TYPES = {
  OFFER: 'OFFER',
  ANSWER: 'ANSWER',
  ICE_CANDIDATE: 'ICE_CANDIDATE',
  PEER_JOINED: 'PEER_JOINED',
  PEER_DISCONNECTED: 'PEER_DISCONNECTED',
  ERROR: 'ERROR',
  FILE_METADATA: 'FILE_METADATA',
  FILE_COMPLETE: 'FILE_COMPLETE',
};

// Connection States
export const CONNECTION_STATE = {
  DISCONNECTED: 'disconnected',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  ERROR: 'error',
};