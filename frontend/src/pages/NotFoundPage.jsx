import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <p className="text-6xl font-bold text-brick">404</p>
      <p className="text-xl">This plate is empty. We couldn't find that page.</p>
      <Link to="/" className="bg-forest px-5 py-2 font-medium text-white shadow-[4px_4px_0_0_var(--color-tangerine)]">Back to home</Link>
    </div>
  );
}
