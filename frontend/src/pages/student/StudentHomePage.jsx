import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mealApi } from '../../api/mealApi';
import { getErrorMessage } from '../../api/axiosClient';
import { useAuth } from '../../context/AuthContext';
import { useServerClock } from '../../hooks/useServerClock';
import { useToast } from '../../components/common/Toast';
import { EmptyState, ErrorState, FactLoader } from '../../components/common/StateViews';
import GreetingCard from '../../components/student/GreetingCard';
import FoodForThought from '../../components/student/FoodForThought';
import MealTabs from '../../components/student/MealTabs';
import DishCard from '../../components/student/DishCard';
import MyPlate from '../../components/student/MyPlate';
import ResponseModal from '../../components/student/ResponseModal';
import { MEAL_LABELS, RESPONSE_STATUS } from '../../utils/constants';
import { firstName, formatClockTime, formatMealDate } from '../../utils/formatters';

export default function StudentHomePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pickedType, setPickedType] = useState(null);
  const [pendingStatus, setPendingStatus] = useState(null);

  const upcomingQuery = useQuery({ queryKey: ['upcomingMeals'], queryFn: mealApi.getUpcoming, refetchInterval: 60_000 });
  const scheduleQuery = useQuery({ queryKey: ['mealSchedule'], queryFn: mealApi.getSchedule, staleTime: Infinity });

  const now = useServerClock(upcomingQuery.data?.serverNow);
  const meals = useMemo(() => upcomingQuery.data?.meals ?? [], [upcomingQuery.data]);

  // Default tab: the un-answered meal whose deadline is closest.
  const defaultType = useMemo(() => {
    const open = meals.filter((m) => !m.response).sort((a, b) => new Date(a.deadlineAt) - new Date(b.deadlineAt));
    return (open[0] ?? meals[0])?.mealType ?? 'BREAKFAST';
  }, [meals]);

  const activeType = pickedType ?? defaultType;
  const activeMeal = meals.find((m) => m.mealType === activeType);
  const msToDeadline = activeMeal ? new Date(activeMeal.deadlineAt) - now : 0;

  // When a deadline passes while the page is open, reload so the next occurrence appears.
  const { isFetching, refetch } = upcomingQuery;
  useEffect(() => {
    if (activeMeal && msToDeadline <= 0 && !isFetching) refetch();
  }, [activeMeal, msToDeadline, isFetching, refetch]);

  const refreshRelated = () => {
    ['upcomingMeals', 'notifications', 'studentHistory', 'studentAnalytics'].forEach((key) =>
      queryClient.invalidateQueries({ queryKey: [key] }));
  };

  const respondMutation = useMutation({
    mutationFn: ({ mealId, status }) => mealApi.setResponse(mealId, status),
    onSuccess: (_data, { status }) => {
      toast.success(status === RESPONSE_STATUS.ATTENDING
        ? `Your ${MEAL_LABELS[activeType].toLowerCase()} preference has been recorded.`
        : 'Your response has been saved.');
      refreshRelated();
    },
    onError: (error) => toast.error(getErrorMessage(error)),
    onSettled: () => setPendingStatus(null),
  });

  const plateMutation = useMutation({
    mutationFn: ({ mealId, dishIds }) => mealApi.setDishPreferences(mealId, dishIds),
    onMutate: async ({ mealId, dishIds }) => {
      await queryClient.cancelQueries({ queryKey: ['upcomingMeals'] });
      const previous = queryClient.getQueryData(['upcomingMeals']);
      queryClient.setQueryData(['upcomingMeals'], (old) => old && {
        ...old,
        meals: old.meals.map((m) => (m.mealId === mealId ? { ...m, selectedDishIds: dishIds } : m)),
      });
      return { previous };
    },
    onError: (error, _vars, context) => {
      if (context?.previous) queryClient.setQueryData(['upcomingMeals'], context.previous);
      toast.error(getErrorMessage(error));
    },
    onSettled: () => {
      ['upcomingMeals', 'studentHistory', 'studentAnalytics'].forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
    },
  });

  const toggleDish = (dishId) => {
    if (!activeMeal) return;
    const current = activeMeal.selectedDishIds ?? [];
    const dishIds = current.includes(dishId) ? current.filter((id) => id !== dishId) : [...current, dishId];
    plateMutation.mutate({ mealId: activeMeal.mealId, dishIds });
  };

  const plateDishes = activeMeal ? activeMeal.dishes.filter((d) => (activeMeal.selectedDishIds ?? []).includes(d.id)) : [];
  const canAdd = activeMeal?.response?.status === RESPONSE_STATUS.ATTENDING;

  return (
    <>
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
        <div className="lg:col-start-1">
          <GreetingCard firstName={firstName(user.fullName)} now={now} schedule={scheduleQuery.data} />
        </div>

        <aside className="space-y-6 lg:col-start-2 lg:row-span-2 lg:row-start-1">
          <FoodForThought />
          {activeMeal?.mealId && (
            <MyPlate meal={activeMeal} msToDeadline={msToDeadline} plateDishes={plateDishes}
              busy={respondMutation.isPending} onRespond={setPendingStatus} onRemoveDish={toggleDish} />
          )}
        </aside>

        <div className="space-y-4 lg:col-start-1">
          <h2 className="text-2xl font-semibold">Select your Meal</h2>
          {upcomingQuery.isLoading && <FactLoader label="Loading today's menu" />}
          {upcomingQuery.isError && <ErrorState message={getErrorMessage(upcomingQuery.error)} onRetry={() => upcomingQuery.refetch()} />}
          {upcomingQuery.isSuccess && (
            <>
              <MealTabs selected={activeType} onSelect={setPickedType} meals={meals} />
              {activeMeal && (
                <p className="text-sm text-ink/70">
                  {MEAL_LABELS[activeType]} · {formatMealDate(activeMeal.mealDate)} · confirm by <strong>{formatClockTime(activeMeal.deadlineAt)}</strong>
                </p>
              )}
              {(!activeMeal || activeMeal.dishes.length === 0) && (
                <EmptyState title={`No ${MEAL_LABELS[activeType].toLowerCase()} menu published yet`}
                  message="The kitchen has not added dishes for this meal. Please check back later." />
              )}
              <div className="space-y-5">
                {activeMeal?.dishes.map((dish) => (
                  <DishCard key={dish.id} dish={dish} canAdd={canAdd}
                    inPlate={(activeMeal.selectedDishIds ?? []).includes(dish.id)}
                    onToggle={toggleDish} disabled={plateMutation.isPending} />
                ))}
              </div>
            </>
          )}
        </div>
      </div>

      <ResponseModal status={pendingStatus} mealType={activeType} busy={respondMutation.isPending}
        onCancel={() => setPendingStatus(null)}
        onConfirm={() => respondMutation.mutate({ mealId: activeMeal.mealId, status: pendingStatus })} />
    </>
  );
}
