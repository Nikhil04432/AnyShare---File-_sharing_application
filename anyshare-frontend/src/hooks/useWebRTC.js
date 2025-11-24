import { useEffect, useCallback, useRef } from 'react';
import useStore from '../store/useStore';
import { ICE_SERVERS, MESSAGE_TYPES } from '../utils/constants';

export const useWebRTC = (sendMessage) => {
  const {
    session,
    remotePeer,
    peerConnection,
    dataChannel,
    setPeerConnection,
    setDataChannel,
    addLog,
  } = useStore();

  const peerConnectionRef = useRef(null);

  /**
   * Initialize WebRTC Peer Connection
   */
  const initializePeerConnection = useCallback(() => {
    if (peerConnectionRef.current) {
      addLog('Peer connection already exists', 'warning');
      return peerConnectionRef.current;
    }

    addLog('Creating WebRTC peer connection...', 'info');

    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });

    // Handle ICE candidates
    pc.onicecandidate = (event) => {
      if (event.candidate && remotePeer) {
        addLog('Sending ICE candidate', 'info');
        sendMessage({
          type: MESSAGE_TYPES.ICE_CANDIDATE,
          targetId: remotePeer.peerId,
          payload: event.candidate,
        });
      }
    };

    // Monitor connection state
    pc.onconnectionstatechange = () => {
      addLog(`Connection state: ${pc.connectionState}`, 'info');
      
      if (pc.connectionState === 'connected') {
        addLog('WebRTC peer connection established!', 'success');
      } else if (pc.connectionState === 'disconnected') {
        addLog('WebRTC peer connection disconnected', 'warning');
      } else if (pc.connectionState === 'failed') {
        addLog('WebRTC peer connection failed', 'error');
      }
    };

    // Monitor ICE connection state
    pc.oniceconnectionstatechange = () => {
      addLog(`ICE connection state: ${pc.iceConnectionState}`, 'info');
    };

    peerConnectionRef.current = pc;
    setPeerConnection(pc);

    return pc;
  }, [remotePeer, sendMessage, setPeerConnection, addLog]);

  /**
   * Create WebRTC Offer (Sender side)
   */
  const createOffer = useCallback(async () => {
  try {
    addLog('=== Starting WebRTC Offer Creation ===', 'info');
    addLog(`Remote peer: ${JSON.stringify(remotePeer)}`, 'info');

    const pc = initializePeerConnection();

    // Create data channel for file transfer with optimized settings
    addLog('Creating data channel...', 'info');
    const dc = pc.createDataChannel('fileTransfer', {
      ordered: true,
      maxRetransmits: 0,  // Don't retransmit - we want speed over reliability for large files
      // OR use maxPacketLifeTime: 1000 for time-based reliability
    });

    setupDataChannel(dc);

    // Create offer
    addLog('Creating WebRTC offer...', 'info');
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);

    addLog(`Local description set. Type: ${offer.type}`, 'success');

    if (!remotePeer) {
      addLog('ERROR: No remote peer available to send offer!', 'error');
      return;
    }

    // Send offer via WebSocket
    addLog(`Sending OFFER to ${remotePeer.peerId}`, 'info');
    
    const success = sendMessage({
      type: MESSAGE_TYPES.OFFER,
      targetId: remotePeer.peerId,
      payload: pc.localDescription,
    });

    if (success) {
      addLog('OFFER sent successfully via WebSocket', 'success');
    } else {
      addLog('Failed to send OFFER via WebSocket', 'error');
    }

  } catch (error) {
    addLog(`Error creating offer: ${error.message}`, 'error');
    console.error('Full error:', error);
  }
}, [remotePeer, initializePeerConnection, sendMessage, addLog]);

  /**
   * Handle incoming WebRTC Offer (Receiver side)
   */
  const handleOffer = useCallback(async (offer, senderId) => {
    try {
      const pc = initializePeerConnection();

      // Handle incoming data channel
      pc.ondatachannel = (event) => {
        addLog('Data channel received', 'info');
        setupDataChannel(event.channel);
      };

      // Set remote description
      addLog('Setting remote description (offer)...', 'info');
      await pc.setRemoteDescription(new RTCSessionDescription(offer));

      // Create answer
      addLog('Creating answer...', 'info');
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);

      // Send answer via WebSocket
      addLog('Sending answer to sender...', 'info');
      sendMessage({
        type: MESSAGE_TYPES.ANSWER,
        targetId: senderId,
        payload: pc.localDescription,
      });
    } catch (error) {
      addLog(`Error handling offer: ${error.message}`, 'error');
    }
  }, [initializePeerConnection, sendMessage, addLog]);

  /**
   * Handle incoming WebRTC Answer (Sender side)
   */
  const handleAnswer = useCallback(async (answer) => {
    try {
      const pc = peerConnectionRef.current;
      
      if (!pc) {
        addLog('No peer connection to set answer', 'error');
        return;
      }

      addLog('Setting remote description (answer)...', 'info');
      await pc.setRemoteDescription(new RTCSessionDescription(answer));
      addLog('Remote description set successfully', 'success');
    } catch (error) {
      addLog(`Error handling answer: ${error.message}`, 'error');
    }
  }, [addLog]);

  /**
   * Handle incoming ICE Candidate
   */
  const handleIceCandidate = useCallback(async (candidate) => {
    try {
      const pc = peerConnectionRef.current;
      
      if (!pc) {
        addLog('No peer connection to add ICE candidate', 'error');
        return;
      }

      addLog('Adding ICE candidate...', 'info');
      await pc.addIceCandidate(new RTCIceCandidate(candidate));
    } catch (error) {
      addLog(`Error adding ICE candidate: ${error.message}`, 'error');
    }
  }, [addLog]);

  /**
   * Setup Data Channel event handlers
   */
  const setupDataChannel = useCallback((dc) => {
    dc.onopen = () => {
      addLog('Data channel opened - Ready for file transfer!', 'success');
      setDataChannel(dc);
    };

    dc.onclose = () => {
      addLog('Data channel closed', 'warning');
      setDataChannel(null);
    };

    dc.onerror = (error) => {
      addLog('Data channel error', 'error');
      console.error('Data channel error:', error);
    };

    // File transfer will be handled by useFileTransfer hook
    dc.onmessage = (event) => {
      window.dispatchEvent(new CustomEvent('data-channel-message', {
        detail: event.data,
      }));
    };

    setDataChannel(dc);
  }, [setDataChannel, addLog]);

  /**
   * Close WebRTC connection
   */
  const closeConnection = useCallback(() => {
    if (dataChannel) {
      dataChannel.close();
      setDataChannel(null);
    }

    if (peerConnectionRef.current) {
      peerConnectionRef.current.close();
      peerConnectionRef.current = null;
      setPeerConnection(null);
      addLog('WebRTC connection closed', 'info');
    }
  }, [dataChannel, setDataChannel, setPeerConnection, addLog]);

  /**
   * Listen for WebRTC signaling events
   */
  useEffect(() => {
    const handleWebRTCSignal = (event) => {
      const message = event.detail;

      switch (message.type) {
        case MESSAGE_TYPES.OFFER:
          handleOffer(message.payload, message.senderId);
          break;
        case MESSAGE_TYPES.ANSWER:
          handleAnswer(message.payload);
          break;
        case MESSAGE_TYPES.ICE_CANDIDATE:
          handleIceCandidate(message.payload);
          break;
        default:
          break;
      }
    };

    window.addEventListener('webrtc-signal', handleWebRTCSignal);

    return () => {
      window.removeEventListener('webrtc-signal', handleWebRTCSignal);
    };
  }, [handleOffer, handleAnswer, handleIceCandidate]);

  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      closeConnection();
    };
  }, []);

  return {
    createOffer,
    closeConnection,
    isDataChannelOpen: dataChannel && dataChannel.readyState === 'open',
  };
};