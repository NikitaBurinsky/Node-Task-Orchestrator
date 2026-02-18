import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { List, CheckCircle, XCircle, Clock, Loader } from 'lucide-react';
import { tasksApi } from '../services/api';
import type { TaskDto } from '../types/api';

export function Tasks() {
  const [tasks, setTasks] = useState<TaskDto[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    fetchTasks();
    const interval = setInterval(fetchTasks, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchTasks = async () => {
    try {
      const response = await tasksApi.getAll();
      setTasks(response.data.sort((a, b) => (b.id || 0) - (a.id || 0)));
    } catch (error) {
      console.error('Failed to fetch tasks:', error);
    }
  };

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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-green-500 font-mono">$ tasks</h1>
        <p className="text-green-700 font-mono mt-1">Execution history</p>
      </div>

      <div className="space-y-3">
        {tasks.map((task) => (
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

      {tasks.length === 0 && (
        <div className="text-center py-12">
          <List className="w-16 h-16 text-green-900 mx-auto mb-4" />
          <p className="text-green-700 font-mono">No tasks executed yet</p>
        </div>
      )}
    </div>
  );
}
