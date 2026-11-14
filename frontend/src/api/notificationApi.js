import axiosClient from './axiosClient';

export const notificationApi = {
  list: () => axiosClient.get('/notifications').then((r) => r.data),
  markAllRead: () => axiosClient.post('/notifications/read-all').then((r) => r.data),
};
