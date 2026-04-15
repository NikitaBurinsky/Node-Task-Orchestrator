import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';

type ConfirmTone = 'default' | 'danger';

interface ConfirmOptions {
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  tone?: ConfirmTone;
}

interface ConfirmDialogContextType {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

interface ActiveConfirmState extends Required<Omit<ConfirmOptions, 'description'>> {
  description: string;
}

const DEFAULT_OPTIONS: ActiveConfirmState = {
  title: 'Confirm action',
  description: '',
  confirmText: 'Confirm',
  cancelText: 'Cancel',
  tone: 'default',
};

const ConfirmDialogContext = createContext<ConfirmDialogContextType | undefined>(undefined);

export function ConfirmDialogProvider({ children }: { children: ReactNode }) {
  const [dialogState, setDialogState] = useState<ActiveConfirmState | null>(null);
  const resolverRef = useRef<((value: boolean) => void) | null>(null);
  const cancelButtonRef = useRef<HTMLButtonElement | null>(null);
  const dialogRef = useRef<HTMLDivElement | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const closeDialog = useCallback((result: boolean) => {
    const resolver = resolverRef.current;
    resolverRef.current = null;
    setDialogState(null);
    resolver?.(result);

    const focusTarget = previousFocusRef.current;
    if (focusTarget) {
      window.setTimeout(() => {
        focusTarget.focus();
      }, 0);
    }
  }, []);

  const confirm = useCallback((options: ConfirmOptions) => {
    if (resolverRef.current) {
      resolverRef.current(false);
      resolverRef.current = null;
    }

    return new Promise<boolean>((resolve) => {
      resolverRef.current = resolve;
      setDialogState({
        ...DEFAULT_OPTIONS,
        ...options,
        description: options.description ?? '',
      });
    });
  }, []);

  useEffect(() => {
    if (!dialogState) return;

    previousFocusRef.current = document.activeElement as HTMLElement | null;
    cancelButtonRef.current?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeDialog(false);
        return;
      }

      if (event.key !== 'Tab') {
        return;
      }

      const dialogNode = dialogRef.current;
      if (!dialogNode) {
        return;
      }

      const focusable = dialogNode.querySelectorAll<HTMLElement>(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );

      if (focusable.length === 0) {
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement as HTMLElement | null;

      if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }

      if (event.shiftKey && active === first) {
        event.preventDefault();
        last.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [closeDialog, dialogState]);

  useEffect(() => {
    return () => {
      if (resolverRef.current) {
        resolverRef.current(false);
      }
    };
  }, []);

  const contextValue = useMemo(() => ({ confirm }), [confirm]);

  return (
    <ConfirmDialogContext.Provider value={contextValue}>
      {children}
      {dialogState && (
        <div
          className="fixed inset-0 z-[70] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-page-enter"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeDialog(false);
            }
          }}
        >
          <div
            ref={dialogRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-dialog-title"
            className="w-full max-w-md bg-gray-900 border border-green-900 rounded-lg p-6 shadow-2xl"
          >
            <div className="flex items-start gap-3">
              <AlertTriangle
                className={`w-5 h-5 mt-0.5 ${
                  dialogState.tone === 'danger' ? 'text-red-400' : 'text-yellow-400'
                }`}
              />
              <div>
                <h2 id="confirm-dialog-title" className="text-green-300 font-mono text-lg font-bold">
                  {dialogState.title}
                </h2>
                {dialogState.description && (
                  <p className="text-green-700 font-mono text-sm mt-2">{dialogState.description}</p>
                )}
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button
                ref={cancelButtonRef}
                type="button"
                onClick={() => closeDialog(false)}
                className="px-4 py-2 rounded font-mono text-sm bg-gray-800 text-gray-300 hover:bg-gray-700 transition-colors"
              >
                {dialogState.cancelText}
              </button>
              <button
                type="button"
                onClick={() => closeDialog(true)}
                className={`px-4 py-2 rounded font-mono text-sm transition-colors ${
                  dialogState.tone === 'danger'
                    ? 'bg-red-900 text-red-200 hover:bg-red-800'
                    : 'bg-green-900 text-green-300 hover:bg-green-800'
                }`}
              >
                {dialogState.confirmText}
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmDialogContext.Provider>
  );
}

export function useConfirmDialog() {
  const context = useContext(ConfirmDialogContext);
  if (!context) {
    throw new Error('useConfirmDialog must be used within ConfirmDialogProvider');
  }
  return context;
}
