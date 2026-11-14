import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getErrorMessage } from '../../api/axiosClient';
import AuthLayout, { TextField, primaryButtonClass } from './AuthLayout';

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ registrationNumber: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />;

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const loggedIn = await login({ registrationNumber: form.registrationNumber.trim(), password: form.password });
      navigate(loggedIn.role === 'ADMIN' ? '/admin' : '/', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'Login failed. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Welcome back"
      footer={<p>New to PrePlate? <Link to="/register" className="font-semibold text-brick underline">Create an account</Link></p>}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <TextField id="registrationNumber" label="Registration number" value={form.registrationNumber} onChange={update('registrationNumber')} autoComplete="username" required />
        <TextField id="password" label="Password" type="password" value={form.password} onChange={update('password')} autoComplete="current-password" required />
        {error && <p role="alert" className="bg-white p-2 text-sm font-medium text-brick">{error}</p>}
        <button type="submit" disabled={submitting || !form.registrationNumber || !form.password} className={primaryButtonClass}>
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
      <div className="mt-5 bg-butter p-3 text-xs text-forest">
        <p className="font-semibold">Demo accounts</p>
        <p>Student: <code>RA2111003010001</code> / <code>student123</code></p>
        <p>Kitchen admin: <code>ADMIN001</code> / <code>admin123</code></p>
      </div>
    </AuthLayout>
  );
}
