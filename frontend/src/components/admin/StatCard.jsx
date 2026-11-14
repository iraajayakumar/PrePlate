const TONES = {
  sage: 'bg-sage text-white shadow-[5px_5px_0_0_var(--color-butter)]',
  brick: 'bg-brick text-white shadow-[5px_5px_0_0_var(--color-tangerine)]',
  tangerine: 'bg-tangerine text-ink shadow-[5px_5px_0_0_var(--color-brick)]',
  butter: 'bg-butter text-ink shadow-[5px_5px_0_0_var(--color-tangerine)]',
  white: 'bg-white border-2 border-forest text-ink shadow-[5px_5px_0_0_var(--color-forest)]',
};

export default function StatCard({ label, value, hint, tone = 'butter' }) {
  return (
    <div className={`p-4 ${TONES[tone]}`}>
      <p className="text-xs font-medium uppercase tracking-wide opacity-90">{label}</p>
      <p className="mt-1 text-3xl font-semibold tabular-nums">{value}</p>
      {hint && <p className="mt-1 text-xs opacity-80">{hint}</p>}
    </div>
  );
}
