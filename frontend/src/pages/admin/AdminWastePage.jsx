import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminApi } from '../../api/adminApi';
import { getErrorMessage } from '../../api/axiosClient';
import { useToast } from '../../components/common/Toast';
import { EmptyState, ErrorState, FactLoader } from '../../components/common/StateViews';
import StatCard from '../../components/admin/StatCard';
import MealTypeTabs from '../../components/admin/MealTypeTabs';
import { MEAL_LABELS } from '../../utils/constants';
import { toIsoDate } from '../../utils/formatters';

const nf = (n) => (n ?? 0).toLocaleString('en-IN');
const cell = 'w-28 border-2 border-forest/40 bg-white px-2 py-1 text-right';

/** Editable table for one meal. Re-mounted (via key) whenever fresh data arrives. */
function ConsumptionForm({ mealId, initialRows, onSaved }) {
  const toast = useToast();
  const [rows, setRows] = useState(initialRows.map((r) => ({ ...r, prepared: r.preparedQuantity ?? r.recommendedQuantity ?? '', consumed: r.consumedQuantity ?? '' })));
  const setValue = (dishId, field, value) => setRows((rs) => rs.map((r) => (r.dishId === dishId ? { ...r, [field]: value } : r)));
  const invalid = rows.some((r) => r.prepared === '' || r.consumed === '' || Number(r.consumed) < 0 || Number(r.consumed) > Number(r.prepared));

  const save = useMutation({
    mutationFn: () => adminApi.saveConsumption({
      mealId,
      records: rows.map((r) => ({ dishId: r.dishId, preparedQuantity: Number(r.prepared), consumedQuantity: Number(r.consumed) })),
    }),
    onSuccess: () => { toast.success('Consumption recorded. Waste analytics updated.'); onSaved(); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <>
      <div className="overflow-x-auto border-2 border-forest bg-white">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-forest text-white">
            <tr><th className="p-3">Dish</th><th className="p-3 text-right">Recommended</th><th className="p-3 text-right">Prepared</th><th className="p-3 text-right">Consumed</th><th className="p-3 text-right">Leftover</th></tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              const leftover = r.prepared === '' || r.consumed === '' ? '-' : Number(r.prepared) - Number(r.consumed);
              return (
                <tr key={r.dishId} className="border-t border-forest/20">
                  <td className="p-3 font-medium">{r.dishName}</td>
                  <td className="p-3 text-right tabular-nums">{r.recommendedQuantity ?? '-'}</td>
                  <td className="p-3 text-right"><input aria-label={`Prepared ${r.dishName}`} type="number" min="0" className={cell} value={r.prepared} onChange={(e) => setValue(r.dishId, 'prepared', e.target.value)} /></td>
                  <td className="p-3 text-right"><input aria-label={`Consumed ${r.dishName}`} type="number" min="0" className={cell} value={r.consumed} onChange={(e) => setValue(r.dishId, 'consumed', e.target.value)} /></td>
                  <td className={`p-3 text-right font-semibold tabular-nums ${typeof leftover === 'number' && leftover < 0 ? 'text-brick' : ''}`}>{leftover}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="mt-3 flex items-center gap-4">
        <button type="button" disabled={invalid || save.isPending} onClick={() => save.mutate()}
          className="bg-forest px-5 py-2 font-medium text-white shadow-[3px_3px_0_0_var(--color-brick)] hover:brightness-110 disabled:opacity-50">
          {save.isPending ? 'Saving...' : 'Save actuals'}
        </button>
        {invalid && <span className="text-xs text-ink/60">Fill prepared and consumed for every dish (consumed cannot exceed prepared).</span>}
      </div>
    </>
  );
}

export default function AdminWastePage() {
  const queryClient = useQueryClient();
  const [date, setDate] = useState(toIsoDate(new Date()));
  const [mealType, setMealType] = useState('LUNCH');
  const [days, setDays] = useState(14);

  const mealsQuery = useQuery({ queryKey: ['adminMeals', date, date], queryFn: () => adminApi.getMeals(date, date) });
  const meal = mealsQuery.data?.find((m) => m.mealType === mealType);
  const consumptionQuery = useQuery({ queryKey: ['adminConsumption', meal?.mealId], queryFn: () => adminApi.getConsumption(meal.mealId), enabled: Boolean(meal) });
  const wasteQuery = useQuery({ queryKey: ['wasteAnalytics', days], queryFn: () => adminApi.getWasteAnalytics(days) });

  const afterSave = () => {
    queryClient.invalidateQueries({ queryKey: ['adminConsumption'] });
    queryClient.invalidateQueries({ queryKey: ['wasteAnalytics'] });
  };
  const w = wasteQuery.data;
  const chartBox = 'border-2 border-forest bg-white p-4';

  return (
    <div className="space-y-10">
      <section aria-labelledby="record-heading" className="space-y-4">
        <h1 id="record-heading" className="text-3xl font-semibold">Post-meal <span className="text-sage">Feedback</span></h1>
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-sm font-medium">Date{' '}
            <input type="date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} className="border-2 border-forest/40 bg-white px-2 py-1" />
          </label>
          <MealTypeTabs value={mealType} onChange={setMealType} />
        </div>
        {mealsQuery.isSuccess && !meal && <EmptyState title={`No ${MEAL_LABELS[mealType]} planned on ${date}`} />}
        {consumptionQuery.isLoading && <FactLoader label="Loading meal" />}
        {consumptionQuery.isError && <ErrorState message={getErrorMessage(consumptionQuery.error)} onRetry={() => consumptionQuery.refetch()} />}
        {consumptionQuery.data && (
          <ConsumptionForm key={`${consumptionQuery.data.mealId}-${consumptionQuery.dataUpdatedAt}`}
            mealId={consumptionQuery.data.mealId} initialRows={consumptionQuery.data.records} onSaved={afterSave} />
        )}
      </section>

      <section aria-labelledby="waste-heading" className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 id="waste-heading" className="text-2xl font-semibold">Waste analytics</h2>
          <select aria-label="Analytics period" value={days} onChange={(e) => setDays(Number(e.target.value))} className="border-2 border-forest/40 bg-white px-3 py-2 text-sm">
            {[7, 14, 30].map((d) => <option key={d} value={d}>Last {d} days</option>)}
          </select>
        </div>
        {wasteQuery.isLoading && <FactLoader label="Loading analytics" />}
        {wasteQuery.isError && <ErrorState message={getErrorMessage(wasteQuery.error)} onRetry={() => wasteQuery.refetch()} />}
        {w && (
          <>
            <div className="grid grid-cols-2 gap-5 lg:grid-cols-5">
              <StatCard label="Food prepared" value={nf(w.totals.prepared)} hint="servings" tone="white" />
              <StatCard label="Food consumed" value={nf(w.totals.consumed)} hint="servings" tone="sage" />
              <StatCard label="Food leftover" value={nf(w.totals.leftover)} hint="servings" tone="tangerine" />
              <StatCard label="Waste %" value={`${w.totals.wastePercentage.toFixed(1)}%`} hint="leftover / prepared" tone="brick" />
              <StatCard label="Forecast accuracy" value={`${w.totals.forecastAccuracy.toFixed(1)}%`} hint="vs actual consumption" tone="butter" />
            </div>
            {w.dailyTrend.length === 0 ? (
              <EmptyState title="No consumption recorded in this period" message="Save post-meal actuals above to populate analytics." />
            ) : (
              <div className="grid gap-6 lg:grid-cols-2">
                <div className={chartBox}>
                  <h3 className="mb-2 font-semibold">Daily waste % trend</h3>
                  <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={w.dailyTrend}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tick={{ fontSize: 11 }} /><YAxis unit="%" /><Tooltip />
                      <Line type="monotone" dataKey="wastePercentage" name="Waste %" stroke="#B83636" strokeWidth={3} isAnimationActive={false} /></LineChart>
                  </ResponsiveContainer>
                </div>
                <div className={chartBox}>
                  <h3 className="mb-2 font-semibold">Prepared vs consumed vs leftover</h3>
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={w.dailyTrend}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tick={{ fontSize: 11 }} /><YAxis /><Tooltip /><Legend />
                      <Bar dataKey="prepared" name="Prepared" fill="#FFF3A1" stroke="#1F4B33" isAnimationActive={false} />
                      <Bar dataKey="consumed" name="Consumed" fill="#7D9B65" isAnimationActive={false} />
                      <Bar dataKey="leftover" name="Leftover" fill="#B83636" isAnimationActive={false} /></BarChart>
                  </ResponsiveContainer>
                </div>
                <div className={chartBox}>
                  <h3 className="mb-2 font-semibold">Meal-wise waste %</h3>
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={w.byMealType.map((m) => ({ ...m, label: MEAL_LABELS[m.mealType] }))}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis unit="%" /><Tooltip />
                      <Bar dataKey="wastePercentage" name="Waste %" fill="#FF9C4B" isAnimationActive={false} /></BarChart>
                  </ResponsiveContainer>
                </div>
                <div className={chartBox}>
                  <h3 className="mb-2 font-semibold">Dish-wise waste % (highest)</h3>
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={w.byDish} layout="vertical" margin={{ left: 30 }}><CartesianGrid strokeDasharray="3 3" /><XAxis type="number" unit="%" /><YAxis type="category" dataKey="dishName" width={130} tick={{ fontSize: 11 }} /><Tooltip />
                      <Bar dataKey="wastePercentage" name="Waste %" fill="#B83636" isAnimationActive={false} /></BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
