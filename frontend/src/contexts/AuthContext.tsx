import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authApi, setAccessToken, setAuthFailureHandler } from '../services/api';
import type { AuthRequestDto } from '../types/api';

interface AuthContextType {
  isAuthenticated: boolean;
  token: string | null;
  login: (credentials: AuthRequestDto) => Promise<void>;
  logout: () => void;
  loading: boolean;
  register: (credentials: AuthRequestDto) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const applyToken = (newToken: string | null) => {
    setToken(newToken);
    setAccessToken(newToken);
  };

  useEffect(() => {
    setAuthFailureHandler(() => applyToken(null));
    return () => setAuthFailureHandler(null);
  }, []);

  useEffect(() => {
    const initialize = async () => {
      try {
        const response = await authApi.refresh();
        const newToken = response.data.accessToken ?? null;
        if (newToken) {
          applyToken(newToken);
        } else {
          applyToken(null);
        }
      } catch {
        applyToken(null);
      } finally {
        setLoading(false);
      }
    };

    initialize();
  }, []);

  const login = async (credentials: AuthRequestDto) => {
    const response = await authApi.login(credentials);
    const newToken = response.data.accessToken ?? null;
    applyToken(newToken);
  };

  const register = async (credentials: AuthRequestDto) => {
    const response = await authApi.register(credentials);
    const newToken = response.data.accessToken ?? null;
    applyToken(newToken);
  };

  const logout = () => {
    authApi.logout().finally(() => applyToken(null));
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: !!token,
        token,
        login,
        logout,
        loading,
        register,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
