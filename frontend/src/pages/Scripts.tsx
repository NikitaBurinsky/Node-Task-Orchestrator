import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Plus, FileCode, Trash2, Eye, CheckSquare, Square } from 'lucide-react';
import { scriptsApi } from '../services/api';
import type { ScriptDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { AsyncState } from '../components/AsyncState';
import { useToast } from '../contexts/ToastContext';
import { useConfirmDialog } from '../contexts/ConfirmDialogContext';
import { useActivityFeed } from '../contexts/ActivityFeedContext';

export function Scripts() {
  const [scripts, setScripts] = useState<ScriptDto[]>([]);
  const [showEditor, setShowEditor] = useState(false);
  const [selectedScript, setSelectedScript] = useState<ScriptDto | null>(null);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isBulkDeleting, setIsBulkDeleting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [formData, setFormData] = useState<ScriptDto>({
    name: '',
    content: '#!/bin/bash\n\n',
    isPublic: true,
  });
  const [searchParams, setSearchParams] = useSearchParams();
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const { showToast } = useToast();
  const { confirm } = useConfirmDialog();
  const { addActivity } = useActivityFeed();

  const isCreateRequested = searchParams.get('create') === '1';
  const searchQuery = searchParams.get('q') ?? '';

  const setQueryParam = useCallback(
    (name: string, value: string | null) => {
      const next = new URLSearchParams(searchParams);
      if (!value || !value.trim()) {
        next.delete(name);
      } else {
        next.set(name, value.trim());
      }
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams]
  );

  const fetchScripts = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await scriptsApi.getAll();
      setScripts(response.data);
      setError(null);
    } catch (fetchError) {
      console.error('Failed to fetch scripts:', fetchError);
      setError('Failed to load scripts.');
      showToast('Failed to load scripts.', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    fetchScripts();
  }, [fetchScripts]);

  useEffect(() => {
    if (isCreateRequested) {
      setShowEditor(true);
    }
  }, [isCreateRequested]);

  useEffect(() => {
    setSelectedIds((current) => current.filter((id) => scripts.some((script) => script.id === id)));
  }, [scripts]);

  useEffect(() => {
    const handler = () => {
      searchInputRef.current?.focus();
      searchInputRef.current?.select();
    };
    window.addEventListener('nto:focus-search', handler as EventListener);
    return () => {
      window.removeEventListener('nto:focus-search', handler as EventListener);
    };
  }, []);

  const openCreateForm = () => {
    setShowEditor(true);
    const params = new URLSearchParams(searchParams);
    params.set('create', '1');
    setSearchParams(params, { replace: true });
  };

  const closeCreateForm = () => {
    setShowEditor(false);
    setFormError(null);
    const params = new URLSearchParams(searchParams);
    params.delete('create');
    setSearchParams(params, { replace: true });
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (isSubmitting) return;

    if (!formData.name?.trim()) {
      setFormError('Script name is required.');
      return;
    }

    setFormError(null);
    setIsSubmitting(true);
    const scriptName = formData.name.trim();

    try {
      await scriptsApi.create({
        ...formData,
        name: scriptName,
      });
      setFormData({
        name: '',
        content: '#!/bin/bash\n\n',
        isPublic: true,
      });
      closeCreateForm();
      showToast('Script created.', 'success');
      addActivity({
        title: 'Script created',
        details: scriptName,
        status: 'success',
      });
      await fetchScripts();
    } catch (submitError) {
      console.error('Failed to create script:', submitError);
      showToast('Failed to create script.', 'error');
      addActivity({
        title: 'Script create failed',
        details: scriptName,
        status: 'error',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (script: ScriptDto) => {
    const confirmed = await confirm({
      title: 'Delete script?',
      description: `${script.name} will be permanently deleted from your script library.`,
      confirmText: 'Delete',
      cancelText: 'Keep',
      tone: 'danger',
    });

    if (!confirmed || !script.id) {
      return;
    }

    try {
      await scriptsApi.delete(script.id);
      showToast('Script deleted.', 'success');
      addActivity({
        title: 'Script deleted',
        details: script.name ?? `#${script.id}`,
        status: 'success',
      });
      await fetchScripts();
    } catch (deleteError) {
      console.error('Failed to delete script:', deleteError);
      showToast('Failed to delete script.', 'error');
      addActivity({
        title: 'Script delete failed',
        details: script.name ?? `#${script.id}`,
        status: 'error',
      });
    }
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0 || isBulkDeleting) {
      return;
    }

    const confirmed = await confirm({
      title: `Delete ${selectedIds.length} script${selectedIds.length === 1 ? '' : 's'}?`,
      description: 'Selected scripts will be permanently removed.',
      confirmText: 'Delete selected',
      cancelText: 'Cancel',
      tone: 'danger',
    });

    if (!confirmed) {
      return;
    }

    setIsBulkDeleting(true);
    const byId = new Map(scripts.map((script) => [script.id, script.name ?? `#${script.id}`]));

    const results = await Promise.allSettled(selectedIds.map((id) => scriptsApi.delete(id)));
    const successIds: number[] = [];
    const failedNames: string[] = [];

    results.forEach((result, index) => {
      const id = selectedIds[index];
      if (result.status === 'fulfilled') {
        successIds.push(id);
      } else {
        failedNames.push(byId.get(id) ?? `#${id}`);
      }
    });

    if (successIds.length > 0) {
      showToast(
        `Deleted ${successIds.length} script${successIds.length === 1 ? '' : 's'}.`,
        failedNames.length > 0 ? 'info' : 'success'
      );
    }
    if (failedNames.length > 0) {
      showToast(`Failed to delete ${failedNames.length} script(s).`, 'error');
      setError(`Failed to delete: ${failedNames.join(', ')}`);
    } else {
      setError(null);
    }

    addActivity({
      title: 'Bulk delete scripts',
      details: `Deleted ${successIds.length}, failed ${failedNames.length}`,
      status: failedNames.length > 0 ? 'error' : 'success',
    });

    setSelectedIds((current) => current.filter((id) => !successIds.includes(id)));
    await fetchScripts();
    setIsBulkDeleting(false);
  };

  const handleView = async (id: number) => {
    try {
      const response = await scriptsApi.getById(id);
      setSelectedScript(response.data);
    } catch (viewError) {
      console.error('Failed to fetch script:', viewError);
      showToast('Failed to open script.', 'error');
      addActivity({
        title: 'Open script failed',
        details: `Script #${id}`,
        status: 'error',
      });
    }
  };

  const filteredScripts = useMemo(() => {
    const term = searchQuery.trim().toLowerCase();
    if (!term) {
      return scripts;
    }

    return scripts.filter((script) =>
      [script.name, script.ownerName, script.content].join(' ').toLowerCase().includes(term)
    );
  }, [scripts, searchQuery]);

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);
  const allVisibleSelected =
    filteredScripts.length > 0 && filteredScripts.every((script) => selectedSet.has(script.id ?? -1));

  const toggleSelect = (id: number, selected: boolean) => {
    setSelectedIds((current) => {
      if (selected) {
        if (current.includes(id)) {
          return current;
        }
        return [...current, id];
      }
      return current.filter((existing) => existing !== id);
    });
  };

  const toggleSelectVisible = () => {
    if (allVisibleSelected) {
      const visibleIds = new Set(filteredScripts.map((script) => script.id).filter(Boolean) as number[]);
      setSelectedIds((current) => current.filter((id) => !visibleIds.has(id)));
      return;
    }
    const toAdd = filteredScripts.map((script) => script.id).filter(Boolean) as number[];
    setSelectedIds((current) => Array.from(new Set([...current, ...toAdd])));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="$ scripts"
        subtitle="Automation library"
        actions={
          <button
            onClick={showEditor ? closeCreateForm : openCreateForm}
            className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors btn-operator"
          >
            <Plus className="w-4 h-4" />
            <span>{showEditor ? 'Close Editor' : 'New Script'}</span>
          </button>
        }
      />

      <div className="bg-gray-900 border border-green-900 rounded-lg p-4 animate-page-enter">
        <div className="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-3">
          <div>
            <label className="block text-green-500 font-mono text-sm mb-2">Search scripts</label>
            <input
              ref={searchInputRef}
              type="text"
              value={searchQuery}
              onChange={(event) => setQueryParam('q', event.target.value)}
              placeholder="name, owner, content"
              className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500"
            />
          </div>
          <button
            type="button"
            onClick={toggleSelectVisible}
            disabled={filteredScripts.length === 0}
            className="self-end inline-flex items-center justify-center gap-2 px-3 py-2 rounded font-mono text-sm bg-gray-800 text-gray-300 hover:bg-gray-700 disabled:opacity-50 btn-operator"
          >
            {allVisibleSelected ? <Square className="w-4 h-4" /> : <CheckSquare className="w-4 h-4" />}
            <span>{allVisibleSelected ? 'Unselect Visible' : 'Select Visible'}</span>
          </button>
        </div>
      </div>

      {selectedIds.length > 0 && (
        <div className="bg-gray-900 border border-green-700 rounded-lg p-4 flex items-center justify-between gap-3 flex-wrap animate-page-enter">
          <p className="text-green-300 font-mono text-sm">
            {selectedIds.length} script{selectedIds.length === 1 ? '' : 's'} selected
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleBulkDelete}
              disabled={isBulkDeleting}
              className="inline-flex items-center gap-2 px-3 py-2 rounded font-mono text-sm bg-red-900 text-red-200 hover:bg-red-800 disabled:opacity-50 btn-operator"
            >
              <Trash2 className="w-4 h-4" />
              <span>{isBulkDeleting ? 'Deleting...' : 'Bulk Delete'}</span>
            </button>
            <button
              type="button"
              onClick={() => setSelectedIds([])}
              className="px-3 py-2 rounded font-mono text-sm bg-gray-800 text-gray-300 hover:bg-gray-700 btn-operator"
            >
              Clear
            </button>
          </div>
        </div>
      )}

      {error && scripts.length > 0 && (
        <div className="border border-red-800 bg-red-950 text-red-300 px-4 py-2 rounded font-mono text-sm">
          {error}
        </div>
      )}

      {showEditor && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">Script Editor</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">Script Name</label>
              <input
                type="text"
                value={formData.name}
                onChange={(event) => setFormData({ ...formData, name: event.target.value })}
                className={`w-full bg-black border rounded px-3 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500 ${
                  formError ? 'border-red-700' : 'border-green-900'
                }`}
                placeholder="system-update.sh"
                required
              />
              {formError && <p className="text-red-400 font-mono text-xs mt-2">{formError}</p>}
            </div>
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">Script Content</label>
              <textarea
                value={formData.content}
                onChange={(event) => setFormData({ ...formData, content: event.target.value })}
                className="w-full h-64 bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono text-sm focus:outline-none focus:border-green-500 resize-none"
                required
              />
            </div>
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="isPublic"
                checked={formData.isPublic}
                onChange={(event) => setFormData({ ...formData, isPublic: event.target.checked })}
                className="w-4 h-4 bg-black border border-green-900 rounded"
              />
              <label htmlFor="isPublic" className="text-green-500 font-mono text-sm">
                Public script
              </label>
            </div>
            <div className="flex space-x-3">
              <button
                type="submit"
                disabled={isSubmitting}
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed btn-operator"
              >
                {isSubmitting ? 'Saving...' : 'Save Script'}
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

      {selectedScript && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6 animate-page-enter">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-green-500 font-mono">{selectedScript.name}</h2>
            <button
              onClick={() => setSelectedScript(null)}
              className="text-green-500 hover:text-green-400 text-sm font-mono"
            >
              Close
            </button>
          </div>
          <div className="bg-black border border-green-900 rounded p-4">
            <pre className="text-green-400 font-mono text-sm whitespace-pre-wrap">
              {selectedScript.content}
            </pre>
          </div>
          <div className="mt-4 text-green-700 text-sm font-mono">
            Owner: {selectedScript.ownerName} |{selectedScript.isPublic ? ' Public' : ' Private'}
          </div>
        </div>
      )}

      {isLoading && !showEditor && (
        <AsyncState
          kind="loading"
          title="Loading scripts"
          description="Collecting your automation scripts."
        />
      )}

      {!isLoading && error && scripts.length === 0 && !showEditor && (
        <AsyncState
          kind="error"
          title="Script library unavailable"
          description={error}
          actionLabel="Retry"
          onAction={fetchScripts}
        />
      )}

      {!isLoading && filteredScripts.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredScripts.map((script, index) => (
            <div
              key={script.id}
              className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 animate-card-stagger card-interactive"
              style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <FileCode className="w-6 h-6 text-green-500" />
                  <div>
                    <h3 className="text-green-400 font-mono font-bold">{script.name}</h3>
                    <p className="text-green-700 text-xs font-mono mt-1">by {script.ownerName}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <label className="inline-flex items-center gap-2 text-green-600 text-xs font-mono cursor-pointer">
                    <input
                      type="checkbox"
                      checked={selectedSet.has(script.id!)}
                      onChange={(event) => toggleSelect(script.id!, event.target.checked)}
                      className="w-4 h-4 bg-black border border-green-800 rounded"
                    />
                    Pick
                  </label>
                  <button
                    onClick={() => handleDelete(script)}
                    className="text-red-500 hover:text-red-400 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div className="bg-black border border-green-900 rounded p-2 mb-4">
                <pre className="text-green-600 font-mono text-xs overflow-hidden h-12">
                  {script.content?.slice(0, 100)}...
                </pre>
              </div>
              <div className="flex items-center justify-between">
                <span
                  className={`text-xs font-mono ${
                    script.isPublic ? 'text-green-600' : 'text-yellow-600'
                  }`}
                >
                  {script.isPublic ? 'PUBLIC' : 'PRIVATE'}
                </span>
                <button
                  onClick={() => handleView(script.id!)}
                  className="flex items-center space-x-1 text-green-500 hover:text-green-400 text-sm font-mono"
                >
                  <Eye className="w-4 h-4" />
                  <span>View</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {!isLoading && filteredScripts.length === 0 && scripts.length > 0 && !showEditor && (
        <AsyncState
          kind="empty"
          title="No scripts match current search"
          description="Try another search query."
        />
      )}

      {!isLoading && scripts.length === 0 && !showEditor && !error && (
        <AsyncState
          kind="empty"
          title="No scripts in library"
          description="Create your first script to automate server operations."
          actionLabel="Create Script"
          onAction={openCreateForm}
        />
      )}
    </div>
  );
}
