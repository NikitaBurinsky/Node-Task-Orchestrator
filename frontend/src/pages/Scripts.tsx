import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Plus, FileCode, Trash2, Eye } from 'lucide-react';
import { scriptsApi } from '../services/api';
import type { ScriptDto } from '../types/api';
import { PageHeader } from '../components/PageHeader';
import { useToast } from '../contexts/ToastContext';

export function Scripts() {
  const [scripts, setScripts] = useState<ScriptDto[]>([]);
  const [showEditor, setShowEditor] = useState(false);
  const [selectedScript, setSelectedScript] = useState<ScriptDto | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formData, setFormData] = useState<ScriptDto>({
    name: '',
    content: '#!/bin/bash\n\n',
    isPublic: true,
  });
  const [searchParams, setSearchParams] = useSearchParams();
  const { showToast } = useToast();

  const isCreateRequested = searchParams.get('create') === '1';

  useEffect(() => {
    fetchScripts();
  }, []);

  useEffect(() => {
    if (isCreateRequested) {
      setShowEditor(true);
    }
  }, [isCreateRequested]);

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

  const fetchScripts = async () => {
    try {
      const response = await scriptsApi.getAll();
      setScripts(response.data);
    } catch (fetchError) {
      console.error('Failed to fetch scripts:', fetchError);
      showToast('Failed to load scripts.', 'error');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    if (!formData.name?.trim()) {
      setFormError('Script name is required.');
      return;
    }

    setFormError(null);
    setIsSubmitting(true);
    try {
      await scriptsApi.create({
        ...formData,
        name: formData.name?.trim(),
      });
      setFormData({
        name: '',
        content: '#!/bin/bash\n\n',
        isPublic: true,
      });
      closeCreateForm();
      showToast('Script created.', 'success');
      await fetchScripts();
    } catch (submitError) {
      console.error('Failed to create script:', submitError);
      showToast('Failed to create script.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this script?')) {
      return;
    }

    try {
      await scriptsApi.delete(id);
      showToast('Script deleted.', 'success');
      await fetchScripts();
    } catch (deleteError) {
      console.error('Failed to delete script:', deleteError);
      showToast('Failed to delete script.', 'error');
    }
  };

  const handleView = async (id: number) => {
    try {
      const response = await scriptsApi.getById(id);
      setSelectedScript(response.data);
    } catch (viewError) {
      console.error('Failed to fetch script:', viewError);
      showToast('Failed to open script.', 'error');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="$ scripts"
        subtitle="Automation library"
        actions={
          <button
            onClick={showEditor ? closeCreateForm : openCreateForm}
            className="flex items-center space-x-2 bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>{showEditor ? 'Close Editor' : 'New Script'}</span>
          </button>
        }
      />

      {showEditor && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
          <h2 className="text-lg font-bold text-green-500 font-mono mb-4">Script Editor</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">Script Name</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
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
                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                className="w-full h-64 bg-black border border-green-900 rounded px-3 py-2 text-green-400 font-mono text-sm focus:outline-none focus:border-green-500 resize-none"
                required
              />
            </div>
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="isPublic"
                checked={formData.isPublic}
                onChange={(e) => setFormData({ ...formData, isPublic: e.target.checked })}
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
                className="bg-green-900 text-green-300 px-4 py-2 rounded font-mono hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? 'Saving...' : 'Save Script'}
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

      {selectedScript && (
        <div className="bg-gray-900 border border-green-900 rounded-lg p-6">
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

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {scripts.map((script) => (
          <div
            key={script.id}
            className="bg-gray-900 border border-green-900 rounded-lg p-6 hover:border-green-700 transition-colors"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center space-x-3">
                <FileCode className="w-6 h-6 text-green-500" />
                <div>
                  <h3 className="text-green-400 font-mono font-bold">{script.name}</h3>
                  <p className="text-green-700 text-xs font-mono mt-1">by {script.ownerName}</p>
                </div>
              </div>
              <button
                onClick={() => handleDelete(script.id!)}
                className="text-red-500 hover:text-red-400 transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
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

      {scripts.length === 0 && !showEditor && (
        <div className="text-center py-12">
          <FileCode className="w-16 h-16 text-green-900 mx-auto mb-4" />
          <p className="text-green-700 font-mono">No scripts in library</p>
        </div>
      )}
    </div>
  );
}
