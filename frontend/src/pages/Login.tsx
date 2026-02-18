import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Terminal } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ username, password });
      navigate('/dashboard');
    } catch (err) {
      setError('Authentication failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-black flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-gray-900 border border-green-900 rounded-lg p-8 shadow-2xl">
          <div className="flex items-center justify-center space-x-3 mb-8">
            <Terminal className="w-12 h-12 text-green-500" />
            <div>
              <h1 className="text-2xl font-bold text-green-500 font-mono">NTO</h1>
              <p className="text-green-700 text-sm font-mono">Node Task Orchestrator</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">
                $ username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-black border border-green-900 rounded px-4 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500 transition-colors"
                placeholder="root"
                required
              />
            </div>

            <div>
              <label className="block text-green-500 font-mono text-sm mb-2">
                $ password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-black border border-green-900 rounded px-4 py-2 text-green-400 font-mono focus:outline-none focus:border-green-500 transition-colors"
                placeholder="••••••••"
                required
              />
            </div>

            {error && (
              <div className="bg-red-900 bg-opacity-20 border border-red-700 rounded p-3">
                <p className="text-red-400 font-mono text-sm">[ERROR] {error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-green-900 text-green-300 font-mono py-3 rounded hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '> authenticating...' : '> login'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-green-700 text-xs font-mono">
              Tip: Any credentials will work in demo mode
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
