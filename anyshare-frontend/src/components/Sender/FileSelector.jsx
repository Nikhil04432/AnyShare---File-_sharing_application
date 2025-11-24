import React, { useRef, useState } from 'react';
import { Upload, FileIcon, X, FolderOpen, Loader2 } from 'lucide-react';
import useStore from '../../store/useStore';
import { formatBytes } from '../../utils/helpers';
import { useFileTransfer } from '../../hooks/useFileTransfer';


const FileSelector = () => {
  const { dataChannel, fileTransfer, setFileTransfer } = useStore();
  const { sendFile } = useFileTransfer();
  const fileInputRef = useRef(null);
  const [dragActive, setDragActive] = useState(false);

  const isDataChannelOpen = dataChannel && dataChannel.readyState === 'open';
  const isSending = fileTransfer.status === 'sending';

  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (file) {
      setFileTransfer({ file, status: 'idle' });
    }
  };

  const handleSendFile = () => {
    if (fileTransfer.file) {
      sendFile(fileTransfer.file);
    }
  };

  const clearFile = () => {
    setFileTransfer({ file: null, status: 'idle' });
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  // Drag and drop handlers
  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      setFileTransfer({ file, status: 'idle' });
    }
  };

  return (
    <div className="space-y-4">
      <h3 className="text-xl font-semibold text-gray-800 mb-4">
        Step 3: Select & Send File
      </h3>

      <div className="space-y-4">
        {/* Hidden file input */}
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileSelect}
          disabled={!isDataChannelOpen}
          className="hidden"
          id="file-input"
        />

        {/* File Upload Area with Drag & Drop */}
        {!fileTransfer.file ? (
          <div
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            className={`relative border-2 border-dashed rounded-xl p-8 transition-all
              ${!isDataChannelOpen 
                ? 'border-gray-300 bg-gray-50 cursor-not-allowed' 
                : dragActive
                  ? 'border-primary-500 bg-primary-50 scale-105'
                  : 'border-primary-300 bg-primary-50 hover:bg-primary-100 hover:border-primary-400'
              }`}
          >
            <div className="text-center space-y-4">
              {/* Upload Icon */}
              <div className={`w-20 h-20 mx-auto rounded-full flex items-center justify-center
                ${isDataChannelOpen ? 'bg-primary-100' : 'bg-gray-200'}`}>
                <Upload className={`w-10 h-10 ${isDataChannelOpen ? 'text-primary-600' : 'text-gray-400'}`} />
              </div>

              {/* Instructions */}
              <div>
                <p className={`text-lg font-semibold mb-2 ${isDataChannelOpen ? 'text-gray-800' : 'text-gray-500'}`}>
                  {dragActive ? 'Drop file here' : 'Drag & drop file here'}
                </p>
                <p className={`text-sm ${isDataChannelOpen ? 'text-gray-600' : 'text-gray-400'}`}>
                  or
                </p>
              </div>

              {/* Browse Button */}
              <button
                onClick={handleBrowseClick}
                disabled={!isDataChannelOpen}
                className={`px-6 py-3 rounded-lg font-semibold transition-all inline-flex items-center gap-2
                  ${isDataChannelOpen 
                    ? 'bg-primary-500 hover:bg-primary-600 text-white shadow-lg hover:shadow-xl' 
                    : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  }`}
              >
                <FolderOpen className="w-5 h-5" />
                Browse Files
              </button>

              {/* Hint */}
              {!isDataChannelOpen && (
                <p className="text-sm text-gray-500 mt-4">
                  Wait for P2P connection to be established
                </p>
              )}
            </div>
          </div>
        ) : (
          /* Selected File Display */
          <div className="bg-gradient-to-br from-primary-50 to-purple-50 rounded-xl p-6 border-2 border-primary-200">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-4 flex-1">
                <div className="w-16 h-16 bg-primary-100 rounded-xl flex items-center justify-center flex-shrink-0">
                  <FileIcon className="w-8 h-8 text-primary-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-800 text-lg truncate">
                    {fileTransfer.file.name}
                  </p>
                  <div className="flex items-center gap-3 mt-1 text-sm text-gray-600">
                    <span className="font-medium">{formatBytes(fileTransfer.file.size)}</span>
                    {fileTransfer.file.type && (
                      <>
                        <span>•</span>
                        <span>{fileTransfer.file.type.split('/')[0]}</span>
                      </>
                    )}
                  </div>
                </div>
              </div>
              
              {!isSending && (
                <button
                  onClick={clearFile}
                  className="p-2 hover:bg-red-100 rounded-lg transition-colors flex-shrink-0"
                  title="Remove file"
                >
                  <X className="w-5 h-5 text-red-600" />
                </button>
              )}
            </div>

            {/* Action Buttons */}
            <div className="flex gap-3">
              <button
                onClick={handleSendFile}
                disabled={isSending}
                className="btn-primary flex-1 flex items-center justify-center gap-2"
              >
                {isSending ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Sending...
                  </>
                ) : (
                  <>
                    <Upload className="w-5 h-5" />
                    Send File
                  </>
                )}
              </button>

              {!isSending && (
                <button
                  onClick={handleBrowseClick}
                  className="px-6 py-3 bg-white hover:bg-gray-50 border-2 border-gray-300 rounded-lg font-semibold transition-all"
                >
                  Change
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default FileSelector;