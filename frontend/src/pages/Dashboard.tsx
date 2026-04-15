import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Server, Layers, FileCode, List, Plus, History, CheckCircle2, AlertCircle, Info } from 'lucide-react';
import { serversApi, groupsApi, scriptsApi, tasksApi } from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useActivityFeed } from '../contexts/ActivityFeedContext';

export function Dashboard() {
  const [counts, setCounts] = useState({ servers: 0, groups: 0, scripts: 0, tasks: 0 });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isTimelineExpanded, setIsTimelineExpanded] = useState(false);
  const { activities, clearActivities } = useActivityFeed();

  const fetchCounts = useCallback(async () => {
    setIsLoading(true);
    try {
      const [servers, groups, scripts, tasks] = await Promise.all([
        serversApi.getAll(),
        groupsApi.getAll(),
        scriptsApi.getAll(),
        tasksApi.getAll(),
      ]);
      setCounts({
        servers: servers.data.length,
        groups: groups.data.length,
        scripts: scripts.data.length,
        tasks: tasks.data.length,
      });
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch counts:', fetchError);
      setError('Failed to load dashboard metrics.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCounts();
  }, [fetchCounts]);

  return (
    <div className="space-y-8">
      <PageHeader title="$ dashboard" subtitle="System Overview" />

      {isLoading && (
        <AsyncState
          kind="loading"
          title="Loading dashboard"
          description="Gathering latest infrastructure metrics."
        />
      )}

      {!isLoading && error && (
        <AsyncState
          kind="error"
          title="Dashboard unavailable"
          description={error}
          actionLabel="Retry"
          onAction={fetchCounts}
        />
      )}

      {!isLoading && !error && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <Link
              to="/servers"
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 card-interactive animate-card-stagger"
            >
              <div className="flex items-center justify-between mb-4">
                <Server className="w-8 h-8 text-green-500" />
                <span className="text-3xl font-bold text-green-400 font-mono">{counts.servers}</span>
              </div>
              <h3 className="text-green-500 font-mono text-lg">Servers</h3>
              <p className="text-green-700 text-sm font-mono mt-1">Managed nodes</p>
            </Link>

            <Link
              to="/groups"
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 card-interactive animate-card-stagger"
              style={{ animationDelay: '40ms' }}
            >
              <div className="flex items-center justify-between mb-4">
                <Layers className="w-8 h-8 text-green-500" />
                <span className="text-3xl font-bold text-green-400 font-mono">{counts.groups}</span>
              </div>
              <h3 className="text-green-500 font-mono text-lg">Groups</h3>
              <p className="text-green-700 text-sm font-mono mt-1">Server clusters</p>
            </Link>

            <Link
              to="/scripts"
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 card-interactive animate-card-stagger"
              style={{ animationDelay: '80ms' }}
            >
              <div className="flex items-center justify-between mb-4">
                <FileCode className="w-8 h-8 text-green-500" />
                <span className="text-3xl font-bold text-green-400 font-mono">{counts.scripts}</span>
              </div>
              <h3 className="text-green-500 font-mono text-lg">Scripts</h3>
              <p className="text-green-700 text-sm font-mono mt-1">Automation library</p>
            </Link>

            <Link
              to="/tasks"
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 card-interactive animate-card-stagger"
              style={{ animationDelay: '120ms' }}
            >
              <div className="flex items-center justify-between mb-4">
                <List className="w-8 h-8 text-green-500" />
                <span className="text-3xl font-bold text-green-400 font-mono">{counts.tasks}</span>
              </div>
              <h3 className="text-green-500 font-mono text-lg">Tasks</h3>
              <p className="text-green-700 text-sm font-mono mt-1">Execution history</p>
            </Link>
          </div>

          <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
            <div className="flex items-center space-x-3 mb-4">
              <Plus className="w-5 h-5 text-green-500" />
              <h2 className="text-xl font-bold text-green-500 font-mono">Quick Actions</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <Link
                to="/servers?create=1"
                className="bg-black border border-green-900 rounded p-4 hover:border-green-700 card-interactive"
              >
                <div className="flex items-center space-x-3">
                  <Server className="w-5 h-5 text-green-500" />
                  <div>
                    <div className="text-green-400 font-mono font-bold">Add Server</div>
                    <div className="text-green-700 text-xs font-mono">Open create form</div>
                  </div>
                </div>
              </Link>
              <Link
                to="/groups?create=1"
                className="bg-black border border-green-900 rounded p-4 hover:border-green-700 card-interactive"
              >
                <div className="flex items-center space-x-3">
                  <Layers className="w-5 h-5 text-green-500" />
                  <div>
                    <div className="text-green-400 font-mono font-bold">New Group</div>
                    <div className="text-green-700 text-xs font-mono">Create server group</div>
                  </div>
                </div>
              </Link>
              <Link
                to="/scripts?create=1"
                className="bg-black border border-green-900 rounded p-4 hover:border-green-700 card-interactive"
              >
                <div className="flex items-center space-x-3">
                  <FileCode className="w-5 h-5 text-green-500" />
                  <div>
                    <div className="text-green-400 font-mono font-bold">New Script</div>
                    <div className="text-green-700 text-xs font-mono">Open script editor</div>
                  </div>
                </div>
              </Link>
              <Link
                to="/tasks"
                className="bg-black border border-green-900 rounded p-4 hover:border-green-700 card-interactive"
              >
                <div className="flex items-center space-x-3">
                  <List className="w-5 h-5 text-green-500" />
                  <div>
                    <div className="text-green-400 font-mono font-bold">Open Tasks</div>
                    <div className="text-green-700 text-xs font-mono">Inspect execution history</div>
                  </div>
                </div>
              </Link>
            </div>
          </div>

          <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
            <div className="flex items-center justify-between gap-3 mb-4">
              <div className="flex items-center space-x-3">
                <History className="w-5 h-5 text-green-500" />
                <h2 className="text-xl font-bold text-green-500 font-mono">Activity Timeline</h2>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsTimelineExpanded((value) => !value)}
                  className="px-3 py-2 rounded text-xs font-mono bg-gray-800 text-gray-300 hover:bg-gray-700 transition-colors btn-operator"
                >
                  {isTimelineExpanded ? 'Hide' : 'Show'}
                </button>
                {isTimelineExpanded && (
                  <button
                    type="button"
                    onClick={clearActivities}
                    disabled={activities.length === 0}
                    className={`px-3 py-2 rounded text-xs font-mono transition-colors btn-operator ${
                      activities.length > 0
                        ? 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                        : 'bg-gray-900 text-gray-600 cursor-not-allowed'
                    }`}
                  >
                    Clear
                  </button>
                )}
              </div>
            </div>

            {isTimelineExpanded ? (
              activities.length > 0 ? (
                <div className="space-y-2">
                  {activities.slice(0, 12).map((activity, index) => (
                    <div
                      key={activity.id}
                      className="bg-black border border-green-900 rounded px-3 py-3 flex items-center justify-between gap-3 animate-card-stagger"
                      style={{ animationDelay: `${Math.min(index, 12) * 30}ms` }}
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        {activity.status === 'success' ? (
                          <CheckCircle2 className="w-4 h-4 text-green-500 shrink-0" />
                        ) : activity.status === 'error' ? (
                          <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                        ) : (
                          <Info className="w-4 h-4 text-blue-500 shrink-0" />
                        )}
                        <div className="min-w-0">
                          <p className="text-green-300 font-mono text-sm truncate">{activity.title}</p>
                          {activity.details && (
                            <p className="text-green-700 font-mono text-xs truncate">
                              {activity.details}
                            </p>
                          )}
                        </div>
                      </div>
                      <span className="text-green-700 text-xs font-mono shrink-0">
                        {new Date(activity.at).toLocaleTimeString()}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <AsyncState
                  kind="empty"
                  title="No activity yet"
                  description="Create, delete, ping, and execute actions will appear here."
                />
              )
            ) : (
              <div className="bg-black border border-green-900 rounded px-4 py-3">
                <p className="text-green-700 font-mono text-sm">
                  Timeline is hidden. Click <span className="text-green-500">Show</span> to expand.
                </p>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
