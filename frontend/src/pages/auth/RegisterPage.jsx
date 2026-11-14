import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getErrorMessage } from '../../api/axiosClient';
import AuthLayout, { TextField, primaryButtonClass } from './AuthLayout';

const EMPTY = { registrationNumber: '', fullName: '', department: '', hostelName: '', hostelRoom: '', password: '', confirmPassword: '' };

export function validateRegistration(form) {
  const errors = {};
  if (!form.registrationNumber.trim()) errors.registrationNumber = 'Registration number is required';
  if (!form.fullName.trim()) errors.fullName = 'Name is required';
  if (!form.department.trim()) errors.department = 'Department is required';
  if (!form.hostelName.trim()) errors.hostelName = 'Hostel name is required';
  if (!form.hostelRoom.trim()) errors.hostelRoom = 'Hostel room is required';
  if (form.password.length < 6) errors.password = 'Password must be at least 6 characters';
  if (form.password !== form.confirmPassword) errors.confirmPassword = 'Passwords do not match';
  return errors;
}

export default function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    const found = validateRegistration(form);
    setErrors(found);
    setServerError('');
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    try {
      // eslint-disable-next-line no-unused-vars
      const { confirmPassword, ...payload } = form;
      await register({ ...payload, registrationNumber: payload.registrationNumber.trim() });
      navigate('/', { replace: true });
    } catch (err) {
      setServerError(getErrorMessage(err, 'Registration failed. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  const field = (id, label, extra = {}) => (
    <TextField id={id} label={label} value={form[id]} onChange={update(id)} error={errors[id]} {...extra} />
  );

  return (
    <AuthLayout title="Create your account"
      footer={<p>Already registered? <Link to="/login" className="font-semibold text-brick underline">Sign in</Link></p>}>
      <form onSubmit={handleSubmit} className="space-y-3" noValidate>
        {field('fullName', 'Full name')}
        {field('registrationNumber', 'Registration number')}
        {field('department', 'Department')}
        <div className="grid grid-cols-2 gap-3">
          {field('hostelName', 'Hostel name')}
          {field('hostelRoom', 'Hostel room')}
        </div>
        {field('password', 'Password', { type: 'password', autoComplete: 'new-password' })}
        {field('confirmPassword', 'Confirm password', { type: 'password', autoComplete: 'new-password' })}
        {serverError && <p role="alert" className="bg-white p-2 text-sm font-medium text-brick">{serverError}</p>}
        <button type="submit" disabled={submitting} className={primaryButtonClass}>{submitting ? 'Creating account...' : 'Create account'}</button>
      </form>
    </AuthLayout>
  );
}
