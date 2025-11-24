import { useCallback, useRef, useEffect } from 'react';
import useStore from '../store/useStore';
import { CHUNK_SIZE, MESSAGE_TYPES } from '../utils/constants';
import { formatBytes, calculateSpeed } from '../utils/helpers';

export const useFileTransfer = () => {
  const {
    dataChannel,
    fileTransfer,
    setFileTransfer,
    addLog,
  } = useStore();

  const receivedChunksRef = useRef([]);
  const startTimeRef = useRef(null);

  /**
   * Send file via data channel
   */
      const sendFile = useCallback(async (file) => {
      if (!dataChannel || dataChannel.readyState !== 'open') {
        addLog('Cannot send file: Data channel not open', 'error');
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

        // Optimized chunk sending with backpressure
        const HIGH_WATER_MARK = 16 * 1024 * 1024; // 16MB buffer threshold
        const LOW_WATER_MARK = 1 * 1024 * 1024;   // 1MB buffer threshold
        
        let offset = 0;
        let chunkIndex = 0;
        let isPaused = false;

        // Function to check if we should pause sending
        const shouldPause = () => {
          return dataChannel.bufferedAmount >= HIGH_WATER_MARK;
        };

        // Function to wait until buffer drains
        const waitForBuffer = () => {
          return new Promise((resolve) => {
            if (dataChannel.bufferedAmount < LOW_WATER_MARK) {
              resolve();
              return;
            }

            const checkBuffer = () => {
              if (dataChannel.bufferedAmount < LOW_WATER_MARK) {
                isPaused = false;
                addLog('Buffer drained, resuming...', 'info');
                resolve();
              } else {
                setTimeout(checkBuffer, 10); // Check every 10ms
              }
            };

            isPaused = true;
            addLog('Buffer full, pausing...', 'warning');
            checkBuffer();
          });
        };

        // Read and send file in chunks
        while (offset < file.size) {
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

          // Calculate progress and speed
          const progress = Math.round((offset / file.size) * 100);
          const elapsed = Date.now() - startTimeRef.current;
          const speed = calculateSpeed(offset, elapsed);

          setFileTransfer({
            progress,
            bytesTransferred: offset,
            speed,
          });

          // Log less frequently to avoid UI lag
          if (chunkIndex % 50 === 0 || offset >= file.size) {
            addLog(`Sent chunk ${chunkIndex}/${metadata.totalChunks} (${progress}%) - ${speed}`, 'info');
          }

          // Small yield to prevent blocking UI
          if (chunkIndex % 100 === 0) {
            await new Promise(resolve => setTimeout(resolve, 0));
          }
        }

        // Send completion signal
        dataChannel.send(JSON.stringify({
          type: MESSAGE_TYPES.FILE_COMPLETE,
        }));

        const totalTime = ((Date.now() - startTimeRef.current) / 1000).toFixed(2);
        const avgSpeed = formatBytes(file.size / (totalTime || 1)) + '/s';
        
        addLog(`File sent successfully in ${totalTime}s (avg: ${avgSpeed})`, 'success');
        
        setFileTransfer({
          status: 'complete',
          progress: 100,
          speed: avgSpeed,
        });

      } catch (error) {
        addLog(`Error sending file: ${error.message}`, 'error');
        setFileTransfer({ status: 'error' });
      }
    }, [dataChannel, setFileTransfer, addLog]);

  /**
   * Handle incoming file data
   */
    const handleFileData = useCallback((data) => {
    // Check if it's metadata or chunk
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
          
          addLog(`File received successfully in ${totalTime}s (avg: ${avgSpeed})`, 'success');
          
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
      // It's a file chunk (ArrayBuffer)
      receivedChunksRef.current.push(data);

      const { metadata } = fileTransfer;
      if (metadata) {
        const progress = Math.round(
          (receivedChunksRef.current.length / metadata.totalChunks) * 100
        );

        const bytesReceived = receivedChunksRef.current.reduce(
          (total, chunk) => total + chunk.byteLength,
          0
        );

        const elapsed = Date.now() - startTimeRef.current;
        const speed = calculateSpeed(bytesReceived, elapsed);

        setFileTransfer({
          progress,
          bytesTransferred: bytesReceived,
          speed,
        });

        // Log less frequently
        if (receivedChunksRef.current.length % 50 === 0 || 
            receivedChunksRef.current.length === metadata.totalChunks) {
          addLog(
            `Received chunk ${receivedChunksRef.current.length}/${metadata.totalChunks} (${progress}%) - ${speed}`,
            'info'
          );
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

      // Combine all chunks into a Blob
      const blob = new Blob(receivedChunksRef.current, {
        type: metadata.mimeType || 'application/octet-stream',
      });

      // Create download link
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = metadata.name;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      addLog(`Downloaded: ${metadata.name}`, 'success');

      // Clear received chunks
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