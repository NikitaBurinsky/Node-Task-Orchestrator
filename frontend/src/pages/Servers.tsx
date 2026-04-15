import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Plus, Activity, Server as ServerIcon, Trash2, CheckSquare, Square } from 'lucide-react';
import { serversApi } from '../services/api';
import type { ServerDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useToast } from '../contexts/ToastContext';
import { useConfirmDialog } from '../contexts/ConfirmDialogContext';
import { useActivityFeed } from '../contexts/ActivityFeedContext';

type PingStatus = 'checking' | 'online' | 'offline';

export function Servers() {
  const [servers, setServers] = useState<ServerDto[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [pingStatus, setPingStatus] = useState<Record<number, PingStatus>>({});
  const [pingErrors, setPingErrors] = useState<Record<number, boolean>>({});
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isBulkDeleting, setIsBulkDeleting] = useState(false);
  const [isBulkPinging, setIsBulkPinging] = useState(false);
  const [formData, setFormData] = useState<ServerDto>({
    hostname: '',
    ipAddress: '',
    port: 22,
    username: '',
    password: '',
  });
  const [searchParams, setSearchParams] = useSearchParams();
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const { showToast } = useToast();
  const { confirm } = useConfirmDialog();
  const { addActivity } = useActivityFeed();

  const isCreateRequested = searchParams.get('create') === '1';
  const searchQuery = searchParams.get('q') ?? '';

  const setQueryParam = useCallback(
    (name: string, value: string | null) => {
      const next = new URLSearchParams(searchParams);
      if (!value || !value.trim()) {
        next.delete(name);
      } else {
        next.set(name, value.trim());
      }
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams]
  );

  const fetchServers = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await serversApi.getAll();
      setServers(response.data);
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch servers:', fetchError);
      setError('Failed to load servers.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchServers();
  }, [fetchServers]);

  useEffect(() => {
    if (isCreateRequested) {
      setShowAddForm(true);
    }
  }, [isCreateRequested]);

  useEffect(() => {
    setSelectedIds((current) =>
      current.filter((id) => servers.some((server) => server.id === id))
    );
  }, [servers]);

  useEffect(() => {
    const handler = () => {
      searchInputRef.current?.focus();
      searchInputRef.current?.select();
    };
    window.addEventListener('nto:focus-search', handler as EventListener);
    return () => {
      window.removeEventListener('nto:focus-search', handler as EventListener);
    };
  }, []);

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

  const clearPingStatus = (serverId: number) => {
    setTimeout(() => {
      setPingStatus((prev) => {
        const next = { ...prev };
        delete next[serverId];
        return next;
      });
    }, 3000);
  };

  const removePingStatus = (serverId: number) => {
    setPingStatus((prev) => {
      const next = { ...prev };
      delete next[serverId];
      return next;
    });
  };

  const clearPingError = (serverId: number) => {
    setPingErrors((prev) => {
      const next = { ...prev };
      delete next[serverId];
      return next;
    });
  };

  const schedulePingErrorClear = (serverId: number) => {
    setTimeout(() => {
      clearPingError(serverId);
    }, 3000);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
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
      addActivity({
        title: 'Server created',
        details: formData.hostname,
        status: 'success',
      });
      await fetchServers();
    } catch (submitError) {
      console.error('Failed to create server:', submitError);
      setError('Failed to create server.');
      showToast('Failed to create server.', 'error');
      addActivity({
        title: 'Server create failed',
        details: formData.hostname || 'Unknown hostname',
        status: 'error',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (serverId: number, serverName: string) => {
    const confirmed = await confirm({
      title: 'Delete server?',
      description: `${serverName} will be permanently removed from your infrastructure inventory.`,
      confirmText: 'Delete',
      cancelText: 'Keep',
      tone: 'danger',
    });

    if (!confirmed) {
      return;
    }

    try {
      await serversApi.delete(serverId);
      removePingStatus(serverId);
      clearPingError(serverId);
      setError(null);
      showToast('Server deleted.', 'success');
      addActivity({
        title: 'Server deleted',
        details: serverName,
        status: 'success',
      });
      await fetchServers();
    } catch (deleteError) {
      console.error('Failed to delete server:', deleteError);
      setError('Failed to delete server.');
      showToast('Failed to delete server.', 'error');
      addActivity({
        title: 'Server delete failed',
        details: serverName,
        status: 'error',
      });
    }
  };

  const handlePing = async (serverId: number, hostname: string) => {
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
      addActivity({
        title: `Ping ${alive ? 'success' : 'offline'}`,
        details: hostname,
        status: alive ? 'success' : 'info',
      });
      clearPingStatus(serverId);
    } catch (pingError) {
      console.error('Failed to ping server:', pingError);
      removePingStatus(serverId);
      setPingErrors((prev) => ({ ...prev, [serverId]: true }));
      schedulePingErrorClear(serverId);
      setError('Failed to ping server.');
      showToast('Failed to ping server.', 'error');
      addActivity({
        title: 'Ping failed',
        details: hostname,
        status: 'error',
      });
    }
  };

  const filteredServers = useMemo(() => {
    const term = searchQuery.trim().toLowerCase();
    if (!term) {
      return servers;
    }

    return servers.filter((server) =>
      [server.hostname, server.ipAddress, server.username].join(' ').toLowerCase().includes(term)
    );
  }, [searchQuery, servers]);

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);
  const allVisibleSelected =
    filteredServers.length > 0 && filteredServers.every((server) => selectedSet.has(server.id ?? -1));

  const toggleSelect = (serverId: number, selected: boolean) => {
    setSelectedIds((current) => {
      if (selected) {
        if (current.includes(serverId)) {
          return current;
        }
        return [...current, serverId];
      }
      return current.filter((id) => id !== serverId);
    });
  };

  const toggleSelectVisible = () => {
    if (allVisibleSelected) {
      const visibleIds = new Set(filteredServers.map((server) => server.id).filter(Boolean) as number[]);
      setSelectedIds((current) => current.filter((id) => !visibleIds.has(id)));
      return;
    }

    const toAdd = filteredServers.map((server) => server.id).filter(Boolean) as number[];
    setSelectedIds((current) => Array.from(new Set([...current, ...toAdd])));
  };

  const clearSelection = () => setSelectedIds([]);

  const handleBulkPing = async () => {
    if (selectedIds.length === 0 || isBulkPinging) {
      return;
    }

    setIsBulkPinging(true);
    selectedIds.forEach((id) => {
      setPingStatus((prev) => ({ ...prev, [id]: 'checking' }));
      clearPingError(id);
    });

    const byId = new Map(servers.map((server) => [server.id, server]));
    const results = await Promise.allSettled(
      selectedIds.map(async (id) => {
        const response = await serversApi.ping(id);
        return { id, alive: response.data.alive };
      })
    );

    let online = 0;
    let offline = 0;
    let failed = 0;

    results.forEach((result, index) => {
      const id = selectedIds[index];
      if (result.status === 'fulfilled') {
        const nextStatus: PingStatus = result.value.alive ? 'online' : 'offline';
        setPingStatus((prev) => ({ ...prev, [id]: nextStatus }));
        if (result.value.alive) {
          online += 1;
        } else {
          offline += 1;
        }
        clearPingStatus(id);
      } else {
        failed += 1;
        removePingStatus(id);
        setPingErrors((prev) => ({ ...prev, [id]: true }));
        schedulePingErrorClear(id);
      }
    });

    const summary = `Bulk ping finished. Online: ${online}, Offline: ${offline}, Errors: ${failed}.`;
    showToast(summary, failed > 0 ? 'error' : offline > 0 ? 'info' : 'success');
    addActivity({
      title: 'Bulk ping servers',
      details: summary,
      status: failed > 0 ? 'error' : 'success',
    });

    if (failed > 0) {
      const failedNames = selectedIds
        .filter((_, idx) => results[idx].status === 'rejected')
        .map((id) => byId.get(id)?.hostname ?? `#${id}`)
        .join(', ');
      setError(`Failed to ping: ${failedNames}`);
    } else {
      setError(null);
    }

    setIsBulkPinging(false);
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0 || isBulkDeleting) {
      return;
    }

    const confirmed = await confirm({
      title: `Delete ${selectedIds.length} server${selectedIds.length === 1 ? '' : 's'}?`,
      description: 'Selected servers will be permanently removed.',
      confirmText: 'Delete selected',
      cancelText: 'Cancel',
      tone: 'danger',
    });

    if (!confirmed) {
      return;
    }

    setIsBulkDeleting(true);

    const byId = new Map(servers.map((server) => [server.id, server.hostname]));
    const results = await Promise.allSettled(selectedIds.map((id) => serversApi.delete(id)));

    const successIds: number[] = [];
    const failedNames: string[] = [];
    results.forEach((result, index) => {
      const id = selectedIds[index];
      if (result.status === 'fulfilled') {
        successIds.push(id);
        removePingStatus(id);
        clearPingError(id);
      } else {
        failedNames.push(byId.get(id) ?? `#${id}`);
      }
    });

    if (successIds.length > 0) {
      showToast(
        `Deleted ${successIds.length} server${successIds.length === 1 ? '' : 's'}.`,
        failedNames.length > 0 ? 'info' : 'success'
      );
      addActivity({
        title: 'Bulk delete servers',
        details: `Deleted ${successIds.length}, failed ${failedNames.length}`,
        status: failedNames.length > 0 ? 'error' : 'success',
      });
    }

    if (failedNames.length > 0) {
      setError(`Failed to delete: ${failedNames.join(', ')}`);
      showToast(`Failed to delete ${failedNames.length} server(s).`, 'error');
    } else {
      setError(null);
    }

    setSelectedIds((current) => current.filter((id) => !successIds.includes(id)));
    await fetchServers();
    setIsBulkDeleting(false);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="$ servers"
        subtitle="Manage your infrastructure"
        actions={
          <button
            onClick={showAddForm ? closeCreateForm : openCreateForm}
            className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors btn-operator"
          >
            <Plus className="w-4 h-4" />
            <span>{showAddForm ? 'Close Form' : 'Add Server'}</span>
          </button>
        }
      />

      <div className="bg-gray-900 border border-green-900 rounded-lg p-4 animate-page-enter">
        <div className="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-3">
          <div>
            <label className="block text-green-500 font-mono text-sm mb-2">Search servers</label>
            <input
              ref={searchInputRef}
              type="text"
              value={searchQuery}
              onChange={(event) => setQueryParam('q', event.target.value)}
              placeholder="hostname, ip, user"
              className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
            />
          </div>
          <button
            type="button"
            onClick={toggleSelectVisible}
            disabled={filteredServers.length === 0}
            className="self-end inline-flex items-center justify-center gap-2 px-3 py-2 rounded font-mono text-sm bg-gray-800 text-gray-300 hover:bg-gray-700 disabled:opacity-50 btn-operator"
          >
            {allVisibleSelected ? <Square className="w-4 h-4" /> : <CheckSquare className="w-4 h-4" />}
            <span>{allVisibleSelected ? 'Unselect Visible' : 'Select Visible'}</span>
          </button>
        </div>
      </div>

      {selectedIds.length > 0 && (
        <div className="bg-gray-900 border border-green-700 rounded-lg p-4 flex items-center justify-between gap-3 flex-wrap animate-page-enter">
          <p className="text-green-300 font-mono text-sm">
            {selectedIds.length} server{selectedIds.length === 1 ? '' : 's'} selected
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleBulkPing}
              disabled={isBulkPinging}
              className="inline-flex items-center gap-2 px-3 py-2 rounded font-mono text-sm bg-blue-900 text-blue-300 hover:bg-blue-800 disabled:opacity-50 btn-operator"
            >
              <Activity className="w-4 h-4" />
              <span>{isBulkPinging ? 'Pinging...' : 'Bulk Ping'}</span>
            </button>
            <button
              type="button"
              onClick={handleBulkDelete}
              disabled={isBulkDeleting}
              className="inline-flex items-center gap-2 px-3 py-2 rounded font-mono text-sm bg-red-900 text-red-200 hover:bg-red-800 disabled:opacity-50 btn-operator"
            >
              <Trash2 className="w-4 h-4" />
              <span>{isBulkDeleting ? 'Deleting...' : 'Bulk Delete'}</span>
            </button>
            <button
              type="button"
              onClick={clearSelection}
              className="px-3 py-2 rounded font-mono text-sm bg-gray-800 text-gray-300 hover:bg-gray-700 btn-operator"
            >
              Clear
            </button>
          </div>
        </div>
      )}

      {error && servers.length > 0 && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-2 rounded font-mono text-sm">
          {error}
        </div>
      )}

      {showAddForm && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">New Server</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">Hostname</label>
                <input
                  type="text"
                  value={formData.hostname}
                  onChange={(event) =>
                    setFormData({
                      ...formData,
                      hostname: event.target.value,
                    })
                  }
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">IP Address</label>
                <input
                  type="text"
                  value={formData.ipAddress}
                  onChange={(event) =>
                    setFormData({
                      ...formData,
                      ipAddress: event.target.value,
                    })
                  }
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  pattern="^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">SSH Username</label>
                <input
                  type="text"
                  value={formData.username}
                  onChange={(event) =>
                    setFormData({
                      ...formData,
                      username: event.target.value,
                    })
                  }
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                  required
                />
              </div>
              <div>
                <label className="block text-green-500 font-mono text-sm mb-2">Port</label>
                <input
                  type="number"
                  value={formData.port}
                  onChange={(event) =>
                    setFormData({
                      ...formData,
                      port: parseInt(event.target.value, 10),
                    })
                  }
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
                  onChange={(event) =>
                    setFormData({
                      ...formData,
                      password: event.target.value,
                    })
                  }
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                />
              </div>
            </div>
            <div className="flex space-x-3">
              <button
                type="submit"
                disabled={isSubmitting}
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed btn-operator"
              >
                {isSubmitting ? 'Creating...' : 'Create'}
              </button>
              <button
                type="button"
                onClick={closeCreateForm}
                className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 transition-colors btn-operator"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {isLoading && !showAddForm && (
        <AsyncState
          kind="loading"
          title="Loading servers"
          description="Reading current server inventory."
        />
      )}

      {!isLoading && error && servers.length === 0 && !showAddForm && (
        <AsyncState
          kind="error"
          title="Server list unavailable"
          description={error}
          actionLabel="Retry"
          onAction={fetchServers}
        />
      )}

      {!isLoading && filteredServers.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredServers.map((server, index) => {
            const status = pingStatus[server.id!];
            const hasPingError = Boolean(pingErrors[server.id!]);
            const isSelected = selectedSet.has(server.id!);
            return (
              <div
                key={server.id}
                className={`bg-gray-900 border rounded-lg p-6 animate-card-stagger card-interactive ${
                  hasPingError
                    ? 'border-red-700 bg-red-950/30 hover:border-red-600'
                    : 'border-green-900 hover:border-green-700'
                }`}
                style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
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
                    <label className="inline-flex items-center gap-2 text-green-600 text-xs font-mono cursor-pointer">
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={(event) => toggleSelect(server.id!, event.target.checked)}
                        className="w-4 h-4 bg-black border border-green-800 rounded"
                      />
                      Pick
                    </label>
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
                      onClick={() => handleDelete(server.id!, server.hostname)}
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
                  onClick={() => handlePing(server.id!, server.hostname)}
                  disabled={status === 'checking'}
                  className={`w-full flex items-center justify-center space-x-2 px-3 py-2 rounded font-mono text-sm transition-colors disabled:opacity-50 btn-operator ${
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
      )}

      {!isLoading && filteredServers.length === 0 && servers.length > 0 && !showAddForm && (
        <AsyncState
          kind="empty"
          title="No servers match current search"
          description="Try another search query."
        />
      )}

      {!isLoading && servers.length === 0 && !showAddForm && !error && (
        <AsyncState
          kind="empty"
          title="No servers configured"
          description="Add your first server to start running scripts."
          actionLabel="Add Server"
          onAction={openCreateForm}
        />
      )}
    </div>
  );
}
