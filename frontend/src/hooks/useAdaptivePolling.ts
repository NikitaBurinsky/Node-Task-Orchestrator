import { useEffect, useRef, useState } from 'react';

interface AdaptivePollingOptions {
  enabled?: boolean;
  baseIntervalMs: number;
  maxIntervalMs?: number;
  runImmediately?: boolean;
  onSuccess?: () => void;
  onError?: (error: unknown, attempt: number) => void;
}

interface AdaptivePollingState {
  lastSuccessAt: number | null;
  consecutiveErrors: number;
  isPaused: boolean;
}

export function useAdaptivePolling(
  callback: () => Promise<void>,
  options: AdaptivePollingOptions
): AdaptivePollingState {
  const {
    enabled = true,
    baseIntervalMs,
    maxIntervalMs = 30000,
    runImmediately = true,
    onSuccess,
    onError,
  } = options;

  const [lastSuccessAt, setLastSuccessAt] = useState<number | null>(null);
  const [consecutiveErrors, setConsecutiveErrors] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const cancelledRef = useRef(false);
  const errorCountRef = useRef(0);

  useEffect(() => {
    errorCountRef.current = consecutiveErrors;
  }, [consecutiveErrors]);

  useEffect(() => {
    if (!enabled) {
      setIsPaused(false);
      setConsecutiveErrors(0);
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      return;
    }

    cancelledRef.current = false;

    const clearTimer = () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };

    const schedule = (delayMs: number) => {
      clearTimer();
      timerRef.current = setTimeout(runCycle, delayMs);
    };

    const getNextDelay = () => {
      const exponentialBackoff = baseIntervalMs * 2 ** errorCountRef.current;
      return Math.min(exponentialBackoff, maxIntervalMs);
    };

    const runCycle = async () => {
      if (cancelledRef.current || !enabled) {
        return;
      }

      if (typeof document !== 'undefined' && document.hidden) {
        setIsPaused(true);
        schedule(baseIntervalMs);
        return;
      }

      setIsPaused(false);

      try {
        await callback();
        errorCountRef.current = 0;
        setConsecutiveErrors(0);
        setLastSuccessAt(Date.now());
        onSuccess?.();
      } catch (error) {
        const nextErrorCount = Math.min(errorCountRef.current + 1, 6);
        errorCountRef.current = nextErrorCount;
        setConsecutiveErrors(nextErrorCount);
        onError?.(error, nextErrorCount);
      }

      schedule(getNextDelay());
    };

    const handleVisibilityChange = () => {
      if (document.hidden) {
        setIsPaused(true);
        return;
      }

      setIsPaused(false);
      schedule(250);
    };

    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', handleVisibilityChange);
    }

    schedule(runImmediately ? 0 : baseIntervalMs);

    return () => {
      cancelledRef.current = true;
      clearTimer();
      if (typeof document !== 'undefined') {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
      }
    };
  }, [callback, enabled, baseIntervalMs, maxIntervalMs, onError, onSuccess, runImmediately]);

  return {
    lastSuccessAt,
    consecutiveErrors,
    isPaused,
  };
}
