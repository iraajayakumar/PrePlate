import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it } from 'vitest';
import App from '../App';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../components/common/Toast';

function renderApp(initialPath) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <AuthProvider>
          <ToastProvider>
            <App />
          </ToastProvider>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('App routing', () => {
  beforeEach(() => localStorage.clear());

  it('redirects a signed-out visitor to the login page', () => {
    renderApp('/');
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument();
  });

  it('shows the registration page at /register', () => {
    renderApp('/register');
    expect(screen.getByRole('heading', { name: 'Create your account' })).toBeInTheDocument();
  });

  it('shows the not-found page for unknown routes', () => {
    renderApp('/no-such-page');
    expect(screen.getByText(/couldn't find that page/i)).toBeInTheDocument();
  });
});