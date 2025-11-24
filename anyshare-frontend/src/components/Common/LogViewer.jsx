import React, { useRef, useEffect } from 'react';
import useStore from '../../store/useStore';
import { Terminal, Trash2 } from 'lucide-react';

const LogViewer = () => {
  const { logs, clearLogs } = useStore();
  const logEndRef = useRef(null);

  // Auto-scroll to bottom when new log added
  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  const getLogColor = (type) => {
    switch (type) {
      case 'success':
        return 'text-green-600';
      case 'error':
        return 'text-red-600';
      case 'warning':
        return 'text-yellow-600';
      default:
        return 'text-blue-600';
    }
  };

  return (
    <div className="mt-6">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <Terminal className="w-5 h-5 text-gray-600" />
          <h3 className="text-lg font-semibold text-gray-800">Logs</h3>
        </div>
        <button
          onClick={clearLogs}
          className="flex items-center gap-1 text-sm text-gray-600 hover:text-red-600 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
          Clear
        </button>
      </div>
      
      <div className="bg-gray-900 rounded-lg p-4 h-64 overflow-y-auto font-mono text-sm">
        {logs.length === 0 ? (
          <div className="text-gray-500 text-center mt-20">No logs yet...</div>
        ) : (
          logs.map((log) => (
            <div key={log.id} className="mb-1">
              <span className="text-gray-500">[{log.time}]</span>{' '}
              <span className={getLogColor(log.type)}>[{log.type.toUpperCase()}]</span>{' '}
              <span className="text-gray-300">{log.message}</span>
            </div>
          ))
        )}
        <div ref={logEndRef} />
      </div>
    </div>
  );
};

export default LogViewer;