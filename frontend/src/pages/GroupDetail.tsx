import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Activity, Play, X } from 'lucide-react';
import { groupsApi, serversApi, scriptsApi } from '../services/api';
import type { ServerGroupDto, ServerDto, ScriptDto, PingResultDto } from '../types/api';

export function GroupDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [group, setGroup] = useState<ServerGroupDto | null>(null);
  const [allServers, setAllServers] = useState<ServerDto[]>([]);
  const [groupServers, setGroupServers] = useState<ServerDto[]>([]);
  const [scripts, setScripts] = useState<ScriptDto[]>([]);
  const [showAddServer, setShowAddServer] = useState(false);
  const [showExecute, setShowExecute] = useState(false);
  const [selectedScript, setSelectedScript] = useState<number | null>(null);
  const [pingResults, setPingResults] = useState<PingResultDto>({});
  const [pinging, setPinging] = useState(false);

  useEffect(() => {
    if (id) {
      fetchData();
    }
  }, [id]);

    const fetchData = async () => {
        try {
            const [groupRes, serversRes, scriptsRes] = await Promise.all([
                groupsApi.getById(parseInt(id!)),
                serversApi.getAll(),
                scriptsApi.getAll(),
            ]);

            setGroup(groupRes.data);
            setAllServers(serversRes.data);
            setScripts(scriptsRes.data);

            // --- БЫЛО (Старая логика) ---
            // const serverIds = groupRes.data.serverIds || [];
            // const servers = serversRes.data.filter((s) => serverIds.includes(s.id!));
            // setGroupServers(servers);

            // --- СТАЛО (Новая логика) ---
            // Бэкенд уже прислал нам полные объекты серверов в поле servers
            if (groupRes.data.servers) {
                setGroupServers(groupRes.data.servers);
            } else {
                setGroupServers([]);
            }

        } catch (error) {
            console.error('Failed to fetch data:', error);
        }
    };

  const handleAddServer = async (serverId: number) => {
    try {
      await groupsApi.addServer(parseInt(id!), serverId);
      setShowAddServer(false);
      fetchData();
    } catch (error) {
      console.error('Failed to add server:', error);
    }
  };

  const handleRemoveServer = async (serverId: number) => {
    try {
      await groupsApi.removeServer(parseInt(id!), serverId);
      fetchData();
    } catch (error) {
      console.error('Failed to remove server:', error);
    }
  };

  const handlePingAll = async () => {
    setPinging(true);
    try {
      const response = await groupsApi.ping(parseInt(id!));
      setPingResults(response.data);
      setTimeout(() => setPingResults({}), 5000);
    } catch (error) {
      console.error('Failed to ping group:', error);
    } finally {
      setPinging(false);
    }
  };

  const handleExecute = async () => {
    if (!selectedScript) return;
    try {
      const response = await groupsApi.execute(parseInt(id!), selectedScript);
      setShowExecute(false);
      if (response.data.length > 0) {
        navigate(`/tasks/${response.data[0].id}`);
      }
    } catch (error) {
      console.error('Failed to execute script:', error);
    }
  };

    const availableServers = allServers.filter(
        (s) => !group?.servers?.some((gs) => gs.id === s.id)
    );

  const getPingStatus = (serverId: number) => {
    const key = serverId.toString();
    if (!(key in pingResults)) return null;
    return pingResults[key] ? 'online' : 'offline';
  };

  if (!group) {
    return (
      <div className="text-center py-12">
        <div className="text-green-500 font-mono">Loading...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <button
          onClick={() => navigate('/groups')}
          className="text-green-500 hover:text-green-400 transition-colors"
        >
          <ArrowLeft className="w-6 h-6" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-green-500 font-mono">{group.name}</h1>
          <p className="text-green-700 font-mono mt-1">
            {groupServers.length} server{groupServers.length !== 1 ? 's' : ''}
          </p>
        </div>
      </div>

      <div className="flex space-x-3">
        <button
          onClick={() => setShowAddServer(true)}
          className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>Add Server</span>
        </button>
        <button
          onClick={handlePingAll}
          disabled={pinging || groupServers.length === 0}
          className="flex items-center space-x-2 bg-blue-900 text-blue-300 px-4 py-2 rounded font-mono hover:bg-blue-800 transition-colors disabled:opacity-50"
        >
          <Activity className="w-4 h-4" />
          <span>{pinging ? 'Pinging...' : 'Ping All'}</span>
        </button>
        <button
          onClick={() => setShowExecute(true)}
          disabled={groupServers.length === 0}
          className="flex items-center space-x-2 bg-yellow-900 text-yellow-300 px-4 py-2 rounded font-mono hover:bg-yellow-800 transition-colors disabled:opacity-50"
        >
          <Play className="w-4 h-4" />
          <span>Execute Script</span>
        </button>
      </div>

      {showAddServer && availableServers.length > 0 && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">
            Add Server to Group
          </h2>
          <div className="space-y-2">
            {availableServers.map((server) => (
              <div
                key={server.id}
                className="flex items-center justify-between bg-black border border-green-900 rounded p-3"
              >
                <div className="font-mono text-green-400">
                  <span className="font-bold">{server.hostname}</span>
                  <span className="text-green-700 ml-3">{server.ipAddress}</span>
                </div>
                <button
                  onClick={() => handleAddServer(server.id!)}
                  className="bg-green-900 text-green-300 px-3 py-1 rounded text-sm font-mono hover:bg-green-800"
                >
                  Add
                </button>
              </div>
            ))}
          </div>
          <button
            onClick={() => setShowAddServer(false)}
            className="mt-4 bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700"
          >
            Cancel
          </button>
        </div>
      )}

      {showExecute && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">
            Execute on All Servers
          </h2>
          <div className="space-y-2 mb-4">
            {scripts.map((script) => (
              <div
                key={script.id}
                onClick={() => setSelectedScript(script.id!)}
                className={`cursor-pointer bg-black border rounded p-3 font-mono transition-colors ${
                  selectedScript === script.id
                    ? 'border-green-500 text-green-400'
                    : 'border-green-900 text-green-600 hover:border-green-700'
                }`}
              >
                {script.name}
              </div>
            ))}
          </div>
          <div className="flex space-x-3">
            <button
              onClick={handleExecute}
              disabled={!selectedScript}
              className="bg-yellow-900 text-yellow-300 px-4 py-2 rounded font-mono hover:bg-yellow-800 disabled:opacity-50"
            >
              Execute
            </button>
            <button
              onClick={() => setShowExecute(false)}
              className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {groupServers.map((server) => {
          const status = getPingStatus(server.id!);
          return (
            <div
              key={server.id}
              className="bg-gray-900 border border-green-900 rounded-lg p-6"
            >
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h3 className="text-green-400 font-mono font-bold flex items-center space-x-2">
                    <span>{server.hostname}</span>
                    {status && (
                      <div
                        className={`w-2 h-2 rounded-full ${
                          status === 'online' ? 'bg-green-500' : 'bg-red-500'
                        }`}
                      />
                    )}
                  </h3>
                  <p className="text-green-700 text-sm font-mono">{server.ipAddress}</p>
                </div>
                <button
                  onClick={() => handleRemoveServer(server.id!)}
                  className="text-red-500 hover:text-red-400 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <div className="space-y-2 text-sm font-mono">
                <div className="flex justify-between text-green-700">
                  <span>User:</span>
                  <span className="text-green-500">{server.username}</span>
                </div>
                <div className="flex justify-between text-green-700">
                  <span>Port:</span>
                  <span className="text-green-500">{server.port}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {groupServers.length === 0 && (
        <div className="text-center py-12">
          <p className="text-green-700 font-mono">No servers in this group</p>
        </div>
      )}
    </div>
  );
}
