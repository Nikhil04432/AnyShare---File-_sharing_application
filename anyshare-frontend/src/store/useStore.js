import { create } from 'zustand';
import { CONNECTION_STATE } from '../utils/constants';

const useStore = create((set, get) => ({
  // Session state
  session: {
    sessionId: null,
    roomCode: null,
    peerId: null,
    token: null,
    expiresAt: null,
  },
  
  // Connection state
  connectionState: CONNECTION_STATE.DISCONNECTED,
  
  // Peer information
  remotePeer: null,
  
  // WebSocket instance
  ws: null,
  
  // WebRTC instances
  peerConnection: null,
  dataChannel: null,
  
  // File transfer state
  fileTransfer: {
    file: null,
    metadata: null,
    progress: 0,
    speed: 0,
    startTime: null,
    bytesTransferred: 0,
    status: 'idle', // idle, sending, receiving, complete, error
  },
  
  // Logs
  logs: [],
  
  // Actions
  setSession: (session) => set({ session: { ...get().session, ...session } }),
  
  setConnectionState: (state) => set({ connectionState: state }),
  
  setRemotePeer: (peer) => set({ remotePeer: peer }),
  
  setWebSocket: (ws) => set({ ws }),
  
  setPeerConnection: (pc) => set({ peerConnection: pc }),
  
  setDataChannel: (dc) => set({ dataChannel: dc }),
  
  setFileTransfer: (data) => set({ 
    fileTransfer: { ...get().fileTransfer, ...data } 
  }),
  
  addLog: (message, type = 'info') => {
    const log = {
      id: Date.now(),
      time: new Date().toLocaleTimeString(),
      message,
      type,
    };
    set({ logs: [...get().logs, log] });
  },
  
  clearLogs: () => set({ logs: [] }),
  
  // Reset entire state
  reset: () => set({
    session: {
      sessionId: null,
      roomCode: null,
      peerId: null,
      token: null,
      expiresAt: null,
    },
    connectionState: CONNECTION_STATE.DISCONNECTED,
    remotePeer: null,
    ws: null,
    peerConnection: null,
    dataChannel: null,
    fileTransfer: {
      file: null,
      metadata: null,
      progress: 0,
      speed: 0,
      startTime: null,
      bytesTransferred: 0,
      status: 'idle',
    },
    logs: [],
  }),
}));

export default useStore;