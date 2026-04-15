import { useEffect, useState } from 'react';
import { Link, useLocation, Outlet } from 'react-router-dom';
import { Terminal, Server, Layers, FileCode, List, LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export function Layout() {
  const location = useLocation();
  const { logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

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
            <button
              onClick={logout}
              className="hidden md:flex items-center space-x-2 px-3 py-2 rounded font-mono text-sm text-red-500 hover:bg-gray-800 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
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
    </div>
  );
}
