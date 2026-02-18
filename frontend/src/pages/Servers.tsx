import { useEffect, useState } from 'react';
import { Plus, Activity, Server as ServerIcon } from 'lucide-react';
import { serversApi } from '../services/api';
import type { ServerDto } from '../types/api';

export function Servers() {
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [pingStatus, setPingStatus] = useState<Record<number, 'checking' | 'online' | 'offline'>>({});
  const [formData, setFormData] = useState<ServerDto>({
    hostname: '',
    ipAddress: '',
    port: 22,
    username: '',
    password: '',
  });

  useEffect(() => {
    fetchServers();
  }, []);

  const fetchServers = async () => {
    try {
      const response = await serversApi.getAll();
      setServers(response.data);
    } catch (error) {
      console.error('Failed to fetch servers:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await serversApi.create(formData);
      setFormData({
        hostname: '',
        ipAddress: '',
        port: 22,
        username: '',
        password: '',
      });
      setShowAddForm(false);
      fetchServers();
    } catch (error) {
      console.error('Failed to create server:', error);
    }
  };

  const handlePing = async (serverId: number) => {
    setPingStatus((prev) => ({ ...prev, [serverId]: 'checking' }));
    try {
      await serversApi.ping(serverId);
      setPingStatus((prev) => ({ ...prev, [serverId]: 'online' }));
      setTimeout(() => {
        setPingStatus((prev) => {
          const newStatus = { ...prev };
          delete newStatus[serverId];
          return newStatus;
        });
      }, 3000);
    } catch (error) {
      setPingStatus((prev) => ({ ...prev, [serverId]: 'offline' }));
      setTimeout(() => {
        setPingStatus((prev) => {
          const newStatus = { ...prev };
          delete newStatus[serverId];
          return newStatus;
        });
      }, 3000);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-green-500 font-mono">$ servers</h1>
          <p className="text-green-700 font-mono mt-1">Manage your infrastructure</p>
        </div>
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>Add Server</span>
        </button>
      </div>

      {showAddForm && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">
            New Server
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">
                  Hostname
                </label>
                <input
                  type="text"
                  value={formData.hostname}
                  onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">
                  IP Address
                </label>
                <input
                  type="text"
                  value={formData.ipAddress}
                  onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  pattern="^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">
                  Username
                </label>
                <input
                  type="text"
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">
                  Port
                </label>
                <input
                  type="number"
                  value={formData.port}
                  onChange={(e) => setFormData({ ...formData, port: parseInt(e.target.value) })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  min="1"
                  max="65535"
                />
              </div>
              <div className="md:col-span-2">
                <label className="block text-green-500 font-mono text-sm mb-2">
                  Password
                </label>
                <input
                  type="password"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                />
              </div>
            </div>
            <div className="flex space-x-3">
              <button
                type="submit"
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
              >
                Create
              </button>
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {servers.map((server) => {
          const status = pingStatus[server.id!];
          return (
            <div
              key={server.id}
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <ServerIcon className="w-6 h-6 text-green-500" />
                  <div>
                    <h3 className="text-green-400 font-mono font-bold">
                      {server.hostname}
                    </h3>
                    <p className="text-green-700 text-sm font-mono">{server.ipAddress}</p>
                  </div>
                </div>
                {status && (
                  <div
                    className={`w-3 h-3 rounded-full ${
                      status === 'checking'
                        ? 'bg-yellow-500 animate-pulse'
                        : status === 'online'
                        ? 'bg-green-500'
                        : 'bg-red-500'
                    }`}
                  />
                )}
              </div>
              <div className="space-y-2 text-sm font-mono mb-4">
                <div className="flex justify-between text-green-700">
                  <span>User:</span>
                  <span className="text-green-500">{server.username}</span>
                </div>
                <div className="flex justify-between text-green-700">
                  <span>Port:</span>
                  <span className="text-green-500">{server.port}</span>
                </div>
              </div>
              <button
                onClick={() => handlePing(server.id!)}
                disabled={status === 'checking'}
                className="w-full flex items-center justify-center space-x-2 bg-green-900 text-green-300 px-3 py-2 rounded font-mono text-sm hover:bg-green-800 transition-colors disabled:opacity-50"
              >
                <Activity className="w-4 h-4" />
                <span>{status === 'checking' ? 'Pinging...' : 'Ping'}</span>
              </button>
            </div>
          );
        })}
      </div>

      {servers.length === 0 && !showAddForm && (
        <div className="text-center py-12">
          <ServerIcon className="w-16 h-16 text-green-900 mx-auto mb-4" />
          <p className="text-green-700 font-mono">No servers configured</p>
        </div>
      )}
    </div>
  );
}
