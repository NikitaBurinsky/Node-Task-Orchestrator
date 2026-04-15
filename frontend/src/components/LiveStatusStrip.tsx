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
