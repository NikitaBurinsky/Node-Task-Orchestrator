import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

type ToastVariant = 'success' | 'error' | 'info';

interface Toast {
  id: number;
  message: string;
  variant: ToastVariant;
}

interface ToastContextType {
  showToast: (message: string, variant?: ToastVariant) => void;
}

const TOAST_TIMEOUT_MS = 3500;

const toastBaseClasses =
  'border rounded-lg px-4 py-3 shadow-lg font-mono text-sm flex items-start gap-3';

const toastVariantClasses: Record<ToastVariant, string> = {
  success: 'bg-green-950 border-green-700 text-green-300',
  error: 'bg-red-950 border-red-700 text-red-300',
  info: 'bg-gray-900 border-green-900 text-green-300',
};

const ToastContext = createContext<ToastContextType | undefined>(undefined);

function getVariantIcon(variant: ToastVariant) {
  switch (variant) {
    case 'success':
      return <CheckCircle2 className="w-4 h-4 mt-0.5" />;
    case 'error':
      return <AlertCircle className="w-4 h-4 mt-0.5" />;
    default:
      return <Info className="w-4 h-4 mt-0.5" />;
  }
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, variant: ToastVariant = 'info') => {
      const id = Date.now() + Math.floor(Math.random() * 1000);
      setToasts((current) => [...current, { id, message, variant }]);

      setTimeout(() => dismissToast(id), TOAST_TIMEOUT_MS);
    },
    [dismissToast]
  );

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed top-4 right-4 z-50 space-y-2 w-[320px] max-w-[calc(100vw-2rem)]">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`${toastBaseClasses} ${toastVariantClasses[toast.variant]}`}
            role="status"
            aria-live="polite"
          >
            {getVariantIcon(toast.variant)}
            <span className="flex-1">{toast.message}</span>
            <button
              type="button"
              onClick={() => dismissToast(toast.id)}
              className="text-current/80 hover:text-current transition-colors"
              aria-label="Dismiss notification"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
