import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Wifi, Loader2, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import useStore from '../store/useStore';
import { useWebSocket } from '../hooks/useWebSocket';
import { useWebRTC } from '../hooks/useWebRTC';

import SessionCreator from '../components/Sender/SessionCreator';
import QRCodeDisplay from '../components/Sender/QRCodeDisplay';
import FileSelector from '../components/Sender/FileSelector';
import TransferProgress from '../components/Sender/TransferProgress';
import StatusIndicator from '../components/Common/StatusIndicator';
import LogViewer from '../components/Common/LogViewer';

const Sender = () => {
  const navigate = useNavigate();
  const { session, connectionState, remotePeer, dataChannel, peerConnection, reset, addLog } = useStore();
  const { connect, disconnect, sendMessage, isConnected } = useWebSocket();
  const { createOffer, isDataChannelOpen } = useWebRTC(sendMessage);
  
  const offerCreatedRef = useRef(false);

  // Auto-connect WebSocket when token is available
  useEffect(() => {
    if (session.token && !isConnected) {
      connect();
    }
  }, [session.token, isConnected, connect]);

  // Create offer when remote peer joins
  useEffect(() => {
    if (remotePeer && !offerCreatedRef.current) {
      addLog('Remote peer detected, creating WebRTC offer...', 'info');
      offerCreatedRef.current = true;
      
      // Small delay to ensure everything is ready
      setTimeout(() => {
        createOffer();
      }, 1000);
    }
  }, [remotePeer, createOffer, addLog]);

  // FIXED: Reset offer flag when remote peer disconnects
  useEffect(() => {
    if (!remotePeer) {
      offerCreatedRef.current = false;
    }
  }, [remotePeer]);

  // Listen for peer joined event
  useEffect(() => {
    const handlePeerJoined = (event) => {
      addLog('Peer joined event received!', 'success');
    };

    window.addEventListener('peer-joined', handlePeerJoined);

    return () => {
      window.removeEventListener('peer-joined', handlePeerJoined);
    };
  }, [addLog]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect();
      reset();
    };
  }, []);

  const handleBack = () => {
    if (window.confirm('Are you sure you want to leave? This will close the session.')) {
      disconnect();
      reset();
      navigate('/');
    }
  };

  // FIXED: Determine connection status
  const isPeerConnected = peerConnection && peerConnection.connectionState === 'connected';
  const isPeerDisconnected = peerConnection && 
    (peerConnection.connectionState === 'disconnected' || 
     peerConnection.connectionState === 'failed');

  return (
    <div className="min-h-screen p-4 md:p-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <button
            onClick={handleBack}
            className="flex items-center gap-2 text-white hover:text-white/80 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span className="font-medium">Back</span>
          </button>
          <h1 className="text-3xl font-bold text-white">📤 Sender</h1>
          <div className="w-20" /> {/* Spacer for centering */}
        </div>

        {/* Main Card */}
        <div className="card space-y-8">
          {/* Step 1: Create Session */}
          <SessionCreator />

          {/* Show QR Code after session created */}
          {session.roomCode && <QRCodeDisplay />}

          {/* Step 2: Connection Status */}
          {session.token && (
            <div className="space-y-4">
              <h3 className="text-xl font-semibold text-gray-800 mb-4">
                Step 2: Wait for Receiver
              </h3>
              
              {/* WebSocket Status */}
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="flex items-center justify-between">
                  <span className="text-gray-700 font-medium">Signaling Server:</span>
                  <StatusIndicator state={connectionState} />
                </div>
              </div>

              {/* Waiting for Receiver */}
              {isConnected && !remotePeer && (
                <div className="text-center py-8 bg-gradient-to-br from-yellow-50 to-orange-50 rounded-xl border-2 border-yellow-200">
                  <div className="w-16 h-16 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <Loader2 className="w-8 h-8 text-yellow-600 animate-spin" />
                  </div>
                  <p className="text-yellow-800 font-semibold text-lg">
                    Waiting for receiver to join...
                  </p>
                  <p className="text-yellow-600 text-sm mt-2">
                    Share the room code above with the receiver
                  </p>
                </div>
              )}

              {/* Receiver Joined - Establishing P2P */}
              {remotePeer && !isDataChannelOpen && !isPeerDisconnected && (
                <div className="text-center py-8 bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl border-2 border-blue-200">
                  <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <Wifi className="w-8 h-8 text-blue-600 animate-pulse" />
                  </div>
                  <p className="text-blue-800 font-semibold text-lg">
                    Receiver joined! Connecting...
                  </p>
                  <p className="text-blue-600 text-sm mt-2">
                    Establishing peer-to-peer connection
                  </p>
                </div>
              )}

              {/* P2P Connected - Ready! */}
              {remotePeer && isPeerConnected && isDataChannelOpen && (
                <div className="text-center py-8 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl border-2 border-green-300">
                  <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <CheckCircle className="w-8 h-8 text-green-600" />
                  </div>
                  <p className="text-green-800 font-semibold text-lg">
                    ✅ Connected & Ready!
                  </p>
                  <p className="text-green-600 text-sm mt-2">
                    You can now send files securely
                  </p>
                </div>
              )}

              {/* FIXED: Connection Lost/Disconnected Warning */}
              {remotePeer && isPeerDisconnected && (
                <div className="text-center py-8 bg-gradient-to-br from-red-50 to-pink-50 rounded-xl border-2 border-red-300">
                  <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <XCircle className="w-8 h-8 text-red-600" />
                  </div>
                  <p className="text-red-800 font-semibold text-lg">
                    ⚠️ Receiver Disconnected
                  </p>
                  <p className="text-red-600 text-sm mt-2 mb-4">
                    The receiver has disconnected or connection failed
                  </p>
                  
                  <div className="flex gap-3 justify-center">
                    <button
                      onClick={() => {
                        addLog('Waiting for receiver to reconnect...', 'info');
                      }}
                      className="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg font-semibold transition-all"
                    >
                      Wait for Reconnection
                    </button>
                    <button
                      onClick={() => {
                        if (window.confirm('Start over with a new session?')) {
                          disconnect();
                          reset();
                          navigate('/');
                        }
                      }}
                      className="px-6 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg font-semibold transition-all"
                    >
                      Start Over
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Step 3: File Transfer */}
          {remotePeer && (
            <div>
              <FileSelector />
              <TransferProgress />
            </div>
          )}

          {/* Logs */}
          <LogViewer />
        </div>
      </div>
    </div>
  );
};

export default Sender;