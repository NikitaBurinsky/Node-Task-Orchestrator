import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Layers, Trash2 } from 'lucide-react';
import { groupsApi } from '../services/api';
import type { ServerGroupDto} from '../types/api';

export function ServerGroups() {
  const [groups, setGroups] = useState<ServerGroupDto[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [groupName, setGroupName] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
        const groupsRes = await groupsApi.getAll();
      setGroups(groupsRes.data);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    }
  };


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await groupsApi.create({ name: groupName });
      setGroupName('');
      setShowAddForm(false);
      fetchData();
    } catch (error) {
      console.error('Failed to create group:', error);
    }
  };

  const handleDelete = async (id: number) => {
    if (confirm('Delete this group?')) {
      try {
        await groupsApi.delete(id);
        fetchData();
      } catch (error) {
        console.error('Failed to delete group:', error);
      }
    }
  };

    const getServerCount = (group: ServerGroupDto) => {
        return group.servers?.length || 0;
    };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-green-500 font-mono">$ groups</h1>
          <p className="text-green-700 font-mono mt-1">Organize servers into clusters</p>
        </div>
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>New Group</span>
        </button>
      </div>

      {showAddForm && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">
            Create Group
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">
                Group Name
              </label>
              <input
                type="text"
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
                placeholder="Production Cluster"
                required
              />
            </div>
            <div className="flex space-x-3">
              <button
                type="submit"
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
              >
                Create
              </button>
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {groups.map((group) => (
          <div
            key={group.id}
            className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors cursor-pointer"
            onClick={() => navigate(`/groups/${group.id}`)}
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center space-x-3">
                <Layers className="w-6 h-6 text-green-500" />
                <div>
                  <h3 className="text-green-400 font-mono font-bold">{group.name}</h3>
                  <p className="text-green-700 text-sm font-mono">
                    {getServerCount(group)} server{getServerCount(group) !== 1 ? 's' : ''}
                  </p>
                </div>
              </div>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(group.id!);
                }}
                className="text-red-500 hover:text-red-400 transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
            <div className="text-green-700 text-sm font-mono">
              Click to manage servers
            </div>
          </div>
        ))}
      </div>

      {groups.length === 0 && !showAddForm && (
        <div className="text-center py-12">
          <Layers className="w-16 h-16 text-green-900 mx-auto mb-4" />
          <p className="text-green-700 font-mono">No groups created yet</p>
        </div>
      )}
    </div>
  );
}
