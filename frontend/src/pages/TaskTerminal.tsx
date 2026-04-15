import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Terminal, CheckCircle, XCircle, Clock, Loader, ArrowRight } from 'lucide-react';
import { tasksApi, serversApi, scriptsApi } from '../services/api';
import type { TaskDto, ServerDto, ScriptDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useSafeBack } from '../hooks/useSafeBack';
import { useAdaptivePolling } from '../hooks/useAdaptivePolling';
import { LiveStatusStrip } from '../components/LiveStatusStrip';

export function TaskTerminal() {
  const { id } = useParams<{ id: string }>();
  const goBack = useSafeBack('/tasks');
  const [task, setTask] = useState<TaskDto | null>(null);
  const [server, setServer] = useState<ServerDto | null>(null);
  const [script, setScript] = useState<ScriptDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [statusTimeline, setStatusTimeline] = useState<Array<{ status: string; at: number }>>([]);

  const fetchTask = useCallback(async () => {
    if (!id) return;

    const response = await tasksApi.getById(parseInt(id, 10));
    const taskData = response.data;
    setTask((current) => {
      const nextStatus = taskData.status ?? 'UNKNOWN';
      if (nextStatus && current?.status !== nextStatus) {
        setStatusTimeline((timeline) => {
          const alreadyFirst = timeline[0]?.status === nextStatus;
          if (alreadyFirst) {
            return timeline;
          }
          return [{ status: nextStatus, at: Date.now() }, ...timeline].slice(0, 12);
        });
      }
      return taskData;
    });

    if (taskData.serverId && taskData.serverId !== server?.id) {
      const serverRes = await serversApi.getById(taskData.serverId);
      setServer(serverRes.data);
    }

    if (taskData.scriptId && taskData.scriptId !== script?.id) {
      const scriptRes = await scriptsApi.getById(taskData.scriptId);
      setScript(scriptRes.data);
    }
  }, [id, script?.id, server?.id]);

  const { lastSuccessAt, consecutiveErrors, isPaused } = useAdaptivePolling(fetchTask, {
    enabled: Boolean(id),
    baseIntervalMs: 2000,
    maxIntervalMs: 12000,
    runImmediately: true,
    onSuccess: () => setError(null),
    onError: (fetchError) => {
      console.error('Failed to fetch task:', fetchError);
      setError('Failed to load task details. Auto-retry is enabled.');
    },
  });

  const refreshNow = useCallback(async () => {
    try {
      await fetchTask();
      setError(null);
    } catch (fetchError) {
      console.error('Manual refresh failed:', fetchError);
      setError('Failed to load task details. Auto-retry is enabled.');
    }
  }, [fetchTask]);

  useEffect(() => {
    const handleRefresh = () => {
      void refreshNow();
    };

    window.addEventListener('nto:poll-refresh', handleRefresh as EventListener);
    return () => {
      window.removeEventListener('nto:poll-refresh', handleRefresh as EventListener);
    };
  }, [refreshNow]);

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

  if (!task && !error) {
    return (
      <AsyncState
        kind="loading"
        title="Loading task terminal"
        description="Fetching output and execution metadata."
      />
    );
  }

  if (error && !task) {
    return (
      <div className="space-y-4">
        <PageHeader title="$ task" subtitle="Execution details" onBack={goBack} />
        <AsyncState
          kind="error"
          title="Task details unavailable"
          description={error}
          actionLabel="Back to tasks"
          onAction={goBack}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`$ task #${task?.id}`}
        subtitle={task?.status ? `Status: ${task.status}` : 'Execution details'}
        onBack={goBack}
        currentLabel={task?.id ? `Task #${task.id}` : 'Task'}
      />

      <LiveStatusStrip
        state={isPaused ? 'paused' : consecutiveErrors > 0 ? 'backoff' : 'active'}
        lastUpdatedAt={lastSuccessAt}
        backoffAttempt={consecutiveErrors}
        onRefresh={refreshNow}
        refreshLabel="Refresh task"
      />

      {error && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-3 rounded font-mono text-sm">
          {error}
        </div>
      )}

      <div className="flex items-center space-x-3">
        {getStatusIcon()}
        <p className={`font-mono text-sm ${getStatusColor()}`}>Status: {task?.status}</p>
      </div>

      {statusTimeline.length > 0 && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4 animate-page-enter">
          <h2 className="text-green-500 font-mono text-sm font-bold mb-3">Status Timeline</h2>
          <div className="space-y-2">
            {statusTimeline.map((item, index) => (
              <div
                key={`${item.status}-${item.at}`}
                className="flex items-center justify-between bg-black border border-green-900 rounded px-3 py-2 text-xs font-mono"
              >
                <div className="flex items-center gap-2 text-green-400">
                  <span>{item.status}</span>
                  {index < statusTimeline.length - 1 && <ArrowRight className="w-3 h-3 text-green-700" />}
                </div>
                <span className="text-green-700">{new Date(item.at).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
          <div className="text-green-700 text-xs font-mono mb-1">SERVER</div>
          <div className="text-green-400 font-mono font-bold">{server?.hostname || 'Unknown'}</div>
          <div className="text-green-600 text-sm font-mono">{server?.ipAddress}</div>
        </div>
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
          <div className="text-green-700 text-xs font-mono mb-1">SCRIPT</div>
          <div className="text-green-400 font-mono font-bold">{script?.name || 'Unknown'}</div>
          <div className="text-green-600 text-sm font-mono">{script?.ownerName || 'N/A'}</div>
        </div>
        <div className="bg-gray-900 border border-green-900 rounded-lg p-4">
          <div className="text-green-700 text-xs font-mono mb-1">STARTED</div>
          <div className="text-green-400 font-mono text-sm">
            {task?.startedAt ? new Date(task.startedAt).toLocaleString() : 'Not started'}
          </div>
          {task?.finishedAt && (
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
          <span className="text-green-500 font-mono text-sm font-bold">TERMINAL OUTPUT</span>
          {(task?.status === 'RUNNING' || task?.status === 'PENDING') && (
            <div className="flex items-center space-x-2 ml-auto">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-green-600 text-xs font-mono">LIVE</span>
            </div>
          )}
        </div>
        <div className="p-4 h-96 overflow-y-auto">
          <pre className="text-green-400 font-mono text-sm whitespace-pre-wrap">
            {task?.output || '> Waiting for output...'}
          </pre>
          {task?.status === 'RUNNING' && (
            <div className="flex items-center space-x-2 mt-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-green-600 font-mono text-sm">Processing...</span>
            </div>
          )}
        </div>
      </div>

      {task?.status === 'SUCCESS' && (
        <div className="bg-green-900 bg-opacity-20 border border-green-700 rounded-lg p-4">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-5 h-5 text-green-500" />
            <span className="text-green-400 font-mono font-bold">Task completed successfully</span>
          </div>
        </div>
      )}

      {task?.status === 'FAILED' && (
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
