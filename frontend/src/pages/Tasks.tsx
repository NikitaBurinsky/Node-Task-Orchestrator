import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { List, CheckCircle, XCircle, Clock, Loader } from 'lucide-react';
import { tasksApi } from '../services/api';
import type { TaskDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { useToast } from '../contexts/ToastContext';

export function Tasks() {
  const [tasks, setTasks] = useState<TaskDto[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | TaskDto['status']>('ALL');
  const [search, setSearch] = useState('');
  const navigate = useNavigate();
  const { showToast } = useToast();

  const fetchTasks = useCallback(async () => {
    try {
      const params =
        statusFilter === 'ALL' || !statusFilter ? undefined : { status: statusFilter };
      const response = await tasksApi.getAll(params);
      setTasks(response.data.sort((a, b) => (b.id || 0) - (a.id || 0)));
    } catch (error) {
      console.error('Failed to fetch tasks:', error);
      showToast('Failed to load tasks.', 'error');
    }
  }, [showToast, statusFilter]);

  useEffect(() => {
    fetchTasks();
    const interval = setInterval(fetchTasks, 5000);
    return () => clearInterval(interval);
  }, [fetchTasks]);

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

  const filteredTasks = tasks.filter((task) => {
    if (!search.trim()) return true;
    const term = search.trim();
    const serverId = task.serverId?.toString() ?? '';
    const scriptId = task.scriptId?.toString() ?? '';
    return serverId.includes(term) || scriptId.includes(term);
  });

  const hasActiveFilters = statusFilter !== 'ALL' || search.trim().length > 0;
  const clearFilters = () => {
    setStatusFilter('ALL');
    setSearch('');
  };

  return (
    <div className="space-y-6">
      <PageHeader title="$ tasks" subtitle="Execution history" />

      <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-green-500 font-mono text-sm mb-2">
              Status
            </label>
            <select
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as 'ALL' | TaskDto['status'])
              }
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
          <div className="md:col-span-2">
            <label className="block text-green-500 font-mono text-sm mb-2">
              Search (Server ID or Script ID)
            </label>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="e.g. 12"
              className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
            />
          </div>
        </div>
        <div className="flex justify-end mt-4">
          <button
            type="button"
            onClick={clearFilters}
            disabled={!hasActiveFilters}
            className={`px-3 py-2 rounded font-mono text-sm transition-colors ${
              hasActiveFilters
                ? 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                : 'bg-gray-900 text-gray-600 cursor-not-allowed'
            }`}
          >
            Clear filters
          </button>
        </div>
      </div>

      <div className="space-y-3">
        {filteredTasks.map((task) => (
          <div
            key={task.id}
            onClick={() => navigate(`/tasks/${task.id}`)}
            className={`bg-gray-900 border rounded-lg p-4 cursor-pointer hover:border-green-500 transition-colors ${getStatusColor(
              task.status
            )}`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                {getStatusIcon(task.status)}
                <div>
                  <div className="flex items-center space-x-3">
                    <span className="text-green-400 font-mono font-bold">
                      Task #{task.id}
                    </span>
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
                  {task.startedAt
                    ? new Date(task.startedAt).toLocaleString()
                    : 'Not started'}
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

      {filteredTasks.length === 0 && hasActiveFilters && (
        <div className="text-center py-12">
          <List className="w-16 h-16 text-green-900 mx-auto mb-4" />
          <p className="text-green-700 font-mono">No tasks match filters</p>
        </div>
      )}

      {tasks.length === 0 && !hasActiveFilters && (
        <div className="text-center py-12">
          <List className="w-16 h-16 text-green-900 mx-auto mb-4" />
          <p className="text-green-700 font-mono">No tasks executed yet</p>
        </div>
      )}
    </div>
  );
}
