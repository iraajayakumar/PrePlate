import axios from 'axios';

export const TOKEN_KEY = 'preplate.token';
export const USER_KEY = 'preplate.user';
export const UNAUTHORIZED_EVENT = 'preplate:unauthorized';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  timeout: 10000,
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginCall) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    return Promise.reject(error);
  },
);

/** Turn any axios error into a message that is safe to show to a user. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error) return fallback;
  if (error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED') {
    return 'Cannot reach the PrePlate server. Check that the backend is running and try again.';
  }
  return error.response?.data?.message ?? fallback;
}

export default axiosClient;
