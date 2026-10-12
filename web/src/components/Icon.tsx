export type IconName = 'people' | 'organization' | 'enterprise' | 'roles' | 'tags' | 'search' | 'plus' | 'arrow' | 'logout' | 'close' | 'check' | 'refresh' | 'chevron' | 'shield' | 'layers' | 'settings'
const paths: Record<IconName, string> = {
  people: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',
  organization: 'M9 3h6v5H9z M3 16h6v5H3z M15 16h6v5h-6z M12 8v4 M6 16v-4h12v4',
  enterprise: 'M4 21V4h12v17 M16 10h4v11 M2 21h20 M8 8h4 M8 12h4 M8 16h4',
  roles: 'M12 3l8 4v5c0 5-8 9-8 9s-8-4-8-9V7z M9 12l2 2 4-4',
  tags: 'M3 3h8l10 10-8 8L3 11z M7 7h.01',
  search: 'M21 21l-5-5 M18 10a8 8 0 1 0-16 0 8 8 0 0 0 16 0',
  plus: 'M12 5v14 M5 12h14', arrow: 'M5 12h14 M13 6l6 6-6 6',
  logout: 'M9 3H4v18h5 M10 12h11 M17 8l4 4-4 4', close: 'M6 6l12 12 M6 18L18 6',
  check: 'M5 12l4 4L19 6', refresh: 'M20 7v5h-5 M4 17v-5h5 M6 6a8 8 0 0 1 13 2 M18 18a8 8 0 0 1-13-2',
  chevron: 'M9 5l7 7-7 7', shield: 'M12 3l8 4v5c0 5-8 9-8 9s-8-4-8-9V7z',
  settings: 'M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1z M15 12a3 3 0 1 0-6 0 3 3 0 0 0 6 0',
  layers: 'M12 3L2 8l10 5 10-5z M2 12l10 5 10-5 M2 16l10 5 10-5',
}
export function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
