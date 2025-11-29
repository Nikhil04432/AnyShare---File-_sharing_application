import React from 'react';
import { QRCodeSVG } from 'qrcode.react';
import useStore from '../../store/useStore';
import { WS_BASE_URL } from '../../utils/constants';
import { Copy, Check } from 'lucide-react';
import { useState } from 'react';

const QRCodeDisplay = () => {
  const { session } = useStore();
  const [copied, setCopied] = useState(false);

  if (!session.roomCode) return null;

  const qrData = JSON.stringify({
    roomCode: session.roomCode,
    wsUrl: WS_BASE_URL,
  });

  const copyToClipboard = () => {
    navigator.clipboard.writeText(session.roomCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="mt-6 bg-gradient-to-br from-primary-50 to-purple-50 rounded-xl p-6 text-center">
      <p className="text-gray-700 font-medium mb-4">Share this code with receiver:</p>
      
      <div className="flex items-center justify-center gap-3 mb-6">
        <div className="text-4xl font-bold text-primary-600 tracking-wider">
          {session.roomCode}
        </div>
        <button
          onClick={copyToClipboard}
          className="p-2 hover:bg-white rounded-lg transition-colors"
          title="Copy to clipboard"
        >
          {copied ? (
            <Check className="w-6 h-6 text-green-600" />
          ) : (
            <Copy className="w-6 h-6 text-gray-600" />
          )}
        </button>
      </div>

      <div className="bg-white p-6 rounded-xl inline-block shadow-lg">
        <QRCodeSVG
          value={qrData}
          size={200}
          level="H"
          includeMargin={true}
        />
      </div>

      <p className="text-sm text-gray-600 mt-4">
        Scan this QR code or enter the room code manually
      </p>
    </div>
  );
};

export default QRCodeDisplay;