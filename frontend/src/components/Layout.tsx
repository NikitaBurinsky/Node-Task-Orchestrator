import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, Outlet, useNavigate } from 'react-router-dom';
import { Terminal, Server, Layers, FileCode, List, LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useTheme, type AppTheme } from '../contexts/ThemeContext';
import { CommandPalette } from './CommandPalette';

function isEditableTarget(target: EventTarget | null) {
  const element = target as HTMLElement | null;
  if (!element) {
    return false;
  }

  if (element.isContentEditable) {
    return true;
  }

  const tag = element.tagName.toLowerCase();
  return tag === 'input' || tag === 'textarea' || tag === 'select';
}

export function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { theme, setTheme, options: themeOptions } = useTheme();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const goSequenceRef = useRef<{ active: boolean; timer: number | null }>({
    active: false,
    timer: null,
  });

  const currentGroupId = useMemo(() => {
    const match = location.pathname.match(/^\/groups\/(\d+)$/);
    return match ? Number(match[1]) : null;
  }, [location.pathname]);

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: Terminal },
    { path: '/servers', label: 'Servers', icon: Server },
    { path: '/groups', label: 'Groups', icon: Layers },
    { path: '/scripts', label: 'Scripts', icon: FileCode },
    { path: '/tasks', label: 'Tasks', icon: List },
  ];

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const resetGoSequence = () => {
      if (goSequenceRef.current.timer) {
        window.clearTimeout(goSequenceRef.current.timer);
      }
      goSequenceRef.current = { active: false, timer: null };
    };

    const startGoSequence = () => {
      resetGoSequence();
      goSequenceRef.current.active = true;
      goSequenceRef.current.timer = window.setTimeout(() => {
        resetGoSequence();
      }, 900);
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      const isPaletteShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k';
      if (isPaletteShortcut) {
        event.preventDefault();
        setPaletteOpen((open) => !open);
        return;
      }

      if (event.key === 'Escape') {
        if (paletteOpen) {
          event.preventDefault();
          setPaletteOpen(false);
        }
        if (mobileMenuOpen) {
          event.preventDefault();
          setMobileMenuOpen(false);
        }
      }

      if (paletteOpen) {
        return;
      }

      const editable = isEditableTarget(event.target);
      if (editable) {
        return;
      }

      if (event.key === '/' && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault();
        window.dispatchEvent(new CustomEvent('nto:focus-search'));
        return;
      }

      if (event.key.toLowerCase() === 'r' && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault();
        window.dispatchEvent(new CustomEvent('nto:poll-refresh'));
        return;
      }

      const key = event.key.toLowerCase();
      if (key === 'g' && !event.ctrlKey && !event.metaKey && !event.altKey) {
        startGoSequence();
        return;
      }

      if (!goSequenceRef.current.active) {
        return;
      }

      const destination: Record<string, string> = {
        d: '/dashboard',
        s: '/servers',
        g: '/groups',
        t: '/tasks',
      };

      if (destination[key]) {
        event.preventDefault();
        navigate(destination[key]);
        resetGoSequence();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      if (goSequenceRef.current.timer) {
        window.clearTimeout(goSequenceRef.current.timer);
      }
    };
  }, [mobileMenuOpen, navigate, paletteOpen]);

  const renderNavItem = (path: string, label: string, Icon: typeof Terminal, mobile = false) => {
    const isActive = location.pathname.startsWith(path);
    return (
      <Link
        key={path}
        to={path}
        className={`flex items-center ${
          mobile ? 'w-full justify-start' : ''
        } space-x-2 px-3 py-2 rounded font-mono text-sm transition-colors ${
          isActive
            ? 'bg-green-900 text-green-300'
            : 'text-green-600 hover:bg-gray-800 hover:text-green-400'
        }`}
      >
        <Icon className="w-4 h-4" />
        <span>{label}</span>
      </Link>
    );
  };

  return (
    <div className="min-h-screen bg-black text-green-500">
      <nav className="bg-gray-900 border-b border-green-900">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-8">
              <div className="flex items-center space-x-2">
                <Terminal className="w-6 h-6" />
                <span className="font-mono text-lg font-bold">NTO</span>
              </div>
              <div className="hidden md:flex space-x-1">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  return renderNavItem(item.path, item.label, Icon);
                })}
              </div>
            </div>
            <div className="hidden md:flex items-center gap-3">
              <label className="text-xs font-mono text-green-700" htmlFor="theme-select-desktop">
                Theme
              </label>
              <select
                id="theme-select-desktop"
                value={theme}
                onChange={(event) => setTheme(event.target.value as AppTheme)}
                className="bg-black border border-green-900 rounded px-2 py-1.5 text-green-400 text-xs font-mono focus:outline-none focus:border-green-500"
              >
                {themeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <button
                onClick={logout}
                className="flex items-center space-x-2 px-3 py-2 rounded font-mono text-sm text-red-500 hover:bg-gray-800 transition-colors"
              >
                <LogOut className="w-4 h-4" />
                <span>Logout</span>
              </button>
            </div>
            <button
              type="button"
              onClick={() => setMobileMenuOpen((open) => !open)}
              className="md:hidden p-2 rounded text-green-400 hover:bg-gray-800 transition-colors"
              aria-expanded={mobileMenuOpen}
              aria-label="Toggle navigation menu"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
          {mobileMenuOpen && (
            <div className="md:hidden border-t border-green-900 py-3 space-y-2 animate-page-enter">
              <div className="space-y-1">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  return renderNavItem(item.path, item.label, Icon, true);
                })}
              </div>
              <div className="px-1">
                <label className="block text-xs font-mono text-green-700 mb-2" htmlFor="theme-select-mobile">
                  Theme
                </label>
                <select
                  id="theme-select-mobile"
                  value={theme}
                  onChange={(event) => setTheme(event.target.value as AppTheme)}
                  className="w-full bg-black border border-green-900 rounded px-3 py-2 text-green-400 text-sm font-mono focus:outline-none focus:border-green-500"
                >
                  {themeOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <button
                onClick={logout}
                className="w-full flex items-center justify-center space-x-2 px-3 py-2 rounded font-mono text-sm text-red-400 bg-gray-800 hover:bg-gray-700 transition-colors"
              >
                <LogOut className="w-4 h-4" />
                <span>Logout</span>
              </button>
            </div>
          )}
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 py-8">
        <div key={location.pathname} className="animate-page-enter">
          <Outlet />
        </div>
      </main>
      <CommandPalette
        open={paletteOpen}
        canPingCurrentGroup={currentGroupId !== null}
        onClose={() => setPaletteOpen(false)}
        onPingCurrentGroup={() => {
          if (currentGroupId === null) {
            return;
          }
          window.dispatchEvent(new CustomEvent('nto:group-ping', { detail: { groupId: currentGroupId } }));
        }}
      />
    </div>
  );
}
