import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Terminal, CheckCircle, XCircle, Clock, Loader } from 'lucide-react';
import { tasksApi, serversApi, scriptsApi } from '../services/api';
import type { TaskDto, ServerDto, ScriptDto } from '../types/api';

export function TaskTerminal() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [task, setTask] = useState<TaskDto | null>(null);
  const [server, setServer] = useState<ServerDto | null>(null);
  const [script, setScript] = useState<ScriptDto | null>(null);

  useEffect(() => {
    if (id) {
      fetchTask();
      const interval = setInterval(fetchTask, 2000);
      return () => clearInterval(interval);
    }
  }, [id]);

  const fetchTask = async () => {
    try {
      const response = await tasksApi.getById(parseInt(id!));
      const taskData = response.data;
      setTask(taskData);

      if (taskData.serverId && !server) {
        const serverRes = await serversApi.getById(taskData.serverId);
        setServer(serverRes.data);
      }

      if (taskData.scriptId && !script) {
        const scriptRes = await scriptsApi.getById(taskData.scriptId);
        setScript(scriptRes.data);
      }
    } catch (error) {
      console.error('Failed to fetch task:', error);
    }
  };

  const getStatusIcon = () => {
    switch (task?.status) {
      case 'SUCCESS':
        return <CheckCircle className="w-6 h-6 text-green-500" />;
      case 'FAILED':
        return <XCircle className="w-6 h-6 text-red-500" />;
      case 'RUNNING':
        return <Loader className="w-6 h-6 text-yellow-500 animate-spin" />;
      case 'PENDING':
        return <Clock className="w-6 h-6 text-blue-500" />;
      default:
        return <Terminal className="w-6 h-6 text-gray-500" />;
    }
  };

  const getStatusColor = () => {
    switch (task?.status) {
      case 'SUCCESS':
        return 'text-green-500';
      case 'FAILED':
        return 'text-red-500';
      case 'RUNNING':
        return 'text-yellow-500';
      case 'PENDING':
        return 'text-blue-500';
      default:
        return 'text-gray-500';
    }
  };

  if (!task) {
    return (
      <div className="text-center py-12">
        <div className="text-green-500 font-mono">Loading task...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <button
          onClick={() => navigate('/tasks')}
          className="text-green-500 hover:text-green-400 transition-colors"
        >
          <ArrowLeft className="w-6 h-6" />
        </button>
        <div className="flex items-center space-x-3">
          {getStatusIcon()}
          <div>
            <h1 className="text-2xl font-bold text-green-500 font-mono">
              Task #{task.id}
            </h1>
            <p className={`font-mono text-sm ${getStatusColor()}`}>
              Status: {task.status}
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
          <div className="text-green-700 text-xs font-mono mb-1">SERVER</div>
          <div className="text-green-400 font-mono font-bold">
            {server?.hostname || 'Unknown'}
          </div>
          <div className="text-green-600 text-sm font-mono">{server?.ipAddress}</div>
        </div>
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
          <div className="text-green-700 text-xs font-mono mb-1">SCRIPT</div>
          <div className="text-green-400 font-mono font-bold">
            {script?.name || 'Unknown'}
          </div>
          <div className="text-green-600 text-sm font-mono">
            {script?.ownerName || 'N/A'}
          </div>
        </div>
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
          <div className="text-green-700 text-xs font-mono mb-1">STARTED</div>
          <div className="text-green-400 font-mono text-sm">
            {task.startedAt
              ? new Date(task.startedAt).toLocaleString()
              : 'Not started'}
          </div>
          {task.finishedAt && (
            <>
              <div className="text-green-700 text-xs font-mono mt-2 mb-1">FINISHED</div>
              <div className="text-green-400 font-mono text-sm">
                {new Date(task.finishedAt).toLocaleString()}
              </div>
            </>
          )}
        </div>
      </div>

      <div className="bg-black border border-green-900 rounded-lg overflow-hidden">
        <div className="bg-gray-900 border-b border-green-900 px-4 py-2 flex items-center space-x-2">
          <Terminal className="w-4 h-4 text-green-500" />
          <span className="text-green-500 font-mono text-sm font-bold">
            TERMINAL OUTPUT
          </span>
          {(task.status === 'RUNNING' || task.status === 'PENDING') && (
            <div className="flex items-center space-x-2 ml-auto">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-green-600 text-xs font-mono">LIVE</span>
            </div>
          )}
        </div>
        <div className="p-4 h-96 overflow-y-auto">
          <pre className="text-green-400 font-mono text-sm whitespace-pre-wrap">
            {task.output || '> Waiting for output...'}
          </pre>
          {task.status === 'RUNNING' && (
            <div className="flex items-center space-x-2 mt-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-green-600 font-mono text-sm">Processing...</span>
            </div>
          )}
        </div>
      </div>

      {task.status === 'SUCCESS' && (
        <div className="bg-green-900 bg-opacity-20 border border-green-700 rounded-lg p-4">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-5 h-5 text-green-500" />
            <span className="text-green-400 font-mono font-bold">
              Task completed successfully
            </span>
          </div>
        </div>
      )}

      {task.status === 'FAILED' && (
        <div className="bg-red-900 bg-opacity-20 border border-red-700 rounded-lg p-4">
          <div className="flex items-center space-x-2">
            <XCircle className="w-5 h-5 text-red-500" />
            <span className="text-red-400 font-mono font-bold">Task failed</span>
          </div>
        </div>
      )}
    </div>
  );
}
