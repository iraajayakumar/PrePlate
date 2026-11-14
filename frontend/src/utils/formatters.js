const pad = (n) => String(n).padStart(2, '0');

/** 4_857_000 ms -> "01 : 20 : 57". Negative values clamp to zero. */
export function formatCountdown(ms) {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return `${pad(hours)} : ${pad(minutes)} : ${pad(seconds)}`;
}

function ordinal(day) {
  if (day >= 11 && day <= 13) return `${day}th`;
  switch (day % 10) {
    case 1: return `${day}st`;
    case 2: return `${day}nd`;
    case 3: return `${day}rd`;
    default: return `${day}th`;
  }
}

/** Date -> "14th November 2026, 12:40 pm" */
export function formatDateTimeLong(date) {
  const month = date.toLocaleString('en-GB', { month: 'long' });
  const time = date.toLocaleString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true }).toLowerCase();
  return `${ordinal(date.getDate())} ${month} ${date.getFullYear()}, ${time}`;
}

/** "2026-11-14" -> "Sat, 14 Nov" (parsed as a local date, no timezone shift) */
export function formatMealDate(isoDate) {
  const [y, m, d] = isoDate.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' });
}

/** ISO date-time -> "2:00 PM" */
export function formatClockTime(isoDateTime) {
  return new Date(isoDateTime).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
}

/** Local Date -> "YYYY-MM-DD" (NOT toISOString, which shifts to UTC). */
export function toIsoDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function addDays(date, days) {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

export function formatRelativeTime(isoDateTime, now = new Date()) {
  const diffSeconds = Math.round((now.getTime() - new Date(isoDateTime).getTime()) / 1000);
  if (diffSeconds < 60) return 'Just now';
  const minutes = Math.floor(diffSeconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days > 1 ? 's' : ''} ago`;
}

export function firstName(fullName) {
  return (fullName ?? '').trim().split(/\s+/)[0] || 'there';
}

/** Sum nutrition of a list of dishes (used by My Plate). */
export function sumNutrition(dishes) {
  return dishes.reduce(
    (total, dish) => ({
      calories: total.calories + (dish.calories ?? 0),
      proteinG: total.proteinG + (dish.proteinG ?? 0),
      carbsG: total.carbsG + (dish.carbsG ?? 0),
      fatG: total.fatG + (dish.fatG ?? 0),
    }),
    { calories: 0, proteinG: 0, carbsG: 0, fatG: 0 },
  );
}

export const round1 = (n) => Math.round((n ?? 0) * 10) / 10;
