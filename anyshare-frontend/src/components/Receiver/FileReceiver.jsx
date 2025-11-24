import React from 'react';
import useStore from '../../store/useStore';
import { formatBytes } from '../../utils/helpers';
import { Download, Loader2, CheckCircle } from 'lucide-react';
import { useFileTransfer } from '../../hooks/useFileTransfer';

const FileReceiver = () => {
  const { fileTransfer } = useStore();
  const { downloadFile } = useFileTransfer();

  if (fileTransfer.status === 'idle') {
    return (
      <div className="mt-6 text-center py-12 bg-gray-50 rounded-xl">
        <Download className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <p className="text-gray-600 font-medium">Waiting for file...</p>
        <p className="text-sm text-gray-500 mt-2">
          File will appear here when sender starts transfer
        </p>
      </div>
    );
  }

  const { metadata, progress, bytesTransferred, speed, status } = fileTransfer;

  return (
    <div className="space-y-4">
      <h3 className="text-xl font-semibold text-gray-800 mb-4">
        Step 3: Receive File
      </h3>

      <div className="bg-gradient-to-br from-primary-50 to-purple-50 rounded-xl p-6 space-y-4">
        {/* File Info */}
        {metadata && (
          <div className="bg-white rounded-lg p-4">
            <div className="flex items-start justify-between mb-2">
              <div>
                <p className="font-semibold text-gray-800 text-lg">{metadata.name}</p>
                <p className="text-sm text-gray-600 mt-1">
                  {formatBytes(metadata.size)}
                  {metadata.mimeType && ` • ${metadata.mimeType.split('/')[0]}`}
                </p>
              </div>
              {status === 'complete' && (
                <CheckCircle className="w-6 h-6 text-green-600" />
              )}
              {status === 'receiving' && (
                <Loader2 className="w-6 h-6 text-primary-600 animate-spin" />
              )}
            </div>

            {/* Progress Bar */}
            <div className="space-y-2 mt-4">
              <div className="flex justify-between text-sm text-gray-600">
                <span>{formatBytes(bytesTransferred)}</span>
                <div className="flex items-center gap-3">
                  {speed && status === 'receiving' && (
                    <span className="text-primary-600 font-medium">{speed}</span>
                  )}
                  <span className="font-semibold">{progress}%</span>
                </div>
              </div>

              <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-300 ${
                    status === 'complete' 
                      ? 'bg-green-500' 
                      : 'bg-gradient-to-r from-primary-500 to-secondary-500'
                  }`}
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Status Messages */}
        {status === 'receiving' && (
          <div className="text-center py-4">
            <p className="text-primary-700 font-medium">Receiving file...</p>
            <p className="text-sm text-gray-600 mt-1">Please wait while the file transfers</p>
          </div>
        )}

        {status === 'complete' && (
          <div className="text-center py-4">
            <CheckCircle className="w-12 h-12 text-green-600 mx-auto mb-3" />
            <p className="text-green-700 font-semibold text-lg">File received successfully!</p>
            <p className="text-sm text-gray-600 mt-1">Click below to download</p>
          </div>
        )}

        {/* Download Button */}
        {status === 'complete' && (
          <button
            onClick={downloadFile}
            className="btn-primary w-full flex items-center justify-center gap-2"
          >
            <Download className="w-5 h-5" />
            Download File
          </button>
        )}
      </div>
    </div>
  );
};

export default FileReceiver;