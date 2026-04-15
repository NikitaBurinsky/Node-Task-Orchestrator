import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Command, CornerDownLeft } from 'lucide-react';

interface CommandPaletteProps {
  open: boolean;
  canPingCurrentGroup: boolean;
  onClose: () => void;
  onPingCurrentGroup: () => void;
}

interface PaletteCommand {
  id: string;
  label: string;
  hint: string;
  keywords: string;
  run: () => void;
}

function isMacLikePlatform() {
  if (typeof navigator === 'undefined') {
    return false;
  }
  return /Mac|iPhone|iPad|iPod/.test(navigator.platform);
}

export function CommandPalette({
  open,
  canPingCurrentGroup,
  onClose,
  onPingCurrentGroup,
}: CommandPaletteProps) {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const commands = useMemo<PaletteCommand[]>(
    () => [
      {
        id: 'nav-dashboard',
        label: 'Go to Dashboard',
        hint: 'g d',
        keywords: 'dashboard home overview',
        run: () => navigate('/dashboard'),
      },
      {
        id: 'nav-servers',
        label: 'Go to Servers',
        hint: 'g s',
        keywords: 'servers nodes inventory',
        run: () => navigate('/servers'),
      },
      {
        id: 'nav-groups',
        label: 'Go to Groups',
        hint: 'g g',
        keywords: 'groups clusters',
        run: () => navigate('/groups'),
      },
      {
        id: 'nav-scripts',
        label: 'Go to Scripts',
        hint: 'scripts automation',
        keywords: 'scripts automation library',
        run: () => navigate('/scripts'),
      },
      {
        id: 'nav-tasks',
        label: 'Go to Tasks',
        hint: 'g t',
        keywords: 'tasks history',
        run: () => navigate('/tasks'),
      },
      {
        id: 'create-server',
        label: 'Add Server',
        hint: 'quick action',
        keywords: 'create server add new',
        run: () => navigate('/servers?create=1'),
      },
      {
        id: 'create-group',
        label: 'New Group',
        hint: 'quick action',
        keywords: 'create group add new',
        run: () => navigate('/groups?create=1'),
      },
      {
        id: 'create-script',
        label: 'New Script',
        hint: 'quick action',
        keywords: 'create script add new',
        run: () => navigate('/scripts?create=1'),
      },
      {
        id: 'ping-current-group',
        label: 'Ping Current Group',
        hint: canPingCurrentGroup ? 'group action' : 'open group first',
        keywords: 'ping group online offline status',
        run: onPingCurrentGroup,
      },
    ],
    [canPingCurrentGroup, navigate, onPingCurrentGroup]
  );

  const filteredCommands = useMemo(() => {
    const term = query.trim().toLowerCase();
    const visible = commands.filter((command) => canPingCurrentGroup || command.id !== 'ping-current-group');
    if (!term) {
      return visible;
    }

    return visible.filter(
      (command) =>
        command.label.toLowerCase().includes(term) ||
        command.keywords.toLowerCase().includes(term) ||
        command.hint.toLowerCase().includes(term)
    );
  }, [canPingCurrentGroup, commands, query]);

  useEffect(() => {
    if (!open) {
      return;
    }

    previousFocusRef.current = document.activeElement as HTMLElement | null;
    setQuery('');
    setActiveIndex(0);
    inputRef.current?.focus();
  }, [open]);

  useEffect(() => {
    if (!open) {
      previousFocusRef.current?.focus();
    }
  }, [open]);

  useEffect(() => {
    if (activeIndex > filteredCommands.length - 1) {
      setActiveIndex(0);
    }
  }, [activeIndex, filteredCommands.length]);

  if (!open) {
    return null;
  }

  const runSelected = (index: number) => {
    const command = filteredCommands[index];
    if (!command) {
      return;
    }
    command.run();
    onClose();
  };

  const shortcut = isMacLikePlatform() ? '⌘K' : 'Ctrl+K';

  return (
    <div
      className="fixed inset-0 z-[80] bg-black/75 backdrop-blur-sm p-4 flex items-start justify-center"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Command palette"
        className="w-full max-w-2xl mt-20 bg-gray-900 border border-green-900 rounded-lg shadow-2xl overflow-hidden animate-page-enter"
      >
        <div className="flex items-center gap-3 px-4 py-3 border-b border-green-900">
          <Command className="w-4 h-4 text-green-500" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'ArrowDown') {
                event.preventDefault();
                setActiveIndex((index) => Math.min(index + 1, filteredCommands.length - 1));
                return;
              }
              if (event.key === 'ArrowUp') {
                event.preventDefault();
                setActiveIndex((index) => Math.max(index - 1, 0));
                return;
              }
              if (event.key === 'Enter') {
                event.preventDefault();
                runSelected(activeIndex);
                return;
              }
              if (event.key === 'Escape') {
                event.preventDefault();
                onClose();
              }
            }}
            placeholder="Type a command..."
            className="flex-1 bg-transparent text-green-300 font-mono text-sm focus:outline-none"
          />
          <span className="text-green-700 font-mono text-xs">{shortcut}</span>
        </div>

        <div className="max-h-[55vh] overflow-y-auto p-2">
          {filteredCommands.length > 0 ? (
            filteredCommands.map((command, index) => (
              <button
                key={command.id}
                type="button"
                className={`w-full text-left px-3 py-3 rounded font-mono transition-colors ${
                  index === activeIndex
                    ? 'bg-green-950/40 border border-green-800'
                    : 'border border-transparent hover:bg-gray-800'
                }`}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => runSelected(index)}
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="text-green-300 text-sm">{command.label}</span>
                  <span className="text-green-700 text-xs">{command.hint}</span>
                </div>
              </button>
            ))
          ) : (
            <div className="px-4 py-8 text-center">
              <p className="text-green-500 font-mono text-sm">No commands found</p>
              <p className="text-green-700 font-mono text-xs mt-1">Try another search term.</p>
            </div>
          )}
        </div>

        <div className="px-4 py-2 border-t border-green-900 text-green-700 font-mono text-xs flex items-center gap-2">
          <CornerDownLeft className="w-3 h-3" />
          <span>Enter to run • Esc to close</span>
        </div>
      </div>
    </div>
  );
}
