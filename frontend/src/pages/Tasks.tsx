import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { List, CheckCircle, XCircle, Clock, Loader } from 'lucide-react';
import { tasksApi } from '../services/api';
import type { TaskDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useToast } from '../contexts/ToastContext';
import { useAdaptivePolling } from '../hooks/useAdaptivePolling';
import { LiveStatusStrip } from '../components/LiveStatusStrip';

type TaskFilterStatus = 'ALL' | 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
type TaskSortOrder = 'newest' | 'oldest';

function normalizeStatus(value: string | null): TaskFilterStatus {
  if (!value) return 'ALL';

  const allowed: TaskFilterStatus[] = ['ALL', 'PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED'];
  return allowed.includes(value as TaskFilterStatus) ? (value as TaskFilterStatus) : 'ALL';
}

export function Tasks() {
  const [tasks, setTasks] = useState<TaskDto[]>([]);
  const [listError, setListError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const statusFilter = normalizeStatus(searchParams.get('status'));
  const search = searchParams.get('q') ?? '';
  const sortOrder: TaskSortOrder = searchParams.get('sort') === 'oldest' ? 'oldest' : 'newest';

  const setQueryParam = useCallback(
    (name: string, value: string | null) => {
      const nextParams = new URLSearchParams(searchParams);
      if (!value || !value.trim()) {
        nextParams.delete(name);
      } else {
        nextParams.set(name, value);
      }
      setSearchParams(nextParams, { replace: true });
    },
    [searchParams, setSearchParams]
  );

  const clearFilters = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true });
  }, [setSearchParams]);

  const fetchTasks = useCallback(async () => {
    const params = statusFilter === 'ALL' ? undefined : { status: statusFilter };
    const response = await tasksApi.getAll(params);
    const sortedTasks = [...response.data].sort((a, b) => {
      const left = a.id ?? 0;
      const right = b.id ?? 0;
      return sortOrder === 'newest' ? right - left : left - right;
    });
    setTasks(sortedTasks);
  }, [sortOrder, statusFilter]);

  const { lastSuccessAt, consecutiveErrors, isPaused } = useAdaptivePolling(fetchTasks, {
    enabled: true,
    baseIntervalMs: 5000,
    maxIntervalMs: 30000,
    runImmediately: true,
    onSuccess: () => setListError(null),
    onError: (error, attempt) => {
      console.error('Failed to fetch tasks:', error);
      setListError('Failed to load tasks. Auto-retry is enabled.');
      if (attempt === 1) {
        showToast('Failed to load tasks. Retrying with backoff.', 'error');
      }
    },
  });

  const retryNow = useCallback(async () => {
    try {
      await fetchTasks();
      setListError(null);
    } catch (error) {
      console.error('Retry failed:', error);
      setListError('Failed to load tasks. Auto-retry is enabled.');
      showToast('Retry failed. Polling continues.', 'error');
    }
  }, [fetchTasks, showToast]);

  useEffect(() => {
    const handleSearchFocus = () => {
      searchInputRef.current?.focus();
      searchInputRef.current?.select();
    };

    const handlePollingRefresh = () => {
      void retryNow();
    };

    window.addEventListener('nto:focus-search', handleSearchFocus as EventListener);
    window.addEventListener('nto:poll-refresh', handlePollingRefresh as EventListener);
    return () => {
      window.removeEventListener('nto:focus-search', handleSearchFocus as EventListener);
      window.removeEventListener('nto:poll-refresh', handlePollingRefresh as EventListener);
    };
  }, [retryNow]);

  const filteredTasks = useMemo(
    () =>
      tasks.filter((task) => {
        if (!search.trim()) return true;
        const term = search.trim();
        const serverId = task.serverId?.toString() ?? '';
        const scriptId = task.scriptId?.toString() ?? '';
        return serverId.includes(term) || scriptId.includes(term);
      }),
    [tasks, search]
  );

  const hasActiveFilters =
    statusFilter !== 'ALL' || search.trim().length > 0 || sortOrder !== 'newest';
  const isInitialLoading = !lastSuccessAt && !listError && tasks.length === 0;

  const getStatusIcon = (status: TaskDto['status']) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'FAILED':
        return <XCircle className="w-5 h-5 text-red-500" />;
      case 'RUNNING':
        return <Loader className="w-5 h-5 text-yellow-500 animate-spin" />;
      case 'PENDING':
        return <Clock className="w-5 h-5 text-blue-500" />;
      default:
        return <List className="w-5 h-5 text-gray-500" />;
    }
  };

  const getStatusColor = (status: TaskDto['status']) => {
    switch (status) {
      case 'SUCCESS':
        return 'border-green-700 bg-green-900 bg-opacity-10';
      case 'FAILED':
        return 'border-red-700 bg-red-900 bg-opacity-10';
      case 'RUNNING':
        return 'border-yellow-700 bg-yellow-900 bg-opacity-10';
      case 'PENDING':
        return 'border-blue-700 bg-blue-900 bg-opacity-10';
      default:
        return 'border-gray-700';
    }
  };

  const getStatusText = (status: TaskDto['status']) => {
    switch (status) {
      case 'SUCCESS':
        return 'text-green-400';
      case 'FAILED':
        return 'text-red-400';
      case 'RUNNING':
        return 'text-yellow-400';
      case 'PENDING':
        return 'text-blue-400';
      default:
        return 'text-gray-400';
    }
  };

  const liveStripState = isPaused ? 'paused' : consecutiveErrors > 0 ? 'backoff' : 'active';

  return (
    <div className="space-y-6">
      <PageHeader title="$ tasks" subtitle="Execution history" />

      <div className="bg-gray-900 border border-green-900 rounded-lg p-4 animate-page-enter">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-green-500 font-mono text-sm mb-2">Status</label>
            <select
              value={statusFilter}
              onChange={(event) => {
                const nextStatus = event.target.value as TaskFilterStatus;
                setQueryParam('status', nextStatus === 'ALL' ? null : nextStatus);
              }}
              className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
            >
              <option value="ALL">ALL</option>
              <option value="PENDING">PENDING</option>
              <option value="RUNNING">RUNNING</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILED">FAILED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>
          <div>
            <label className="block text-green-500 font-mono text-sm mb-2">Sort</label>
            <select
              value={sortOrder}
              onChange={(event) => {
                const nextSort = event.target.value as TaskSortOrder;
                setQueryParam('sort', nextSort === 'newest' ? null : nextSort);
              }}
              className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
            >
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
            </select>
          </div>
          <div className="md:col-span-2">
            <label className="block text-green-500 font-mono text-sm mb-2">
              Search (Server ID or Script ID)
            </label>
            <input
              ref={searchInputRef}
              type="text"
              value={search}
              onChange={(event) => setQueryParam('q', event.target.value)}
              placeholder="e.g. 12"
              className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
            />
          </div>
        </div>
        <div className="flex items-center justify-between mt-4 gap-3 flex-wrap">
          <button
            type="button"
            onClick={clearFilters}
            disabled={!hasActiveFilters}
            className={`px-3 py-2 rounded font-mono text-sm transition-colors btn-operator ${
              hasActiveFilters
                ? 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                : 'bg-gray-900 text-gray-600 cursor-not-allowed'
            }`}
          >
            Clear filters
          </button>
        </div>
      </div>

      <LiveStatusStrip
        state={liveStripState}
        lastUpdatedAt={lastSuccessAt}
        backoffAttempt={consecutiveErrors}
        onRefresh={retryNow}
        refreshLabel="Refresh tasks"
      />

      {isInitialLoading && (
        <AsyncState
          kind="loading"
          title="Loading tasks"
          description="Fetching execution history from the backend."
        />
      )}

      {!isInitialLoading && listError && tasks.length === 0 && (
        <AsyncState
          kind="error"
          title="Task feed unavailable"
          description={listError}
          actionLabel="Retry now"
          onAction={retryNow}
        />
      )}

      {!isInitialLoading && filteredTasks.length > 0 && (
        <div className="space-y-3">
          {filteredTasks.map((task, index) => (
            <div
              key={task.id}
              onClick={() => navigate(`/tasks/${task.id}`)}
              className={`bg-gray-900 border rounded-lg p-4 cursor-pointer hover:border-green-500 ${getStatusColor(
                task.status
              )} animate-card-stagger card-interactive`}
              style={{ animationDelay: `${Math.min(index, 10) * 45}ms` }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  {getStatusIcon(task.status)}
                  <div>
                    <div className="flex items-center space-x-3">
                      <span className="text-green-400 font-mono font-bold">Task #{task.id}</span>
                      <span className={`font-mono text-sm ${getStatusText(task.status)}`}>
                        {task.status}
                      </span>
                    </div>
                    <div className="text-green-700 text-sm font-mono mt-1">
                      Server ID: {task.serverId} | Script ID: {task.scriptId}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-green-600 text-sm font-mono">
                    {task.startedAt ? new Date(task.startedAt).toLocaleString() : 'Not started'}
                  </div>
                  {task.finishedAt && (
                    <div className="text-green-700 text-xs font-mono mt-1">
                      Finished: {new Date(task.finishedAt).toLocaleTimeString()}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {!isInitialLoading && filteredTasks.length === 0 && hasActiveFilters && (
        <AsyncState
          kind="empty"
          title="No tasks match current filters"
          description="Try a different status, search term, or sort order."
          actionLabel="Reset filters"
          onAction={clearFilters}
        />
      )}

      {!isInitialLoading && !hasActiveFilters && tasks.length === 0 && (
        <AsyncState
          kind="empty"
          title="No tasks executed yet"
          description="Run a script on a server or group to populate execution history."
        />
      )}
    </div>
  );
}
