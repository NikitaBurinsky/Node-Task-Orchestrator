import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

interface HistoryStateLike {
  idx?: number;
}

export function useSafeBack(fallbackPath: string) {
  const navigate = useNavigate();

  return useCallback(() => {
    const historyState = window.history.state as HistoryStateLike | null;
    const canGoBack = typeof historyState?.idx === 'number' && historyState.idx > 0;

    if (canGoBack) {
      navigate(-1);
      return;
    }

    navigate(fallbackPath, { replace: true });
  }, [navigate, fallbackPath]);
}
