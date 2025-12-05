import React, { useState } from 'react';
import { Loader2, Plus } from 'lucide-react';
import { createSession } from '../../services/api';
import useStore from '../../store/useStore';

const SessionCreator = () => {
  const [loading, setLoading] = useState(false);
  const { session, setSession, reset, addLog } = useStore();

  const handleCreateSession = async () => {
    // If there's already a session, reset first
    if (session.sessionId) {
      addLog('Resetting previous session...', 'warning');
      reset();
      await new Promise(resolve => setTimeout(resolve, 500));
    }

    setLoading(true);
    addLog('Creating new session...', 'info');

    try {
      // Step 1: Create session
      const createData = await createSession();
      addLog(`✅ Session created: ${createData.sessionId}`, 'success');

      setSession({
        sessionId: createData.sessionId,
        roomCode: createData.roomCode,
        expiresAt: createData.expiresAt,
      });

      // Step 2: Sender joins their own session
      addLog('Joining session as sender...', 'info');

      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
      const joinResponse = await fetch(`${apiBaseUrl}/sessions/${createData.roomCode}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          deviceType: 'DESKTOP',
          userAgent: navigator.userAgent,
        }),
      });

      if (!joinResponse.ok) {
        const errorData = await joinResponse.json();
        throw new Error(errorData.errorMessage || 'Failed to join session');
      }

      const joinData = await joinResponse.json();
      addLog(`✅ Joined as ${joinData.peerId}`, 'success');

      // Step 3: Update session with peer info and token
      setSession({
        peerId: joinData.peerId,
        token: joinData.token,
      });

    } catch (error) {
      addLog(`❌ Error: ${error.message}`, 'error');
      console.error('Session creation error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <h3 className="text-xl font-semibold text-gray-800 mb-4">Step 1: Create Session</h3>
      
      <button
        onClick={handleCreateSession}
        disabled={loading}
        className="btn-primary w-full flex items-center justify-center gap-2"
      >
        {loading ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            {session.sessionId ? 'Creating New Session...' : 'Creating...'}
          </>
        ) : (
          <>
            <Plus className="w-5 h-5" />
            {session.sessionId ? 'Create New Session' : 'Create Session'}
          </>
        )}
      </button>

      {session.sessionId && (
        <p className="text-sm text-gray-600 text-center bg-yellow-50 py-2 px-4 rounded-lg border border-yellow-200">
          💡 Creating a new session will reset the current one
        </p>
      )}
    </div>
  );
};

export default SessionCreator;