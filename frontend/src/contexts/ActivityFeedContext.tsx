import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export type ActivityStatus = 'success' | 'error' | 'info';

export interface ActivityItem {
  id: string;
  at: number;
  title: string;
  details?: string;
  status: ActivityStatus;
}

interface ActivityFeedContextType {
  activities: ActivityItem[];
  addActivity: (activity: Omit<ActivityItem, 'id' | 'at'> & { at?: number }) => void;
  clearActivities: () => void;
}

const STORAGE_KEY = 'nto:activity-feed';
const MAX_ITEMS = 80;

const ActivityFeedContext = createContext<ActivityFeedContextType | undefined>(undefined);

function loadInitialActivities(): ActivityItem[] {
  if (typeof window === 'undefined') {
    return [];
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as ActivityItem[];
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((item) => typeof item?.id === 'string' && typeof item?.at === 'number');
  } catch {
    return [];
  }
}

export function ActivityFeedProvider({ children }: { children: ReactNode }) {
  const [activities, setActivities] = useState<ActivityItem[]>(() => loadInitialActivities());

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(activities));
  }, [activities]);

  const addActivity = useCallback(
    (activity: Omit<ActivityItem, 'id' | 'at'> & { at?: number }) => {
      const item: ActivityItem = {
        ...activity,
        id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        at: activity.at ?? Date.now(),
      };

      setActivities((current) => [item, ...current].slice(0, MAX_ITEMS));
    },
    []
  );

  const clearActivities = useCallback(() => {
    setActivities([]);
  }, []);

  const value = useMemo(
    () => ({
      activities,
      addActivity,
      clearActivities,
    }),
    [activities, addActivity, clearActivities]
  );

  return <ActivityFeedContext.Provider value={value}>{children}</ActivityFeedContext.Provider>;
}

export function useActivityFeed() {
  const context = useContext(ActivityFeedContext);
  if (!context) {
    throw new Error('useActivityFeed must be used within ActivityFeedProvider');
  }
  return context;
}
