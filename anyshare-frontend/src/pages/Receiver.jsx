import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Wifi, Loader2, CheckCircle } from 'lucide-react';
import useStore from '../store/useStore';
import { useWebSocket } from '../hooks/useWebSocket';
import { useWebRTC } from '../hooks/useWebRTC';

import SessionJoiner from '../components/Receiver/SessionJoiner';
import FileReceiver from '../components/Receiver/FileReceiver';
import StatusIndicator from '../components/Common/StatusIndicator';
import LogViewer from '../components/Common/LogViewer';

const Receiver = () => {
  const navigate = useNavigate();
  const { session, connectionState, reset } = useStore();
  const { connect, disconnect, sendMessage, isConnected } = useWebSocket();
  const { isDataChannelOpen } = useWebRTC(sendMessage);

  // Auto-connect WebSocket when token is available
  useEffect(() => {
    if (session.token && !isConnected) {
      connect();
    }
  }, [session.token, isConnected, connect]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect();
      reset();
    };
  }, []);

  const handleBack = () => {
    if (window.confirm('Are you sure you want to leave?')) {
      disconnect();
      reset();
      navigate('/');
    }
  };

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
          <h1 className="text-3xl font-bold text-white">📥 Receiver</h1>
          <div className="w-20" /> {/* Spacer */}
        </div>

        {/* Main Card */}
        <div className="card space-y-8">
          {/* Step 1: Join Session */}
          {!session.token && <SessionJoiner />}

          {/* Step 2: Connection Status */}
            {session.token && (
              <div className="space-y-4">
                <h3 className="text-xl font-semibold text-gray-800 mb-4">
                  Step 2: Connecting to Sender
                </h3>
                
                {/* WebSocket Status */}
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-700 font-medium">Signaling Server:</span>
                    <StatusIndicator state={connectionState} />
                  </div>
                </div>

                {/* Establishing P2P */}
                {isConnected && !isDataChannelOpen && (
                  <div className="text-center py-8 bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl border-2 border-blue-200">
                    <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Wifi className="w-8 h-8 text-blue-600 animate-pulse" />
                    </div>
                    <p className="text-blue-800 font-semibold text-lg">
                      Connecting to sender...
                    </p>
                    <p className="text-blue-600 text-sm mt-2">
                      Establishing secure peer-to-peer connection
                    </p>
                  </div>
                )}

                {/* P2P Connected - Ready! */}
                {isDataChannelOpen && (
                  <div className="text-center py-8 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl border-2 border-green-300">
                    <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                      <CheckCircle className="w-8 h-8 text-green-600" />
                    </div>
                    <p className="text-green-800 font-semibold text-lg">
                      ✓ Connected & Ready!
                    </p>
                    <p className="text-green-600 text-sm mt-2">
                      Waiting for sender to send files
                    </p>
                  </div>
                )}
              </div>
            )}

          {/* Step 3: File Transfer */}
          {isDataChannelOpen && <FileReceiver />}

          {/* Logs */}
          {session.token && <LogViewer />}
        </div>
      </div>
    </div>
  );
};

export default Receiver;