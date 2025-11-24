import React from 'react';
import useStore from '../../store/useStore';
import { formatBytes } from '../../utils/helpers';
import { CheckCircle, XCircle, Loader2 } from 'lucide-react';

const TransferProgress = () => {
  const { fileTransfer } = useStore();

  if (fileTransfer.status === 'idle') return null;

  const { metadata, progress, bytesTransferred, speed, status } = fileTransfer;

  const getStatusIcon = () => {
    switch (status) {
      case 'complete':
        return <CheckCircle className="w-6 h-6 text-green-600" />;
      case 'error':
        return <XCircle className="w-6 h-6 text-red-600" />;
      case 'sending':
      case 'receiving':
        return <Loader2 className="w-6 h-6 text-primary-600 animate-spin" />;
      default:
        return null;
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'sending':
        return 'Sending...';
      case 'receiving':
        return 'Receiving...';
      case 'complete':
        return 'Transfer Complete!';
      case 'error':
        return 'Transfer Failed';
      default:
        return '';
    }
  };

  return (
    <div className="mt-6 bg-gray-50 rounded-xl p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          {getStatusIcon()}
          <div>
            <p className="font-semibold text-gray-800">{getStatusText()}</p>
            {metadata && (
              <p className="text-sm text-gray-600">{metadata.name}</p>
            )}
          </div>
        </div>
        {speed && status !== 'complete' && (
          <span className="text-sm font-medium text-primary-600">{speed}</span>
        )}
      </div>

      {metadata && (
        <div className="space-y-2">
          <div className="flex justify-between text-sm text-gray-600">
            <span>{formatBytes(bytesTransferred)} / {formatBytes(metadata.size)}</span>
            <span>{progress}%</span>
          </div>
          
          <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-300 ${
                status === 'complete' ? 'bg-green-500' : 
                status === 'error' ? 'bg-red-500' : 
                'bg-gradient-to-r from-primary-500 to-secondary-500'
              }`}
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}

      {status === 'complete' && (
        <div className="text-center text-green-600 font-medium">
          ✓ File transferred successfully!
        </div>
      )}

      {status === 'error' && (
        <div className="text-center text-red-600 font-medium">
          ✗ Transfer failed. Please try again.
        </div>
      )}
    </div>
  );
};

export default TransferProgress;