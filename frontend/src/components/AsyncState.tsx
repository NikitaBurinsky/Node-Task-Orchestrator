import type { ReactNode } from 'react';
import { AlertTriangle, Loader2, Inbox } from 'lucide-react';

type AsyncStateKind = 'loading' | 'error' | 'empty';

interface AsyncStateProps {
  kind: AsyncStateKind;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  icon?: ReactNode;
}

function defaultIcon(kind: AsyncStateKind) {
  switch (kind) {
    case 'loading':
      return <Loader2 className="w-8 h-8 text-green-500 animate-spin" />;
    case 'error':
      return <AlertTriangle className="w-8 h-8 text-red-400" />;
    default:
      return <Inbox className="w-8 h-8 text-green-700" />;
  }
}

export function AsyncState({ kind, title, description, actionLabel, onAction, icon }: AsyncStateProps) {
  const style =
    kind === 'error'
      ? 'border-red-800 bg-red-950/30'
      : 'border-green-900 bg-gray-900';

  return (
    <div className={`rounded-lg border p-8 text-center font-mono ${style}`}>
      <div className="flex justify-center mb-3">{icon ?? defaultIcon(kind)}</div>
      <h3 className={`text-base font-bold ${kind === 'error' ? 'text-red-300' : 'text-green-400'}`}>
        {title}
      </h3>
      {description && (
        <p className={`text-sm mt-2 ${kind === 'error' ? 'text-red-400' : 'text-green-700'}`}>
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className={`mt-5 px-4 py-2 rounded text-sm transition-colors ${
            kind === 'error'
              ? 'bg-red-900 text-red-100 hover:bg-red-800'
              : 'bg-green-900 text-green-300 hover:bg-green-800'
          }`}
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
