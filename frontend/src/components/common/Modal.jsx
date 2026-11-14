import { useEffect } from 'react';

export default function Modal({ open, onClose, children, label }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-sage/70 p-4 backdrop-blur-sm" onClick={onClose} role="presentation">
      <div role="dialog" aria-modal="true" aria-label={label}
        className="pp-pop w-full max-w-lg bg-white p-6 shadow-[8px_8px_0_0_#000] sm:p-8"
        onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}
