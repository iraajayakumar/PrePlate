import { useEffect, useState } from 'react';
import { useFacts } from '../../hooks/useFacts';

export function Skeleton({ className = 'h-24 w-full' }) {
  return <div className={`pp-skeleton ${className}`} aria-hidden="true" />;
}

/** Skeleton + rotating sustainability fact for genuine loading moments. */
export function FactLoader({ label = 'Loading...' }) {
  const { facts } = useFacts();
  const [index, setIndex] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setIndex((i) => (i + 1) % facts.length), 4000);
    return () => clearInterval(id);
  }, [facts.length]);

  return (
    <div className="space-y-4" role="status" aria-label={label}>
      <Skeleton className="h-28 w-full" />
      <Skeleton className="h-20 w-full" />
      <Skeleton className="h-20 w-full" />
      <p className="bg-butter p-4 text-sm text-forest">
        <span className="font-semibold text-brick">Did you know? </span>
        {facts[index % facts.length]}
      </p>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div role="alert" className="border-2 border-brick bg-white p-6 text-center">
      <p className="font-semibold text-brick">Something went wrong</p>
      <p className="mt-1 text-sm text-ink/80">{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry}
          className="mt-4 bg-brick px-5 py-2 text-sm font-medium text-white shadow-[3px_3px_0_0_#000] hover:brightness-110">
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, message }) {
  return (
    <div className="border-2 border-dashed border-sage bg-white/60 p-6 text-center">
      <p className="font-semibold text-forest">{title}</p>
      {message && <p className="mt-1 text-sm text-ink/70">{message}</p>}
    </div>
  );
}
