import { useEffect, useCallback } from 'react';
import useStore from '../store/useStore';
import { WS_BASE_URL, MESSAGE_TYPES, CONNECTION_STATE } from '../utils/constants';

export const useWebSocket = () => {
  const { 
    session, 
    ws, 
    setWebSocket, 
    setConnectionState, 
    setRemotePeer,
    addLog 
  } = useStore();

  /**
   * Connect to WebSocket
   */
  const connect = useCallback(() => {
    if (!session.token) {
      addLog('Cannot connect: No token available', 'error');
      return;
    }

    if (ws && ws.readyState === WebSocket.OPEN) {
      addLog('Already connected to WebSocket', 'warning');
      return;
    }

    addLog('Connecting to WebSocket...', 'info');
    setConnectionState(CONNECTION_STATE.CONNECTING);

    const wsUrl = `${WS_BASE_URL}?token=${session.token}`;
    const websocket = new WebSocket(wsUrl);

    websocket.onopen = () => {
      addLog('✅ WebSocket connected!', 'success');
      setConnectionState(CONNECTION_STATE.CONNECTED);
      setWebSocket(websocket);
    };

    websocket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        addLog(`Received: ${message.type}`, 'info');
        handleMessage(message);
      } catch (error) {
        addLog(`Error parsing message: ${error.message}`, 'error');
      }
    };

    websocket.onerror = (error) => {
      addLog('WebSocket error', 'error');
      console.error('WebSocket error:', error);
      setConnectionState(CONNECTION_STATE.ERROR);
    };

    websocket.onclose = (event) => {
      addLog(`WebSocket closed: ${event.code} ${event.reason}`, 'warning');
      setConnectionState(CONNECTION_STATE.DISCONNECTED);
      setWebSocket(null);
      
      // ADDED: Clear remote peer on WebSocket close
      setRemotePeer(null);
    };

  }, [session.token, ws, setWebSocket, setConnectionState, setRemotePeer, addLog]);

  /**
   * Disconnect from WebSocket
   */
  const disconnect = useCallback(() => {
    if (ws) {
      addLog('Disconnecting WebSocket...', 'info');
      ws.close();
      setWebSocket(null);
      setConnectionState(CONNECTION_STATE.DISCONNECTED);
      setRemotePeer(null);
    }
  }, [ws, setWebSocket, setConnectionState, setRemotePeer, addLog]);

  /**
   * Send message via WebSocket
   */
  const sendMessage = useCallback((message) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      addLog('Cannot send message: WebSocket not connected', 'error');
      return false;
    }

    try {
      ws.send(JSON.stringify(message));
      addLog(`Sent: ${message.type}`, 'info');
      return true;
    } catch (error) {
      addLog(`Error sending message: ${error.message}`, 'error');
      return false;
    }
  }, [ws, addLog]);

  /**
   * Handle incoming WebSocket messages
   */
  const handleMessage = useCallback((message) => {
    switch (message.type) {
      case MESSAGE_TYPES.PEER_JOINED:
        addLog(`✅ Peer joined: ${message.senderId}`, 'success');
        addLog(`Device type: ${message.payload}`, 'info');
        
        // Set remote peer with all details
        setRemotePeer({
          peerId: message.senderId,
          deviceType: message.payload,
          sessionId: message.sessionId,
        });
        
        // Dispatch event for components to react
        window.dispatchEvent(new CustomEvent('peer-joined', { 
          detail: {
            peerId: message.senderId,
            deviceType: message.payload,
          }
        }));
        
        addLog('Remote peer set in store', 'success');
        break;

      case MESSAGE_TYPES.OFFER:
      case MESSAGE_TYPES.ANSWER:
      case MESSAGE_TYPES.ICE_CANDIDATE:
        // Forward to WebRTC handler
        window.dispatchEvent(new CustomEvent('webrtc-signal', { 
          detail: message 
        }));
        break;

      case MESSAGE_TYPES.PEER_DISCONNECTED:
        addLog(`⚠️ Peer disconnected: ${message.senderId}`, 'warning');
        
        // Clear remote peer
        setRemotePeer(null);
        
        // Dispatch disconnect event
        window.dispatchEvent(new CustomEvent('peer-disconnected', { 
          detail: message 
        }));
        break;

      case MESSAGE_TYPES.ERROR:
        addLog(`Error: ${message.message}`, 'error');
        break;

      default:
        addLog(`Unknown message type: ${message.type}`, 'warning');
    }
  }, [addLog, setRemotePeer]);

  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      if (ws) {
        ws.close();
      }
    };
  }, [ws]);

  return {
    connect,
    disconnect,
    sendMessage,
    isConnected: ws && ws.readyState === WebSocket.OPEN,
  };
};