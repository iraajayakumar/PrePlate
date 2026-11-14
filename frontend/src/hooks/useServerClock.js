import { useEffect, useRef, useState } from 'react';

/**
 * Returns a Date that ticks every second and is aligned with the SERVER clock.
 * Pass the `serverNow` ISO string from the API. This keeps countdowns correct
 * even when the backend uses a simulated time (preplate.dev.simulated-now).
 */
export function useServerClock(serverNow) {
  const skewMs = useRef(0);
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    if (serverNow) {
      skewMs.current = new Date(serverNow).getTime() - Date.now();
      setNow(new Date(Date.now() + skewMs.current));
    }
  }, [serverNow]);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date(Date.now() + skewMs.current)), 1000);
    return () => clearInterval(id);
  }, []);

  return now;
}
