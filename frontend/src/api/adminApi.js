import axiosClient from './axiosClient';

export const adminApi = {
  getDishes: () => axiosClient.get('/admin/dishes').then((r) => r.data),
  createDish: (payload) => axiosClient.post('/admin/dishes', payload).then((r) => r.data),
  getMeals: (from, to) => axiosClient.get('/admin/meals', { params: { from, to } }).then((r) => r.data),
  createMeal: (payload) => axiosClient.post('/admin/meals', payload).then((r) => r.data),
  updateMeal: (mealId, dishIds) => axiosClient.put(`/admin/meals/${mealId}`, { dishIds }).then((r) => r.data),
  deleteMeal: (mealId) => axiosClient.delete(`/admin/meals/${mealId}`).then((r) => r.data),
  getDashboard: (mealDate, mealType) =>
    axiosClient.get('/admin/dashboard', { params: { mealDate, mealType } }).then((r) => r.data),
  refreshForecast: (mealId) => axiosClient.post(`/admin/forecasts/${mealId}/refresh`).then((r) => r.data),
  getConsumption: (mealId) => axiosClient.get(`/admin/consumption/${mealId}`).then((r) => r.data),
  saveConsumption: (payload) => axiosClient.post('/admin/consumption', payload).then((r) => r.data),
  getWasteAnalytics: (days) => axiosClient.get('/admin/analytics/waste', { params: { days } }).then((r) => r.data),
};
