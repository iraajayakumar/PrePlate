// Tailwind needs complete class strings, so every tone is spelled out in full.
const TONES = {
  peach: 'bg-peach shadow-[8px_8px_0_0_var(--color-brick)]',
  sage: 'bg-sage shadow-[8px_8px_0_0_var(--color-butter)]',
  butter: 'bg-butter shadow-[8px_8px_0_0_var(--color-tangerine)]',
  cream: 'bg-cream shadow-[6px_6px_0_0_var(--color-forest)]',
  white: 'bg-white border-2 border-forest shadow-[6px_6px_0_0_var(--color-forest)]',
};

export default function ShadowCard({ tone = 'cream', className = '', children, ...rest }) {
  return (
    <section className={`${TONES[tone]} ${className}`} {...rest}>
      {children}
    </section>
  );
}
