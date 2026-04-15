import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Plus, Layers, Trash2, Lock } from 'lucide-react';
import { groupsApi } from '../services/api';
import type { ServerGroupDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useToast } from '../contexts/ToastContext';
import { useConfirmDialog } from '../contexts/ConfirmDialogContext';
import { useActivityFeed } from '../contexts/ActivityFeedContext';

export function ServerGroups() {
  const [groups, setGroups] = useState<ServerGroupDto[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [groupName, setGroupName] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { confirm } = useConfirmDialog();
  const { addActivity } = useActivityFeed();

  const isCreateRequested = searchParams.get('create') === '1';

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const groupsRes = await groupsApi.getAll();
      setGroups(groupsRes.data);
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch data:', fetchError);
      setError('Failed to load groups.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

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

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
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
      addActivity({
        title: 'Group created',
        details: trimmedName,
        status: 'success',
      });
      await fetchData();
    } catch (submitError) {
      console.error('Failed to create group:', submitError);
      setError('Failed to create group.');
      showToast('Failed to create group.', 'error');
      addActivity({
        title: 'Group create failed',
        details: trimmedName,
        status: 'error',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const isDefaultGroup = (group: ServerGroupDto) => group.name === 'Default';

  const handleDelete = async (group: ServerGroupDto) => {
    const confirmed = await confirm({
      title: 'Delete group?',
      description: `${group.name} will be removed. This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Keep',
      tone: 'danger',
    });

    if (!confirmed || !group.id) {
      return;
    }

    try {
      await groupsApi.delete(group.id);
      setError(null);
      showToast('Group deleted.', 'success');
      addActivity({
        title: 'Group deleted',
        details: group.name,
        status: 'success',
      });
      await fetchData();
    } catch (deleteError) {
      console.error('Failed to delete group:', deleteError);
      setError('Failed to delete group.');
      showToast('Failed to delete group.', 'error');
      addActivity({
        title: 'Group delete failed',
        details: group.name,
        status: 'error',
      });
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
            className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors btn-operator"
          >
            <Plus className="w-4 h-4" />
            <span>{showAddForm ? 'Close Form' : 'New Group'}</span>
          </button>
        }
      />

      {error && groups.length > 0 && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-2 rounded font-mono text-sm">
          {error}
        </div>
      )}

      {showAddForm && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">Create Group</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">Group Name</label>
              <input
                type="text"
                value={groupName}
                onChange={(event) => setGroupName(event.target.value)}
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
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed btn-operator"
              >
                {isSubmitting ? 'Creating...' : 'Create'}
              </button>
              <button
                type="button"
                onClick={closeCreateForm}
                className="bg-gray-800 text-gray-400 px-4 py-2 rounded font-mono hover:bg-gray-700 transition-colors btn-operator"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {isLoading && !showAddForm && (
        <AsyncState
          kind="loading"
          title="Loading groups"
          description="Collecting your server groups."
        />
      )}

      {!isLoading && error && groups.length === 0 && !showAddForm && (
        <AsyncState
          kind="error"
          title="Group list unavailable"
          description={error}
          actionLabel="Retry"
          onAction={fetchData}
        />
      )}

      {!isLoading && groups.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {sortedGroups.map((group, index) => {
            const isDefault = isDefaultGroup(group);
            return (
              <div
                key={group.id}
                className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 cursor-pointer animate-card-stagger card-interactive"
                style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
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
                    onClick={(event) => {
                      event.stopPropagation();
                      if (!isDefault) {
                        handleDelete(group);
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
      )}

      {!isLoading && groups.length === 0 && !showAddForm && !error && (
        <AsyncState
          kind="empty"
          title="No groups created yet"
          description="Create your first group to organize servers by environment."
          actionLabel="Create Group"
          onAction={openCreateForm}
        />
      )}
    </div>
  );
}
