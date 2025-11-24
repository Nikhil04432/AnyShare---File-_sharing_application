import React, { useState } from 'react';
import { Loader2, LogIn, CheckCircle } from 'lucide-react';
import { joinSession } from '../../services/api';
import useStore from '../../store/useStore';
import { isValidRoomCode } from '../../utils/helpers';

const SessionJoiner = () => {
  const [roomCode, setRoomCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [step, setStep] = useState(1); // Track progress
  const { setSession, addLog } = useStore();

  const handleJoinSession = async () => {
    setError('');

    // Validate room code format
    if (!isValidRoomCode(roomCode)) {
      setError('Invalid room code format (e.g., SWIFT-7284)');
      return;
    }

    setLoading(true);
    setStep(1);
    addLog(`Joining session: ${roomCode}`, 'info');

    try {
      // Step 1: Join session via REST API
      const data = await joinSession(roomCode);
      
      addLog(`✓ Session joined as ${data.peerId}`, 'success');
      setStep(2);
      
      setSession({
        sessionId: data.sessionId,
        roomCode: roomCode,
        peerId: data.peerId,
        token: data.token,
        expiresAt: data.expiresAt,
      });

      // Step 2: WebSocket will auto-connect via useEffect
      addLog('Connecting to signaling server...', 'info');

    } catch (error) {
      const errorMsg = error.message || 'Failed to join session';
      addLog(`Error joining session: ${errorMsg}`, 'error');
      setError(errorMsg);
      setStep(1);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleJoinSession();
    }
  };

  return (
    <div className="space-y-4">
      <h3 className="text-xl font-semibold text-gray-800 mb-4">
        Step 1: Enter Room Code
      </h3>

      <div className="space-y-3">
        <input
          type="text"
          value={roomCode}
          onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
          onKeyPress={handleKeyPress}
          placeholder="e.g., SWIFT-7284"
          className="input-field text-center text-2xl font-bold tracking-wider"
          disabled={loading}
        />

        {error && (
          <div className="text-red-600 text-sm text-center bg-red-50 py-2 px-4 rounded-lg border border-red-200">
            {error}
          </div>
        )}

        {/* Progress Indicator */}
        {loading && (
          <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
            <div className="space-y-3">
              {/* Step 1: Joining Session */}
              <div className="flex items-center gap-3">
                {step >= 1 ? (
                  step === 1 ? (
                    <Loader2 className="w-5 h-5 text-blue-600 animate-spin" />
                  ) : (
                    <CheckCircle className="w-5 h-5 text-green-600" />
                  )
                ) : (
                  <div className="w-5 h-5 rounded-full border-2 border-gray-300" />
                )}
                <span className={step >= 1 ? 'text-blue-800 font-medium' : 'text-gray-500'}>
                  Joining session...
                </span>
              </div>

              {/* Step 2: Connecting WebSocket */}
              {step >= 2 && (
                <div className="flex items-center gap-3">
                  <Loader2 className="w-5 h-5 text-blue-600 animate-spin" />
                  <span className="text-blue-800 font-medium">
                    Connecting to signaling server...
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        <button
          onClick={handleJoinSession}
          disabled={!roomCode || loading}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <Loader2 className="w-5 h-5 animate-spin" />
              Joining...
            </>
          ) : (
            <>
              <LogIn className="w-5 h-5" />
              Join Session
            </>
          )}
        </button>
      </div>

      <div className="text-sm text-gray-600 text-center bg-gray-50 p-4 rounded-lg">
        <p className="font-medium mb-1">💡 Tip:</p>
        <p>Ask the sender for their room code or scan the QR code</p>
      </div>
    </div>
  );
};

export default SessionJoiner;