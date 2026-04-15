import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Plus, Activity, Server as ServerIcon, Trash2 } from 'lucide-react';
import { serversApi } from '../services/api';
import type { ServerDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { useToast } from '../contexts/ToastContext';

export function Servers() {
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [pingStatus, setPingStatus] = useState<Record<number, 'checking' | 'online' | 'offline'>>(
    {}
  );
  const [pingErrors, setPingErrors] = useState<Record<number, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState<ServerDto>({
    hostname: '',
    ipAddress: '',
    port: 22,
    username: '',
    password: '',
  });
  const [searchParams, setSearchParams] = useSearchParams();
  const { showToast } = useToast();

  const isCreateRequested = searchParams.get('create') === '1';

  useEffect(() => {
    fetchServers();
  }, []);

  useEffect(() => {
    if (isCreateRequested) {
      setShowAddForm(true);
    }
  }, [isCreateRequested]);

  const openCreateForm = () => {
    setShowAddForm(true);
    const params = new URLSearchParams(searchParams);
    params.set('create', '1');
    setSearchParams(params, { replace: true });
  };

  const closeCreateForm = () => {
    setShowAddForm(false);
    const params = new URLSearchParams(searchParams);
    params.delete('create');
    setSearchParams(params, { replace: true });
  };

  const fetchServers = async () => {
    try {
      const response = await serversApi.getAll();
      setServers(response.data);
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch servers:', fetchError);
      setError('Failed to load servers.');
    }
  };

  const clearPingStatus = (serverId: number) => {
    setTimeout(() => {
      setPingStatus((prev) => {
        const newStatus = { ...prev };
        delete newStatus[serverId];
        return newStatus;
      });
    }, 3000);
  };

  const removePingStatus = (serverId: number) => {
    setPingStatus((prev) => {
      const newStatus = { ...prev };
      delete newStatus[serverId];
      return newStatus;
    });
  };

  const clearPingError = (serverId: number) => {
    setPingErrors((prev) => {
      const newErrors = { ...prev };
      delete newErrors[serverId];
      return newErrors;
    });
  };

  const schedulePingErrorClear = (serverId: number) => {
    setTimeout(() => {
      clearPingError(serverId);
    }, 3000);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    setIsSubmitting(true);
    try {
      await serversApi.create(formData);
      setFormData({
        hostname: '',
        ipAddress: '',
        port: 22,
        username: '',
        password: '',
      });
      closeCreateForm();
      setError(null);
      showToast('Server created.', 'success');
      await fetchServers();
    } catch (submitError) {
      console.error('Failed to create server:', submitError);
      setError('Failed to create server.');
      showToast('Failed to create server.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (serverId: number) => {
    if (!confirm('Delete this server?')) {
      return;
    }

    try {
      await serversApi.delete(serverId);
      removePingStatus(serverId);
      clearPingError(serverId);
      setError(null);
      showToast('Server deleted.', 'success');
      await fetchServers();
    } catch (deleteError) {
      console.error('Failed to delete server:', deleteError);
      setError('Failed to delete server.');
      showToast('Failed to delete server.', 'error');
    }
  };

  const handlePing = async (serverId: number) => {
    setPingStatus((prev) => ({ ...prev, [serverId]: 'checking' }));
    clearPingError(serverId);
    try {
      const response = await serversApi.ping(serverId);
      const alive = response.data.alive;
      setPingStatus((prev) => ({
        ...prev,
        [serverId]: alive ? 'online' : 'offline',
      }));
      setError(null);
      showToast(alive ? 'Server is online.' : 'Server is offline.', alive ? 'success' : 'info');
      clearPingStatus(serverId);
    } catch (pingError) {
      console.error('Failed to ping server:', pingError);
      removePingStatus(serverId);
      setPingErrors((prev) => ({ ...prev, [serverId]: true }));
      schedulePingErrorClear(serverId);
      setError('Failed to ping server.');
      showToast('Failed to ping server.', 'error');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="$ servers"
        subtitle="Manage your infrastructure"
        actions={
          <button
            onClick={showAddForm ? closeCreateForm : openCreateForm}
            className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>{showAddForm ? 'Close Form' : 'Add Server'}</span>
          </button>
        }
      />

      {error && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-2 rounded font-mono text-sm">
          {error}
        </div>
      )}

      {showAddForm && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">New Server</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">Hostname</label>
                <input
                  type="text"
                  value={formData.hostname}
                  onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">IP Address</label>
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
                <label className="block text-green-500 font-mono text-sm mb-2">SSH Username</label>
                <input
                  type="text"
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">Port</label>
                <input
                  type="number"
                  value={formData.port}
                  onChange={(e) => setFormData({ ...formData, port: parseInt(e.target.value, 10) })}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  min="1"
                  max="65535"
                />
              </div>
              <div className="md:col-span-2">
                <label className="block text-green-500 font-mono text-sm mb-2">Password</label>
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
                disabled={isSubmitting}
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? 'Creating...' : 'Create'}
              </button>
              <button
                type="button"
                onClick={closeCreateForm}
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
          const hasPingError = Boolean(pingErrors[server.id!]);
          return (
            <div
              key={server.id}
              className={`bg-gray-900 border rounded-lg p-6 transition-colors ${
                hasPingError
                  ? 'border-red-700 bg-red-950/30 hover:border-red-600'
                  : 'border-green-900 hover:border-green-700'
              }`}
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <ServerIcon className="w-6 h-6 text-green-500" />
                  <div>
                    <h3 className="text-green-400 font-mono font-bold">{server.hostname}</h3>
                    <p className="text-green-700 text-sm font-mono">{server.ipAddress}</p>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
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
                  <button
                    onClick={() => handleDelete(server.id!)}
                    className="text-red-500 hover:text-red-400 transition-colors"
                    title="Delete server"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div className="space-y-2 text-sm font-mono mb-4">
                <div className="flex justify-between text-green-700">
                  <span>SSH User:</span>
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
                className={`w-full flex items-center justify-center space-x-2 px-3 py-2 rounded font-mono text-sm transition-colors disabled:opacity-50 ${
                  hasPingError
                    ? 'bg-red-900 text-red-200 hover:bg-red-800'
                    : 'bg-green-900 text-green-300 hover:bg-green-800'
                }`}
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
