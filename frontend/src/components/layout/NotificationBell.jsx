import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../api/notificationApi';
import { ASSET_PATHS } from '../../utils/constants';
import { formatRelativeTime } from '../../utils/formatters';

export default function NotificationBell() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  const { data } = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationApi.list,
    refetchInterval: 30_000, // backend generates time-based reminders; poll for them
  });

  const markAllRead = useMutation({
    mutationFn: notificationApi.markAllRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  useEffect(() => {
    const onClickAway = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickAway);
    return () => document.removeEventListener('mousedown', onClickAway);
  }, []);

  const unread = data?.unreadCount ?? 0;
  const notifications = data?.notifications ?? [];

  return (
    <div className="relative" ref={containerRef}>
      <button type="button" aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`} aria-expanded={open}
        onClick={() => setOpen((v) => !v)} className="relative block">
        <img src={ASSET_PATHS.icons.bell} alt="" className="h-11 w-11 sm:h-14 sm:w-14" />
        {unread > 0 && (
          <span className="absolute -right-1 -top-1 flex h-6 min-w-6 items-center justify-center rounded-full bg-brick px-1 text-xs font-bold text-white">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="pp-pop absolute right-0 z-50 mt-3 w-80 max-w-[85vw] border-2 border-forest bg-white shadow-[6px_6px_0_0_var(--color-tangerine)]">
          <div className="flex items-center justify-between border-b-2 border-forest bg-butter px-4 py-2">
            <h2 className="font-semibold text-forest">Notifications</h2>
            <button type="button" className="text-xs font-medium text-brick underline disabled:opacity-40"
              disabled={unread === 0} onClick={() => markAllRead.mutate()}>
              Mark all read
            </button>
          </div>
          <ul className="max-h-80 divide-y divide-sage/30 overflow-y-auto">
            {notifications.length === 0 && <li className="p-4 text-sm text-ink/70">You're all caught up. No notifications yet.</li>}
            {notifications.map((n) => (
              <li key={n.id} className={`p-3 text-sm ${n.read ? 'bg-white' : 'bg-cream'}`}>
                <div className="flex items-start gap-2">
                  {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brick" aria-label="unread" />}
                  <div>
                    <p className="font-semibold text-forest">{n.title}</p>
                    <p className="text-ink/80">{n.message}</p>
                    <p className="mt-1 text-xs text-ink/50">{formatRelativeTime(n.createdAt)}</p>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
