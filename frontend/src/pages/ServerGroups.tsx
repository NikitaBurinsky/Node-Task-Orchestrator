import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Plus, Layers, Trash2, Lock } from 'lucide-react';
import { groupsApi } from '../services/api';
import type { ServerGroupDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { useToast } from '../contexts/ToastContext';

export function ServerGroups() {
  const [groups, setGroups] = useState<ServerGroupDto[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [groupName, setGroupName] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const isCreateRequested = searchParams.get('create') === '1';

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (isCreateRequested) {
      setShowAddForm(true);
    }
  }, [isCreateRequested]);

  const openCreateForm = () => {
    setShowAddForm(true);
    const params = new URLSearchParams(searchParams);
    params.set('create', '1');
    setSearchParams(params, { replace: true });
  };

  const closeCreateForm = () => {
    setShowAddForm(false);
    setGroupName('');
    setFormError(null);
    const params = new URLSearchParams(searchParams);
    params.delete('create');
    setSearchParams(params, { replace: true });
  };

  const fetchData = async () => {
    try {
      const groupsRes = await groupsApi.getAll();
      setGroups(groupsRes.data);
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch data:', fetchError);
      setError('Failed to load groups.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    const trimmedName = groupName.trim();
    if (!trimmedName) {
      setFormError('Group name is required.');
      return;
    }
    if (trimmedName.length < 3) {
      setFormError('Group name must be at least 3 characters.');
      return;
    }

    setFormError(null);
    setIsSubmitting(true);

    try {
      await groupsApi.create({ name: trimmedName });
      closeCreateForm();
      setError(null);
      showToast('Group created.', 'success');
      await fetchData();
    } catch (submitError) {
      console.error('Failed to create group:', submitError);
      setError('Failed to create group.');
      showToast('Failed to create group.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const isDefaultGroup = (group: ServerGroupDto) => group.name === 'Default';

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this group?')) {
      return;
    }

    try {
      await groupsApi.delete(id);
      setError(null);
      showToast('Group deleted.', 'success');
      await fetchData();
    } catch (deleteError) {
      console.error('Failed to delete group:', deleteError);
      setError('Failed to delete group.');
      showToast('Failed to delete group.', 'error');
    }
  };

  const getServerCount = (group: ServerGroupDto) => {
    return group.servers?.length || 0;
  };

  const sortedGroups = [...groups].sort((a, b) => {
    if (isDefaultGroup(a) && !isDefaultGroup(b)) return -1;
    if (!isDefaultGroup(a) && isDefaultGroup(b)) return 1;
    return a.name.localeCompare(b.name);
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="$ groups"
        subtitle="Organize servers into clusters"
        actions={
          <button
            onClick={showAddForm ? closeCreateForm : openCreateForm}
            className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>{showAddForm ? 'Close Form' : 'New Group'}</span>
          </button>
        }
      />

      {error && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-2 rounded font-mono text-sm">
          {error}
        </div>
      )}

      {showAddForm && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">Create Group</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">Group Name</label>
              <input
                type="text"
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                className={`w-full bg-black border rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500 ${
                  formError ? 'border-red-700' : 'border-green-900'
                }`}
                placeholder="Production Cluster"
                required
              />
              {formError && <p className="text-red-400 font-mono text-xs mt-2">{formError}</p>}
            </div>
            <div className="flex space-x-3">
              <button
                type="submit"
                disabled={isSubmitting}
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? 'Creating...' : 'Create'}
              </button>
              <button
                type="button"
                onClick={closeCreateForm}
                className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {sortedGroups.map((group) => {
          const isDefault = isDefaultGroup(group);
          return (
            <div
              key={group.id}
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors cursor-pointer"
              onClick={() => navigate(`/groups/${group.id}`)}
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <Layers className="w-6 h-6 text-green-500" />
                  <div>
                    <div className="flex items-center space-x-2">
                      <h3 className="text-green-400 font-mono font-bold">{group.name}</h3>
                      {isDefault && <Lock className="w-4 h-4 text-green-600" />}
                    </div>
                    <p className="text-green-700 text-sm font-mono">
                      {getServerCount(group)} server{getServerCount(group) !== 1 ? 's' : ''}
                    </p>
                    {isDefault && (
                      <p
                        className="text-green-600 text-xs font-mono mt-1"
                        title="Default group is always present"
                      >
                        Default group
                      </p>
                    )}
                  </div>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (!isDefault) {
                      handleDelete(group.id!);
                    }
                  }}
                  className={`transition-colors ${
                    isDefault
                      ? 'text-red-900 cursor-not-allowed'
                      : 'text-red-500 hover:text-red-400'
                  }`}
                  disabled={isDefault}
                  title={isDefault ? 'Default group cannot be deleted' : 'Delete group'}
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
              <div className="text-green-700 text-sm font-mono">Click to manage servers</div>
            </div>
          );
        })}
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
