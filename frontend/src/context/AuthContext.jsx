import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { TOKEN_KEY, USER_KEY, UNAUTHORIZED_EVENT } from '../api/axiosClient';
import { ROLES } from '../utils/constants';

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState(readStoredUser);

  const persistSession = useCallback((authResponse) => {
    localStorage.setItem(TOKEN_KEY, authResponse.token);
    localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
    setUser(authResponse.user);
  }, []);

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, clearSession);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, clearSession);
  }, [clearSession]);

  const login = useCallback(async (credentials) => {
    const response = await authApi.login(credentials);
    persistSession(response);
    return response.user;
  }, [persistSession]);

  const register = useCallback(async (payload) => {
    const response = await authApi.register(payload);
    persistSession(response);
    return response.user;
  }, [persistSession]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      /* token may already be invalid - still clear the local session */
    }
    clearSession();
  }, [clearSession]);

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(user), isAdmin: user?.role === ROLES.ADMIN, login, register, logout }),
    [user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
