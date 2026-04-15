import { Activity, PauseCircle, RefreshCw, AlertTriangle } from 'lucide-react';

export type LiveStripState = 'active' | 'paused' | 'backoff';

interface LiveStatusStripProps {
  state: LiveStripState;
  lastUpdatedAt: number | null;
  backoffAttempt?: number;
  onRefresh?: () => void;
  refreshLabel?: string;
}

function getStateMeta(state: LiveStripState, backoffAttempt: number) {
  if (state === 'paused') {
    return {
      icon: <PauseCircle className="w-4 h-4 text-yellow-400" />,
      text: 'Polling paused while tab is hidden.',
      className: 'border-yellow-900 bg-yellow-950/20 text-yellow-300',
    };
  }

  if (state === 'backoff') {
    return {
      icon: <AlertTriangle className="w-4 h-4 text-orange-400" />,
      text: `Connection unstable. Retrying with backoff (attempt ${backoffAttempt}).`,
      className: 'border-orange-900 bg-orange-950/20 text-orange-300',
    };
  }

  return {
    icon: <Activity className="w-4 h-4 text-green-400" />,
    text: 'Live polling active.',
    className: 'border-green-900 bg-gray-900 text-green-300',
  };
}

export function LiveStatusStrip({
  state,
  lastUpdatedAt,
  backoffAttempt = 0,
  onRefresh,
  refreshLabel = 'Refresh now',
}: LiveStatusStripProps) {
  if (state === 'active') {
    return (
      <div
        className="fixed bottom-4 right-4 z-30 px-2.5 py-2 rounded border border-green-900 bg-gray-900/90 backdrop-blur flex items-center gap-2 animate-page-enter"
        title={
          lastUpdatedAt
            ? `Auto-refresh enabled. Last updated: ${new Date(lastUpdatedAt).toLocaleTimeString()}`
            : 'Auto-refresh enabled'
        }
      >
        <Activity className="w-3.5 h-3.5 text-green-400" />
        {lastUpdatedAt && (
          <span className="hidden sm:inline text-[10px] leading-none text-green-700 font-mono">
            {new Date(lastUpdatedAt).toLocaleTimeString()}
          </span>
        )}
        {onRefresh && (
          <button
            type="button"
            onClick={onRefresh}
            className="inline-flex items-center justify-center w-6 h-6 rounded bg-black/60 border border-green-900 text-green-400 hover:bg-black transition-colors btn-operator"
            aria-label={refreshLabel}
            title={refreshLabel}
          >
            <RefreshCw className="w-3 h-3" />
          </button>
        )}
      </div>
    );
  }

  const meta = getStateMeta(state, backoffAttempt);

  return (
    <div
      className={`px-4 py-3 rounded-lg border flex items-center justify-between gap-3 flex-wrap animate-page-enter ${meta.className}`}
    >
      <div className="flex items-center gap-2">
        {meta.icon}
        <p className="font-mono text-xs">
          {meta.text}{' '}
          {lastUpdatedAt && <>Last updated: {new Date(lastUpdatedAt).toLocaleTimeString()}.</>}
        </p>
      </div>
      {onRefresh && (
        <button
          type="button"
          onClick={onRefresh}
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded bg-black/60 border border-current/30 text-xs font-mono hover:bg-black transition-colors btn-operator"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>{refreshLabel}</span>
        </button>
      )}
    </div>
  );
}
