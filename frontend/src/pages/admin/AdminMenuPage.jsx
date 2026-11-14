import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/adminApi';
import { getErrorMessage } from '../../api/axiosClient';
import { useToast } from '../../components/common/Toast';
import { EmptyState, ErrorState, FactLoader } from '../../components/common/StateViews';
import { MEAL_LABELS, MEAL_TYPES } from '../../utils/constants';
import { addDays, formatClockTime, formatMealDate, toIsoDate } from '../../utils/formatters';

const EMPTY_DISH = { name: '', description: '', calories: '', proteinG: '', carbsG: '', fatG: '', ingredients: '', vegetarian: true };
const inputClass = 'w-full border-2 border-forest/40 bg-white px-2 py-1.5 text-sm';

export default function AdminMenuPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const today = new Date();
  const from = toIsoDate(today);
  const to = toIsoDate(addDays(today, 7));

  const mealsQuery = useQuery({ queryKey: ['adminMeals', from, to], queryFn: () => adminApi.getMeals(from, to) });
  const dishesQuery = useQuery({ queryKey: ['adminDishes'], queryFn: adminApi.getDishes });

  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ mealDate: from, mealType: 'LUNCH', dishIds: [] });
  const [dishForm, setDishForm] = useState(EMPTY_DISH);
  const [showDishForm, setShowDishForm] = useState(false);

  const invalidateMeals = () => queryClient.invalidateQueries({ queryKey: ['adminMeals'] });
  const resetForm = () => { setEditingId(null); setForm({ mealDate: from, mealType: 'LUNCH', dishIds: [] }); };

  const saveMeal = useMutation({
    mutationFn: () => (editingId ? adminApi.updateMeal(editingId, form.dishIds) : adminApi.createMeal(form)),
    onSuccess: () => { toast.success(editingId ? 'Meal updated.' : 'Meal created.'); invalidateMeals(); resetForm(); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });
  const deleteMeal = useMutation({
    mutationFn: (id) => adminApi.deleteMeal(id),
    onSuccess: () => { toast.success('Meal deleted.'); invalidateMeals(); resetForm(); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });
  const createDish = useMutation({
    mutationFn: () => adminApi.createDish({
      ...dishForm,
      calories: Number(dishForm.calories), proteinG: Number(dishForm.proteinG),
      carbsG: Number(dishForm.carbsG), fatG: Number(dishForm.fatG),
      ingredients: dishForm.ingredients.split(',').map((s) => s.trim()).filter(Boolean),
    }),
    onSuccess: () => {
      toast.success('Dish added to the catalogue.');
      queryClient.invalidateQueries({ queryKey: ['adminDishes'] });
      setDishForm(EMPTY_DISH); setShowDishForm(false);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const toggleDish = (id) => setForm((f) => ({ ...f, dishIds: f.dishIds.includes(id) ? f.dishIds.filter((x) => x !== id) : [...f.dishIds, id] }));
  const startEdit = (meal) => {
    setEditingId(meal.mealId);
    setForm({ mealDate: meal.mealDate, mealType: meal.mealType, dishIds: meal.dishes.map((x) => x.id) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  const setDish = (field) => (e) => setDishForm((f) => ({ ...f, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const grouped = (mealsQuery.data ?? []).reduce((acc, m) => { (acc[m.mealDate] ??= []).push(m); return acc; }, {});

  return (
    <div className="space-y-8">
      <h1 className="text-3xl font-semibold">Menu <span className="text-sage">Management</span></h1>

      <section className="bg-butter p-5 shadow-[6px_6px_0_0_var(--color-tangerine)]" aria-label="Meal editor">
        <h2 className="mb-3 text-xl font-semibold">{editingId ? 'Edit meal dishes' : 'Plan a meal'}</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-medium">Date
            <input type="date" className={inputClass} value={form.mealDate} disabled={Boolean(editingId)} onChange={(e) => setForm((f) => ({ ...f, mealDate: e.target.value }))} />
          </label>
          <label className="text-sm font-medium">Meal type
            <select className={inputClass} value={form.mealType} disabled={Boolean(editingId)} onChange={(e) => setForm((f) => ({ ...f, mealType: e.target.value }))}>
              {MEAL_TYPES.map((t) => <option key={t} value={t}>{MEAL_LABELS[t]}</option>)}
            </select>
          </label>
        </div>

        <p className="mb-2 mt-4 text-sm font-medium">Dishes ({form.dishIds.length} selected)</p>
        {dishesQuery.isLoading && <p className="text-sm">Loading dishes...</p>}
        <div className="grid max-h-64 gap-1 overflow-y-auto bg-white/70 p-3 sm:grid-cols-2 lg:grid-cols-3">
          {(dishesQuery.data ?? []).map((dish) => (
            <label key={dish.id} className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={form.dishIds.includes(dish.id)} onChange={() => toggleDish(dish.id)} />
              {dish.name} <span className="text-xs text-ink/50">{dish.calories} kcal</span>
            </label>
          ))}
        </div>

        <div className="mt-4 flex flex-wrap gap-3">
          <button type="button" disabled={saveMeal.isPending || form.dishIds.length === 0} onClick={() => saveMeal.mutate()}
            className="bg-forest px-5 py-2 font-medium text-white shadow-[3px_3px_0_0_var(--color-brick)] hover:brightness-110 disabled:opacity-50">
            {editingId ? 'Save changes' : 'Create meal'}
          </button>
          {editingId && <button type="button" onClick={resetForm} className="border-2 border-forest px-5 py-2 font-medium">Cancel</button>}
          <button type="button" onClick={() => setShowDishForm((v) => !v)} className="ml-auto underline">{showDishForm ? 'Hide new dish form' : '+ Add a new dish'}</button>
        </div>

        {showDishForm && (
          <form className="mt-4 grid gap-3 bg-white/80 p-4 sm:grid-cols-2" onSubmit={(e) => { e.preventDefault(); createDish.mutate(); }}>
            <label className="text-sm font-medium sm:col-span-2">Name<input required className={inputClass} value={dishForm.name} onChange={setDish('name')} /></label>
            <label className="text-sm font-medium sm:col-span-2">Description<input className={inputClass} value={dishForm.description} onChange={setDish('description')} /></label>
            {[['calories', 'Calories (kcal)'], ['proteinG', 'Protein (g)'], ['carbsG', 'Carbs (g)'], ['fatG', 'Fat (g)']].map(([key, label]) => (
              <label key={key} className="text-sm font-medium">{label}
                <input required type="number" min="0" step="0.1" className={inputClass} value={dishForm[key]} onChange={setDish(key)} />
              </label>
            ))}
            <label className="text-sm font-medium sm:col-span-2">Ingredients (comma separated)<input className={inputClass} value={dishForm.ingredients} onChange={setDish('ingredients')} /></label>
            <label className="flex items-center gap-2 text-sm font-medium"><input type="checkbox" checked={dishForm.vegetarian} onChange={setDish('vegetarian')} /> Vegetarian</label>
            <button type="submit" disabled={createDish.isPending} className="bg-sage px-4 py-2 font-medium text-white sm:col-span-2">Add dish</button>
          </form>
        )}
      </section>

      <section aria-label="Planned meals">
        <h2 className="mb-3 text-xl font-semibold">Planned meals - next 7 days</h2>
        {mealsQuery.isLoading && <FactLoader label="Loading meals" />}
        {mealsQuery.isError && <ErrorState message={getErrorMessage(mealsQuery.error)} onRetry={() => mealsQuery.refetch()} />}
        {mealsQuery.isSuccess && Object.keys(grouped).length === 0 && <EmptyState title="No meals planned" message="Use the form above to plan one." />}
        <div className="space-y-6">
          {Object.entries(grouped).map(([date, meals]) => (
            <div key={date}>
              <h3 className="mb-2 font-semibold text-forest">{formatMealDate(date)}</h3>
              <div className="grid gap-4 md:grid-cols-2">
                {meals.map((meal) => (
                  <article key={meal.mealId} className="bg-cream p-4 shadow-[5px_5px_0_0_var(--color-forest)]">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className="text-lg font-semibold">{MEAL_LABELS[meal.mealType]}</h4>
                        <p className="text-xs text-ink/60">Responses close {formatClockTime(meal.deadlineAt)}</p>
                      </div>
                      <div className="flex gap-3 text-sm">
                        <button type="button" onClick={() => startEdit(meal)} className="font-medium underline">Edit</button>
                        <button type="button" className="font-medium text-brick underline"
                          onClick={() => window.confirm(`Delete ${MEAL_LABELS[meal.mealType]} on ${date}? Student responses for it will be removed.`) && deleteMeal.mutate(meal.mealId)}>Delete</button>
                      </div>
                    </div>
                    <p className="mt-2 text-sm">{meal.dishes.map((x) => x.name).join(', ') || 'No dishes'}</p>
                  </article>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
