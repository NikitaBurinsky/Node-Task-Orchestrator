import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, Activity, Play, X, Lock, CheckCircle2, XCircle } from 'lucide-react';
import { groupsApi, serversApi, scriptsApi } from '../services/api';
import type { ServerGroupDto, ServerDto, ScriptDto, PingResultDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useSafeBack } from '../hooks/useSafeBack';
import { useToast } from '../contexts/ToastContext';
import { useConfirmDialog } from '../contexts/ConfirmDialogContext';

type PingUiStatus = 'online' | 'offline' | 'unknown';

export function GroupDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const goBack = useSafeBack('/groups');
  const [group, setGroup] = useState<ServerGroupDto | null>(null);
  const [allServers, setAllServers] = useState<ServerDto[]>([]);
  const [groupServers, setGroupServers] = useState<ServerDto[]>([]);
  const [scripts, setScripts] = useState<ScriptDto[]>([]);
  const [showAddServer, setShowAddServer] = useState(false);
  const [showExecute, setShowExecute] = useState(false);
  const [selectedScript, setSelectedScript] = useState<number | null>(null);
  const [pingResults, setPingResults] = useState<PingResultDto>({});
  const [lastPingAt, setLastPingAt] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pinging, setPinging] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [updatingMembership, setUpdatingMembership] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showToast } = useToast();
  const { confirm } = useConfirmDialog();

  const groupId = id ? parseInt(id, 10) : null;

  const fetchData = useCallback(async () => {
    if (!groupId) return;

    setIsLoading(true);
    try {
      const [groupRes, serversRes, scriptsRes] = await Promise.all([
        groupsApi.getById(groupId),
        serversApi.getAll(),
        scriptsApi.getAll(),
      ]);

      setGroup(groupRes.data);
      setAllServers(serversRes.data);
      setScripts(scriptsRes.data);
      setGroupServers(groupRes.data.servers ?? []);
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch data:', fetchError);
      setError('Failed to load group data.');
    } finally {
      setIsLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const isDefaultGroup = group?.name === 'Default';

  const handleAddServer = async (serverId: number) => {
    if (!groupId || updatingMembership) return;

    setUpdatingMembership(true);
    try {
      await groupsApi.addServer(groupId, serverId);
      setShowAddServer(false);
      setError(null);
      showToast('Server added to group.', 'success');
      await fetchData();
    } catch (addError) {
      console.error('Failed to add server:', addError);
      setError('Failed to add server to group.');
      showToast('Failed to add server to group.', 'error');
    } finally {
      setUpdatingMembership(false);
    }
  };

  const handleRemoveServer = async (server: ServerDto) => {
    if (!groupId || updatingMembership || !server.id) return;

    const confirmed = await confirm({
      title: 'Remove server from group?',
      description: `${server.hostname} will be detached from this group.`,
      confirmText: 'Remove',
      cancelText: 'Keep',
      tone: 'danger',
    });

    if (!confirmed) {
      return;
    }

    setUpdatingMembership(true);
    try {
      await groupsApi.removeServer(groupId, server.id);
      setError(null);
      showToast('Server removed from group.', 'success');
      await fetchData();
    } catch (removeError) {
      console.error('Failed to remove server:', removeError);
      setError('Failed to remove server from group.');
      showToast('Failed to remove server from group.', 'error');
    } finally {
      setUpdatingMembership(false);
    }
  };

  const handlePingAll = async () => {
    if (!groupId) return;

    setPinging(true);
    try {
      const response = await groupsApi.ping(groupId);
      setPingResults(response.data);
      setLastPingAt(new Date().toISOString());
      setError(null);

      const onlineCount = groupServers.filter(
        (server) => response.data[(server.id ?? 0).toString()] === true
      ).length;
      const offlineCount = groupServers.length - onlineCount;

      showToast(
        `Ping completed. Online: ${onlineCount}, Offline: ${offlineCount}.`,
        offlineCount > 0 ? 'info' : 'success'
      );
    } catch (pingError) {
      console.error('Failed to ping group:', pingError);
      setError('Failed to ping group.');
      showToast('Failed to ping group.', 'error');
    } finally {
      setPinging(false);
    }
  };

  const handleExecute = async () => {
    if (!groupId || !selectedScript || executing) return;

    setExecuting(true);
    try {
      const response = await groupsApi.execute(groupId, selectedScript);
      setShowExecute(false);
      setSelectedScript(null);
      setError(null);
      showToast('Script execution started.', 'success');
      if (response.data.length > 0 && response.data[0].id) {
        navigate(`/tasks/${response.data[0].id}`);
      }
    } catch (executeError) {
      console.error('Failed to execute script:', executeError);
      setError('Failed to execute script.');
      showToast('Failed to execute script.', 'error');
    } finally {
      setExecuting(false);
    }
  };

  const availableServers = useMemo(
    () =>
      allServers.filter((server) => !groupServers.some((groupServer) => groupServer.id === server.id)),
    [allServers, groupServers]
  );

  const pingStatusRows = useMemo(
    () =>
      groupServers.map((server) => ({
        server,
        status:
          pingResults[server.id!.toString()] === undefined
            ? ('unknown' as PingUiStatus)
            : pingResults[server.id!.toString()]
            ? ('online' as PingUiStatus)
            : ('offline' as PingUiStatus),
      })),
    [groupServers, pingResults]
  );

  const statusByServerId = useMemo(
    () =>
      pingStatusRows.reduce<Record<number, PingUiStatus>>((acc, row) => {
        if (row.server.id !== undefined) {
          acc[row.server.id] = row.status;
        }
        return acc;
      }, {}),
    [pingStatusRows]
  );

  const onlineServers = pingStatusRows.filter((row) => row.status === 'online');
  const offlineServers = pingStatusRows.filter((row) => row.status === 'offline');
  const unknownServers = pingStatusRows.filter((row) => row.status === 'unknown');

  if (isLoading) {
    return (
      <AsyncState
        kind="loading"
        title="Loading group details"
        description="Fetching servers, scripts, and membership data."
      />
    );
  }

  if (!group) {
    return (
      <div className="space-y-4">
        <PageHeader title="$ group" subtitle="Details" onBack={goBack} />
        <AsyncState
          kind="error"
          title="Group unavailable"
          description={error ?? 'Unable to open this group.'}
          actionLabel="Back to groups"
          onAction={goBack}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`$ ${group.name}`}
        subtitle={`${groupServers.length} server${groupServers.length !== 1 ? 's' : ''}`}
        onBack={goBack}
        currentLabel={group.name}
      />

      {isDefaultGroup && (
        <div
          className="inline-flex items-center space-x-2 text-green-600 text-xs font-mono bg-gray-900 border border-green-900 rounded px-3 py-2"
          title="Default group is always present"
        >
          <Lock className="w-3 h-3" />
          <span>Default group</span>
        </div>
      )}

      {error && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-2 rounded font-mono text-sm">
          {error}
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <button
          onClick={() => setShowAddServer(true)}
          className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors btn-operator"
        >
          <Plus className="w-4 h-4" />
          <span>Add Server</span>
        </button>
        <button
          onClick={handlePingAll}
          disabled={pinging || groupServers.length === 0}
          className="flex items-center space-x-2 bg-blue-900 text-blue-300 px-4 py-2 rounded font-mono hover:bg-blue-800 transition-colors disabled:opacity-50 btn-operator"
        >
          <Activity className="w-4 h-4" />
          <span>{pinging ? 'Pinging...' : 'Ping All'}</span>
        </button>
        <button
          onClick={() => setShowExecute(true)}
          disabled={groupServers.length === 0}
          className="flex items-center space-x-2 bg-yellow-900 text-yellow-300 px-4 py-2 rounded font-mono hover:bg-yellow-800 transition-colors disabled:opacity-50 btn-operator"
        >
          <Play className="w-4 h-4" />
          <span>Execute Script</span>
        </button>
      </div>

      {!lastPingAt && groupServers.length > 0 && (
        <div className="border border-blue-900 bg-blue-950/30 text-blue-300 px-4 py-3 rounded font-mono text-sm">
          Run <span className="font-bold">Ping All</span> to get explicit availability for each server.
        </div>
      )}

      {lastPingAt && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4 space-y-4 animate-page-enter">
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <h2 className="text-green-400 font-mono font-bold">Ping Results</h2>
            <span className="text-green-700 text-xs font-mono">
              Last check: {new Date(lastPingAt).toLocaleTimeString()}
            </span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="bg-black border border-green-900 rounded p-3">
              <div className="text-green-600 text-xs font-mono">ONLINE</div>
              <div className="text-green-400 font-mono text-2xl font-bold">{onlineServers.length}</div>
            </div>
            <div className="bg-black border border-red-900 rounded p-3">
              <div className="text-red-600 text-xs font-mono">OFFLINE</div>
              <div className="text-red-400 font-mono text-2xl font-bold">{offlineServers.length}</div>
            </div>
            <div className="bg-black border border-yellow-900 rounded p-3">
              <div className="text-yellow-600 text-xs font-mono">UNKNOWN</div>
              <div className="text-yellow-400 font-mono text-2xl font-bold">{unknownServers.length}</div>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="bg-black border border-green-900 rounded p-3">
              <div className="flex items-center gap-2 mb-2">
                <CheckCircle2 className="w-4 h-4 text-green-500" />
                <span className="text-green-400 text-sm font-mono font-bold">Active Servers</span>
              </div>
              {onlineServers.length > 0 ? (
                <ul className="space-y-1">
                  {onlineServers.map(({ server }, index) => (
                    <li
                      key={`online-${server.id}`}
                      className="text-green-500 text-sm font-mono animate-card-stagger"
                      style={{ animationDelay: `${Math.min(index, 10) * 35}ms` }}
                    >
                      {server.hostname} ({server.ipAddress})
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-green-700 text-sm font-mono">No active servers.</p>
              )}
            </div>
            <div className="bg-black border border-red-900 rounded p-3">
              <div className="flex items-center gap-2 mb-2">
                <XCircle className="w-4 h-4 text-red-500" />
                <span className="text-red-400 text-sm font-mono font-bold">Inactive Servers</span>
              </div>
              {offlineServers.length > 0 ? (
                <ul className="space-y-1">
                  {offlineServers.map(({ server }, index) => (
                    <li
                      key={`offline-${server.id}`}
                      className="text-red-500 text-sm font-mono animate-card-stagger"
                      style={{ animationDelay: `${Math.min(index, 10) * 35}ms` }}
                    >
                      {server.hostname} ({server.ipAddress})
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-red-700 text-sm font-mono">No inactive servers.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {showAddServer && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">Add Server to Group</h2>
          {availableServers.length > 0 ? (
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
                    disabled={updatingMembership}
                    className="bg-green-900 text-green-300 px-3 py-1 rounded text-sm font-mono hover:bg-green-800 disabled:opacity-50 btn-operator"
                  >
                    Add
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-green-700 font-mono text-sm">No available servers to add.</p>
          )}
          <button
            onClick={() => setShowAddServer(false)}
            className="mt-4 bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 btn-operator"
          >
            Cancel
          </button>
        </div>
      )}

      {showExecute && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">Execute on All Servers</h2>
          <div className="space-y-2 mb-4">
            {scripts.map((script) => (
              <div
                key={script.id}
                onClick={() => setSelectedScript(script.id!)}
                className={`cursor-pointer bg-black border rounded p-3 font-mono transition-colors card-interactive ${
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
              disabled={!selectedScript || executing}
              className="bg-yellow-900 text-yellow-300 px-4 py-2 rounded font-mono hover:bg-yellow-800 disabled:opacity-50 btn-operator"
            >
              {executing ? 'Executing...' : 'Execute'}
            </button>
            <button
              onClick={() => {
                setShowExecute(false);
                setSelectedScript(null);
              }}
              className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 btn-operator"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {groupServers.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {groupServers.map((server, index) => {
            const status = statusByServerId[server.id!] ?? 'unknown';
            const statusLabel =
              status === 'online' ? 'ONLINE' : status === 'offline' ? 'OFFLINE' : 'UNKNOWN';
            const statusClass =
              status === 'online'
                ? 'border-green-700 text-green-400 bg-green-950/40'
                : status === 'offline'
                ? 'border-red-700 text-red-400 bg-red-950/40'
                : 'border-yellow-700 text-yellow-400 bg-yellow-950/30';
            return (
              <div
                key={server.id}
                className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-card-stagger card-interactive"
                style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
              >
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="text-green-400 font-mono font-bold">{server.hostname}</h3>
                    <p className="text-green-700 text-sm font-mono">{server.ipAddress}</p>
                    <div
                      className={`inline-flex mt-2 px-2 py-1 rounded border text-xs font-mono ${
                        status !== 'unknown' ? 'animate-status-pop' : ''
                      } ${statusClass}`}
                    >
                      {statusLabel}
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      if (!isDefaultGroup) {
                        handleRemoveServer(server);
                      }
                    }}
                    className={`transition-colors ${
                      isDefaultGroup
                        ? 'text-red-900 cursor-not-allowed'
                        : 'text-red-500 hover:text-red-400'
                    }`}
                    disabled={isDefaultGroup || updatingMembership}
                    title={isDefaultGroup ? 'Default group cannot remove servers' : 'Remove server'}
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <div className="space-y-2 text-sm font-mono">
                  <div className="flex justify-between text-green-700">
                    <span>SSH User:</span>
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
      )}

      {groupServers.length === 0 && (
        <AsyncState
          kind="empty"
          title={isDefaultGroup ? 'Default group is empty' : 'No servers in this group'}
          description="Add servers to start pinging and executing scripts in this group."
          actionLabel="Add Server"
          onAction={() => setShowAddServer(true)}
        />
      )}
    </div>
  );
}
