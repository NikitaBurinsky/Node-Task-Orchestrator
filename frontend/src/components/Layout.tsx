import { Link, useLocation, Outlet } from 'react-router-dom';
import { Terminal, Server, Layers, FileCode, List, LogOut } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export function Layout() {
  const location = useLocation();
  const { logout } = useAuth();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: Terminal },
    { path: '/servers', label: 'Servers', icon: Server },
    { path: '/groups', label: 'Groups', icon: Layers },
    { path: '/scripts', label: 'Scripts', icon: FileCode },
    { path: '/tasks', label: 'Tasks', icon: List },
  ];

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
              <div className="flex space-x-1">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  const isActive = location.pathname.startsWith(item.path);
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`flex items-center space-x-2 px-3 py-2 rounded font-mono text-sm transition-colors ${
                        isActive
                          ? 'bg-green-900 text-green-300'
                          : 'text-green-600 hover:bg-gray-800 hover:text-green-400'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
            <button
              onClick={logout}
              className="flex items-center space-x-2 px-3 py-2 rounded font-mono text-sm text-red-500 hover:bg-gray-800 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
