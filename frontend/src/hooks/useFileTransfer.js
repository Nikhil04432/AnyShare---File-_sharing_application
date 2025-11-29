import { useCallback, useRef, useEffect } from 'react';
import useStore from '../store/useStore';
import { CHUNK_SIZE, MESSAGE_TYPES } from '../utils/constants';
import { formatBytes, calculateSpeed } from '../utils/helpers';

export const useFileTransfer = () => {
  const {
    dataChannel,
    peerConnection,
    fileTransfer,
    setFileTransfer,
    addLog,
  } = useStore();

  const receivedChunksRef = useRef([]);
  const startTimeRef = useRef(null);

  /**
   * Send file via data channel - WITH CONNECTION VALIDATION
   */
  const sendFile = useCallback(async (file) => {
    // FIXED: Validate peer connection state
    if (!peerConnection || 
        (peerConnection.connectionState !== 'connected' && 
         peerConnection.connectionState !== 'connecting')) {
      addLog('Cannot send file: Peer connection not active', 'error');
      setFileTransfer({ 
        status: 'error',
        error: 'Receiver is disconnected. Cannot send file.'
      });
      return;
    }

    // Validate data channel
    if (!dataChannel || dataChannel.readyState !== 'open') {
      addLog('Cannot send file: Data channel not open', 'error');
      setFileTransfer({ 
        status: 'error',
        error: 'Data channel not ready. Cannot send file.'
      });
      return;
    }

    if (!file) {
      addLog('No file selected', 'error');
      return;
    }

    addLog(`Starting file transfer: ${file.name} (${formatBytes(file.size)})`, 'info');

    // Reset state
    setFileTransfer({
      file,
      metadata: {
        name: file.name,
        size: file.size,
        type: file.type,
        totalChunks: Math.ceil(file.size / CHUNK_SIZE),
      },
      progress: 0,
      status: 'sending',
      startTime: Date.now(),
      bytesTransferred: 0,
    });

    startTimeRef.current = Date.now();

    try {
      // Send metadata first
      const metadata = {
        type: MESSAGE_TYPES.FILE_METADATA,
        name: file.name,
        size: file.size,
        mimeType: file.type,
        totalChunks: Math.ceil(file.size / CHUNK_SIZE),
      };

      dataChannel.send(JSON.stringify(metadata));
      addLog('File metadata sent', 'info');

      // Buffer management
      const HIGH_WATER_MARK = 256 * 1024;  // 256KB
      const LOW_WATER_MARK = 64 * 1024;    // 64KB
      
      let offset = 0;
      let chunkIndex = 0;

      // Batch progress updates
      let lastProgressUpdate = Date.now();
      const PROGRESS_UPDATE_INTERVAL = 100;

      const shouldPause = () => {
        return dataChannel.bufferedAmount >= HIGH_WATER_MARK;
      };

      const waitForBuffer = () => {
        return new Promise((resolve) => {
          if (dataChannel.bufferedAmount < LOW_WATER_MARK) {
            resolve();
            return;
          }

          const checkBuffer = () => {
            // ADDED: Check if peer is still connected while waiting
            if (!peerConnection || peerConnection.connectionState !== 'connected') {
              addLog('Peer disconnected during transfer', 'error');
              throw new Error('Peer disconnected');
            }

            if (dataChannel.bufferedAmount < LOW_WATER_MARK) {
              resolve();
            } else {
              setTimeout(checkBuffer, 5);
            }
          };

          checkBuffer();
        });
      };

      // Read and send file in chunks
      while (offset < file.size) {
        // ADDED: Check connection status before each chunk
        if (!peerConnection || peerConnection.connectionState !== 'connected') {
          throw new Error('Peer disconnected during file transfer');
        }

        if (!dataChannel || dataChannel.readyState !== 'open') {
          throw new Error('Data channel closed during file transfer');
        }

        // Wait if buffer is full
        if (shouldPause()) {
          await waitForBuffer();
        }

        // Read chunk
        const slice = file.slice(offset, offset + CHUNK_SIZE);
        const chunk = await slice.arrayBuffer();

        // Send chunk
        dataChannel.send(chunk);
        
        chunkIndex++;
        offset += chunk.byteLength;

        // Throttle progress updates
        const now = Date.now();
        if (now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL || offset >= file.size) {
          const progress = Math.round((offset / file.size) * 100);
          const elapsed = now - startTimeRef.current;
          const speed = calculateSpeed(offset, elapsed);

          setFileTransfer({
            progress,
            bytesTransferred: offset,
            speed,
          });

          lastProgressUpdate = now;

          if (chunkIndex % 100 === 0 || offset >= file.size) {
            addLog(`Sent ${chunkIndex}/${metadata.totalChunks} chunks (${progress}%) @ ${speed}`, 'info');
          }
        }

        // Yield occasionally
        if (chunkIndex % 200 === 0) {
          await new Promise(resolve => setTimeout(resolve, 0));
        }
      }

      // Send completion signal
      dataChannel.send(JSON.stringify({
        type: MESSAGE_TYPES.FILE_COMPLETE,
      }));

      const totalTime = ((Date.now() - startTimeRef.current) / 1000).toFixed(2);
      const avgSpeed = formatBytes(file.size / (totalTime || 1)) + '/s';
      
      addLog(`✅ File sent in ${totalTime}s (avg: ${avgSpeed})`, 'success');
      
      setFileTransfer({
        status: 'complete',
        progress: 100,
        speed: avgSpeed,
      });

    } catch (error) {
      addLog(`Error sending file: ${error.message}`, 'error');
      setFileTransfer({ 
        status: 'error',
        error: error.message 
      });
    }
  }, [dataChannel, peerConnection, setFileTransfer, addLog]);

  /**
   * Handle incoming file data
   */
  const handleFileData = useCallback((data) => {
    if (typeof data === 'string') {
      try {
        const message = JSON.parse(data);

        if (message.type === MESSAGE_TYPES.FILE_METADATA) {
          addLog(`Receiving: ${message.name} (${formatBytes(message.size)})`, 'info');
          
          receivedChunksRef.current = [];
          startTimeRef.current = Date.now();

          setFileTransfer({
            metadata: message,
            progress: 0,
            status: 'receiving',
            startTime: Date.now(),
            bytesTransferred: 0,
          });

        } else if (message.type === MESSAGE_TYPES.FILE_COMPLETE) {
          const totalTime = ((Date.now() - startTimeRef.current) / 1000).toFixed(2);
          const avgSpeed = formatBytes(fileTransfer.metadata?.size / (totalTime || 1)) + '/s';
          
          addLog(`✅ File received in ${totalTime}s (avg: ${avgSpeed})`, 'success');
          
          setFileTransfer({
            status: 'complete',
            progress: 100,
            speed: avgSpeed,
          });
        }
      } catch (error) {
        // Not JSON, ignore
      }
    } else {
      // It's a file chunk
      receivedChunksRef.current.push(data);

      const { metadata } = fileTransfer;
      if (metadata) {
        const chunksReceived = receivedChunksRef.current.length;
        const bytesReceived = receivedChunksRef.current.reduce(
          (total, chunk) => total + chunk.byteLength,
          0
        );

        if (chunksReceived % 50 === 0 || chunksReceived === metadata.totalChunks) {
          const progress = Math.round((bytesReceived / metadata.size) * 100);
          const elapsed = Date.now() - startTimeRef.current;
          const speed = calculateSpeed(bytesReceived, elapsed);

          setFileTransfer({
            progress,
            bytesTransferred: bytesReceived,
            speed,
          });

          if (chunksReceived % 100 === 0 || chunksReceived === metadata.totalChunks) {
            addLog(
              `Received ${chunksReceived}/${metadata.totalChunks} chunks (${progress}%) @ ${speed}`,
              'info'
            );
          }
        }
      }
    }
  }, [fileTransfer, setFileTransfer, addLog]);

  /**
   * Download received file
   */
  const downloadFile = useCallback(() => {
    const { metadata } = fileTransfer;

    if (!metadata || receivedChunksRef.current.length === 0) {
      addLog('No file to download', 'error');
      return;
    }

    try {
      addLog('Creating download...', 'info');

      const blob = new Blob(receivedChunksRef.current, {
        type: metadata.mimeType || 'application/octet-stream',
      });

      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = metadata.name;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      addLog(`Downloaded: ${metadata.name}`, 'success');

      receivedChunksRef.current = [];

    } catch (error) {
      addLog(`Error downloading file: ${error.message}`, 'error');
    }
  }, [fileTransfer, addLog]);

  /**
   * Listen for data channel messages
   */
  useEffect(() => {
    const handleDataChannelMessage = (event) => {
      handleFileData(event.detail);
    };

    window.addEventListener('data-channel-message', handleDataChannelMessage);

    return () => {
      window.removeEventListener('data-channel-message', handleDataChannelMessage);
    };
  }, [handleFileData]);

  return {
    sendFile,
    downloadFile,
  };
};