import axiosClient from './axiosClient';

export const studentApi = {
  getHistory: (period) => axiosClient.get('/student/history', { params: { period } }).then((r) => r.data),
  getAnalytics: (scope) => axiosClient.get('/student/analytics', { params: { scope } }).then((r) => r.data),
};
