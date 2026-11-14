import axiosClient from './axiosClient';

export const mealApi = {
  getSchedule: () => axiosClient.get('/meal-schedule').then((r) => r.data),
  getUpcoming: () => axiosClient.get('/meals/upcoming').then((r) => r.data),
  setResponse: (mealId, status) =>
    axiosClient.put(`/meals/${mealId}/response`, { status }).then((r) => r.data),
  setDishPreferences: (mealId, dishIds) =>
    axiosClient.put(`/meals/${mealId}/dish-preferences`, { dishIds }).then((r) => r.data),
};
