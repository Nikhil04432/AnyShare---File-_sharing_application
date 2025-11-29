/**
 * Format bytes to human-readable string
 */
export const formatBytes = (bytes, decimals = 2) => {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
};

/**
 * Format time remaining
 */
export const formatTimeRemaining = (seconds) => {
  if (seconds < 60) return `${Math.round(seconds)}s`;
  if (seconds < 3600) return `${Math.round(seconds / 60)}m`;
  return `${Math.round(seconds / 3600)}h`;
};

/**
 * Calculate transfer speed
 */
export const calculateSpeed = (bytes, milliseconds) => {
  const seconds = milliseconds / 1000;
  const bytesPerSecond = bytes / seconds;
  return formatBytes(bytesPerSecond) + '/s';
};

/**
 * Validate room code format
 */
export const isValidRoomCode = (code) => {
  // Format: WORD-NNNN (e.g., SWIFT-7284)
  const regex = /^[A-Z]+-\d{4}$/;
  return regex.test(code);
};

/**
 * Generate random color for peer
 */
export const generatePeerColor = (peerId) => {
  let hash = 0;
  for (let i = 0; i < peerId.length; i++) {
    hash = peerId.charCodeAt(i) + ((hash << 5) - hash);
  }
  const hue = hash % 360;
  return `hsl(${hue}, 70%, 50%)`;
};

/**
 * Log with timestamp
 */
export const logWithTime = (message, type = 'info') => {
  const time = new Date().toLocaleTimeString();
  console.log(`[${time}] [${type.toUpperCase()}]`, message);
  return { time, message, type };
};