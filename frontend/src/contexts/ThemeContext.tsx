import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export type AppTheme = 'terminal' | 'amber' | 'day';

interface ThemeOption {
  value: AppTheme;
  label: string;
}

interface ThemeContextType {
  theme: AppTheme;
  setTheme: (theme: AppTheme) => void;
  options: ThemeOption[];
}

const STORAGE_KEY = 'nto:theme';
const DEFAULT_THEME: AppTheme = 'terminal';

const themeOptions: ThemeOption[] = [
  { value: 'terminal', label: 'Terminal Green' },
  { value: 'amber', label: 'Dark Amber' },
  { value: 'day', label: 'Blue Daylight' },
];

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

function isTheme(value: string): value is AppTheme {
  return value === 'terminal' || value === 'amber' || value === 'day';
}

function getInitialTheme(): AppTheme {
  if (typeof window === 'undefined') {
    return DEFAULT_THEME;
  }
  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored && isTheme(stored)) {
    return stored;
  }
  return DEFAULT_THEME;
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<AppTheme>(() => getInitialTheme());

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', theme);
    }
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(STORAGE_KEY, theme);
    }
  }, [theme]);

  const value = useMemo(
    () => ({
      theme,
      setTheme,
      options: themeOptions,
    }),
    [theme]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}
