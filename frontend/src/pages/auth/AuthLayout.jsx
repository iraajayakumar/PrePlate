import ShadowCard from '../../components/common/ShadowCard';
import { ASSET_PATHS } from '../../utils/constants';

export default function AuthLayout({ title, children, footer }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-white p-4">
      <div className="w-full max-w-md">
        <img src={ASSET_PATHS.logo} alt="PrePlate" className="mx-auto mb-6 h-24" />
        <ShadowCard tone="peach" className="p-6 sm:p-8">
          <h1 className="mb-4 text-3xl font-semibold">{title}</h1>
          {children}
        </ShadowCard>
        <div className="mt-6 text-center text-sm">{footer}</div>
      </div>
    </div>
  );
}

export function TextField({ label, id, error, ...inputProps }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium">{label}</label>
      <input id={id} className={`w-full border-2 bg-white px-3 py-2 outline-none focus:border-forest ${error ? 'border-brick' : 'border-forest/40'}`}
        aria-invalid={Boolean(error)} {...inputProps} />
      {error && <p className="mt-1 text-xs text-brick">{error}</p>}
    </div>
  );
}

export const primaryButtonClass =
  'w-full bg-forest px-4 py-3 font-medium text-white shadow-[4px_4px_0_0_var(--color-brick)] transition hover:brightness-110 disabled:opacity-60';
