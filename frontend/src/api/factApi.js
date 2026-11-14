import axiosClient from './axiosClient';

export const factApi = {
  list: () => axiosClient.get('/sustainability-facts').then((r) => r.data),
};
