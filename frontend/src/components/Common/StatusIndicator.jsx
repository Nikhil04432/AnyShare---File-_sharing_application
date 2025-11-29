import React from 'react';
import { CONNECTION_STATE } from '../../utils/constants';
import { Wifi, WifiOff, Loader2 } from 'lucide-react';

const StatusIndicator = ({ state }) => {
  const getStatusConfig = () => {
    switch (state) {
      case CONNECTION_STATE.CONNECTED:
        return {
          icon: Wifi,
          text: 'Connected',
          className: 'bg-green-100 text-green-800 border-green-300',
          iconClassName: 'text-green-600',
        };
      case CONNECTION_STATE.CONNECTING:
        return {
          icon: Loader2,
          text: 'Connecting...',
          className: 'bg-yellow-100 text-yellow-800 border-yellow-300',
          iconClassName: 'text-yellow-600 animate-spin',
        };
      case CONNECTION_STATE.ERROR:
        return {
          icon: WifiOff,
          text: 'Error',
          className: 'bg-red-100 text-red-800 border-red-300',
          iconClassName: 'text-red-600',
        };
      default:
        return {
          icon: WifiOff,
          text: 'Disconnected',
          className: 'bg-gray-100 text-gray-800 border-gray-300',
          iconClassName: 'text-gray-600',
        };
    }
  };

  const config = getStatusConfig();
  const Icon = config.icon;

  return (
    <div className={`flex items-center gap-2 px-4 py-3 rounded-lg border-2 font-semibold ${config.className}`}>
      <Icon className={`w-5 h-5 ${config.iconClassName}`} />
      <span>{config.text}</span>
    </div>
  );
};

export default StatusIndicator;