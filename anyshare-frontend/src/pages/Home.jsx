import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Send, Download, Zap, Shield, Globe } from 'lucide-react';

const Home = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="max-w-4xl w-full">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-6xl font-bold text-white mb-4">
            🚀 AnyShare
          </h1>
          <p className="text-2xl text-white/90 font-medium">
            Share files instantly. Anywhere. Anytime.
          </p>
          <p className="text-white/70 mt-2">
            Fast, secure, peer-to-peer file sharing
          </p>
        </div>

        {/* Action Cards */}
        <div className="grid md:grid-cols-2 gap-6 mb-12">
          {/* Send Card */}
          <div
            onClick={() => navigate('/sender')}
            className="card cursor-pointer hover:shadow-3xl hover:-translate-y-2 transition-all duration-300 group"
          >
            <div className="text-center">
              <div className="w-20 h-20 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform">
                <Send className="w-10 h-10 text-white" />
              </div>
              <h2 className="text-3xl font-bold text-gray-800 mb-3">Send Files</h2>
              <p className="text-gray-600 mb-6">
                Create a session and share files with anyone
              </p>
              <div className="btn-primary inline-block">
                Start Sending →
              </div>
            </div>
          </div>

          {/* Receive Card */}
          <div
            onClick={() => navigate('/receiver')}
            className="card cursor-pointer hover:shadow-3xl hover:-translate-y-2 transition-all duration-300 group"
          >
            <div className="text-center">
              <div className="w-20 h-20 bg-gradient-to-br from-green-500 to-blue-500 rounded-full flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform">
                <Download className="w-10 h-10 text-white" />
              </div>
              <h2 className="text-3xl font-bold text-gray-800 mb-3">Receive Files</h2>
              <p className="text-gray-600 mb-6">
                Enter room code and receive files instantly
              </p>
              <div className="btn-primary inline-block">
                Start Receiving →
              </div>
            </div>
          </div>
        </div>

        {/* Features */}
        <div className="card">
          <h3 className="text-2xl font-bold text-gray-800 text-center mb-8">
            Why Choose AnyShare?
          </h3>
          
          <div className="grid md:grid-cols-3 gap-6">
            <div className="text-center">
              <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <Zap className="w-6 h-6 text-primary-600" />
              </div>
              <h4 className="font-semibold text-gray-800 mb-2">Lightning Fast</h4>
              <p className="text-sm text-gray-600">
                Direct peer-to-peer transfer at LAN speeds
              </p>
            </div>

            <div className="text-center">
              <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <Shield className="w-6 h-6 text-green-600" />
              </div>
              <h4 className="font-semibold text-gray-800 mb-2">Secure & Private</h4>
              <p className="text-sm text-gray-600">
                End-to-end encrypted, files never touch our servers
              </p>
            </div>

            <div className="text-center">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <Globe className="w-6 h-6 text-blue-600" />
              </div>
              <h4 className="font-semibold text-gray-800 mb-2">No Limits</h4>
              <p className="text-sm text-gray-600">
                Share any file type, any size, unlimited transfers
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;