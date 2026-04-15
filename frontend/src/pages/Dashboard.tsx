import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Server, Layers, FileCode, List, Plus } from 'lucide-react';
import { serversApi, groupsApi, scriptsApi, tasksApi } from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';

export function Dashboard() {
  const [counts, setCounts] = useState({ servers: 0, groups: 0, scripts: 0, tasks: 0 });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
        </>
      )}
    </div>
  );
}
