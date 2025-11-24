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
      reset(); // Clear everything
      
      // Small delay to ensure cleanup completes
      await new Promise(resolve => setTimeout(resolve, 500));
    }

    setLoading(true);
    addLog('Creating new session...', 'info');

    try {
      const data = await createSession();
      
      addLog(`Session created: ${data.sessionId}`, 'success');
      
      setSession({
        sessionId: data.sessionId,
        roomCode: data.roomCode,
        expiresAt: data.expiresAt,
      });

      // Now sender needs to join their own session
      const joinResponse = await fetch(`http://localhost:8080/api/v1/sessions/${data.roomCode}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          deviceType: 'DESKTOP',
          userAgent: navigator.userAgent,
        }),
      });

      const joinData = await joinResponse.json();
      
      addLog(`Joined as ${joinData.peerId}`, 'success');
      
      setSession({
        peerId: joinData.peerId,
        token: joinData.token,
      });

    } catch (error) {
      addLog(`Error creating session: ${error.message}`, 'error');
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