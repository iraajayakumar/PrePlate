import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const ToastContext = createContext({ success: () => {}, error: () => {} });

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);

  const show = useCallback((type, message) => {
    const id = Date.now();
    setToast({ id, type, message });
    setTimeout(() => setToast((current) => (current?.id === id ? null : current)), 3500);
  }, []);

  const api = useMemo(() => ({ success: (m) => show('success', m), error: (m) => show('error', m) }), [show]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      {toast && (
        <div role="status" aria-live="polite"
          className={`pp-pop fixed bottom-24 left-1/2 z-[60] max-w-sm -translate-x-1/2 px-5 py-3 text-sm font-medium text-white shadow-[4px_4px_0_0_#000] lg:bottom-8 ${toast.type === 'error' ? 'bg-brick' : 'bg-forest'}`}>
          {toast.message}
        </div>
      )}
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
