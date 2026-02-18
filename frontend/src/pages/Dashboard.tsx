import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Activity, Server, Layers, FileCode, List } from 'lucide-react';
import { statsApi, serversApi, groupsApi, scriptsApi, tasksApi } from '../services/api';
import type { StatsDto } from '../types/api';

export function Dashboard() {
  const [stats, setStats] = useState<StatsDto>({});
  const [counts, setCounts] = useState({ servers: 0, groups: 0, scripts: 0, tasks: 0 });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await statsApi.getStats();
        setStats(response.data);
      } catch (error) {
        console.error('Failed to fetch stats:', error);
      }
    };

    fetchStats();
    const interval = setInterval(fetchStats, 3000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const fetchCounts = async () => {
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
      } catch (error) {
        console.error('Failed to fetch counts:', error);
      }
    };
    fetchCounts();
  }, []);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-green-500 font-mono mb-2">
          $ dashboard
        </h1>
        <p className="text-green-700 font-mono">System Overview</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Link
          to="/servers"
          className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors"
        >
          <div className="flex items-center justify-between mb-4">
            <Server className="w-8 h-8 text-green-500" />
            <span className="text-3xl font-bold text-green-400 font-mono">
              {counts.servers}
            </span>
          </div>
          <h3 className="text-green-500 font-mono text-lg">Servers</h3>
          <p className="text-green-700 text-sm font-mono mt-1">Managed nodes</p>
        </Link>

        <Link
          to="/groups"
          className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors"
        >
          <div className="flex items-center justify-between mb-4">
            <Layers className="w-8 h-8 text-green-500" />
            <span className="text-3xl font-bold text-green-400 font-mono">
              {counts.groups}
            </span>
          </div>
          <h3 className="text-green-500 font-mono text-lg">Groups</h3>
          <p className="text-green-700 text-sm font-mono mt-1">Server clusters</p>
        </Link>

        <Link
          to="/scripts"
          className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors"
        >
          <div className="flex items-center justify-between mb-4">
            <FileCode className="w-8 h-8 text-green-500" />
            <span className="text-3xl font-bold text-green-400 font-mono">
              {counts.scripts}
            </span>
          </div>
          <h3 className="text-green-500 font-mono text-lg">Scripts</h3>
          <p className="text-green-700 text-sm font-mono mt-1">Automation library</p>
        </Link>

        <Link
          to="/tasks"
          className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors"
        >
          <div className="flex items-center justify-between mb-4">
            <List className="w-8 h-8 text-green-500" />
            <span className="text-3xl font-bold text-green-400 font-mono">
              {counts.tasks}
            </span>
          </div>
          <h3 className="text-green-500 font-mono text-lg">Tasks</h3>
          <p className="text-green-700 text-sm font-mono mt-1">Execution history</p>
        </Link>
      </div>

      <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
        <div className="flex items-center space-x-3 mb-6">
          <Activity className="w-6 h-6 text-green-500" />
          <h2 className="text-xl font-bold text-green-500 font-mono">
            Concurrency Stats
          </h2>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-black border border-green-900 rounded p-4">
            <div className="text-green-700 font-mono text-sm mb-2">Safe Counter</div>
            <div className="text-4xl font-bold text-green-400 font-mono">
              {stats.safeCounter || 0}
            </div>
            <div className="mt-2 flex items-center space-x-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-green-600 text-xs font-mono">LIVE</span>
            </div>
          </div>
          <div className="bg-black border border-yellow-900 rounded p-4">
            <div className="text-yellow-700 font-mono text-sm mb-2">Unsafe Counter</div>
            <div className="text-4xl font-bold text-yellow-400 font-mono">
              {stats.unsafeCounter || 0}
            </div>
            <div className="mt-2 flex items-center space-x-2">
              <div className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse"></div>
              <span className="text-yellow-600 text-xs font-mono">LIVE</span>
            </div>
          </div>
        </div>
        <p className="text-green-700 text-xs font-mono mt-4">
          Auto-refreshing every 3 seconds
        </p>
      </div>
    </div>
  );
}
