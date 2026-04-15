import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ChevronRight, ArrowLeft } from 'lucide-react';

const segmentLabels: Record<string, string> = {
  dashboard: 'Dashboard',
  servers: 'Servers',
  groups: 'Groups',
  scripts: 'Scripts',
  tasks: 'Tasks',
};

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  onBack?: () => void;
  backLabel?: string;
  currentLabel?: string;
}

interface BreadcrumbItem {
  path: string;
  label: string;
}

function toTitleCase(value: string) {
  if (!value) return value;
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function resolveDynamicLabel(path: string, segment: string) {
  if (/^\d+$/.test(segment)) {
    if (path.startsWith('/tasks/')) {
      return `Task #${segment}`;
    }
    if (path.startsWith('/groups/')) {
      return `Group #${segment}`;
    }
  }
  return toTitleCase(segment);
}

function useBreadcrumbs(currentLabel?: string) {
  const location = useLocation();
  const segments = location.pathname.split('/').filter(Boolean);

  const items: BreadcrumbItem[] = [];
  let currentPath = '';

  for (const [index, segment] of segments.entries()) {
    currentPath += `/${segment}`;
    const isLast = index === segments.length - 1;
    const defaultLabel = segmentLabels[segment] ?? resolveDynamicLabel(currentPath, segment);

    items.push({
      path: currentPath,
      label: isLast && currentLabel ? currentLabel : defaultLabel,
    });
  }

  return items;
}

export function PageHeader({
  title,
  subtitle,
  actions,
  onBack,
  backLabel = 'Back',
  currentLabel,
}: PageHeaderProps) {
  const breadcrumbs = useBreadcrumbs(currentLabel);

  return (
    <div className="space-y-3">
      {breadcrumbs.length > 0 && (
        <div className="flex items-center flex-wrap gap-2 text-xs font-mono text-green-700">
          {breadcrumbs.map((crumb, index) => {
            const isLast = index === breadcrumbs.length - 1;
            return (
              <div key={crumb.path} className="flex items-center gap-2">
                {isLast ? (
                  <span className="text-green-500">{crumb.label}</span>
                ) : (
                  <Link to={crumb.path} className="hover:text-green-500 transition-colors">
                    {crumb.label}
                  </Link>
                )}
                {!isLast && <ChevronRight className="w-3 h-3" />}
              </div>
            );
          })}
        </div>
      )}

      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          {onBack && (
            <button
              type="button"
              onClick={onBack}
              className="mt-1 text-green-500 hover:text-green-400 transition-colors inline-flex items-center gap-1"
              aria-label={backLabel}
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
          )}
          <div>
            <h1 className="text-3xl font-bold text-green-500 font-mono">{title}</h1>
            {subtitle && <p className="text-green-700 font-mono mt-1">{subtitle}</p>}
          </div>
        </div>
        {actions && <div>{actions}</div>}
      </div>
    </div>
  );
}
