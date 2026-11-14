import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminApi } from '../../api/adminApi';
import { getErrorMessage } from '../../api/axiosClient';
import { useToast } from '../../components/common/Toast';
import { EmptyState, ErrorState, FactLoader } from '../../components/common/StateViews';
import StatCard from '../../components/admin/StatCard';
import MealTypeTabs from '../../components/admin/MealTypeTabs';
import { MEAL_LABELS } from '../../utils/constants';
import { formatClockTime, toIsoDate } from '../../utils/formatters';

const nf = (n) => (n ?? 0).toLocaleString('en-IN');

export default function AdminDashboardPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [date, setDate] = useState(toIsoDate(new Date()));
  const [mealType, setMealType] = useState('DINNER');
  const queryKey = ['adminDashboard', date, mealType];

  const query = useQuery({ queryKey, queryFn: () => adminApi.getDashboard(date, mealType), refetchInterval: 30_000 });

  const refresh = useMutation({
    mutationFn: (mealId) => adminApi.refreshForecast(mealId),
    onSuccess: (data) => { queryClient.setQueryData(queryKey, data); toast.success('Forecast refreshed with the latest responses.'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const d = query.data;
  const f = d?.forecast;
  const responseData = d && [
    { name: 'Confirmed', value: d.confirmedAttendance, color: '#7D9B65' },
    { name: 'Not attending', value: d.notAttending, color: '#B83636' },
    { name: 'Uncertain', value: d.uncertain, color: '#FF9C4B' },
    { name: 'No response', value: d.noResponse, color: '#FFF3A1' },
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold">Kitchen <span className="text-sage">Dashboard</span></h1>
          <p className="text-sm text-ink/70">Live demand, forecast and preparation recommendation per meal.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-sm font-medium">Date{' '}
            <input type="date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} className="border-2 border-forest/40 bg-white px-2 py-1" />
          </label>
          <MealTypeTabs value={mealType} onChange={setMealType} />
        </div>
      </div>

      {query.isLoading && <FactLoader label="Loading dashboard" />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} onRetry={() => query.refetch()} />}
      {d && !d.mealExists && <EmptyState title={`No ${MEAL_LABELS[mealType]} planned for ${date}`} message="Create the meal under Menu to see demand here." />}

      {d?.mealExists && (
        <>
          <div className="grid grid-cols-2 gap-5 md:grid-cols-3 xl:grid-cols-5">
            <StatCard label="Eligible students" value={nf(d.eligibleStudents)} tone="white" />
            <StatCard label="Confirmed attendance" value={nf(d.confirmedAttendance)} tone="sage" />
            <StatCard label="Not attending" value={nf(d.notAttending)} tone="brick" />
            <StatCard label="Uncertain" value={nf(d.uncertain)} tone="tangerine" />
            <StatCard label="No response" value={nf(d.noResponse)} tone="butter" />
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <section className="bg-butter p-5 shadow-[6px_6px_0_0_var(--color-tangerine)] lg:col-span-2" aria-labelledby="forecast-heading">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <h2 id="forecast-heading" className="text-xl font-semibold">Forecast</h2>
                <button type="button" onClick={() => refresh.mutate(d.mealId)} disabled={refresh.isPending}
                  className="bg-forest px-4 py-2 text-sm font-medium text-white shadow-[3px_3px_0_0_var(--color-brick)] hover:brightness-110 disabled:opacity-60">
                  {refresh.isPending ? 'Refreshing...' : 'Refresh forecast'}
                </button>
              </div>
              {f ? (
                <>
                  <div className="mt-3 grid gap-4 sm:grid-cols-2">
                    <div className="bg-white/80 p-4">
                      <p className="text-xs font-medium uppercase">Predicted attendance</p>
                      <p className="text-4xl font-semibold tabular-nums">{nf(f.predictedAttendance)}</p>
                      <p className="text-sm">Likely range {nf(f.lowerBound)} - {nf(f.upperBound)}</p>
                    </div>
                    <div className="bg-white/80 p-4">
                      <p className="text-xs font-medium uppercase">Recommended preparation</p>
                      <p className="text-4xl font-semibold tabular-nums text-brick">{nf(f.recommendedPreparation)}</p>
                      <p className="text-sm">servings (includes safety buffer)</p>
                    </div>
                  </div>
                  <div className="mt-4">
                    <div className="flex justify-between text-xs font-medium"><span>Confidence</span><span>{Math.round(f.confidence * 100)}%</span></div>
                    <div className="h-3 bg-white"><div className="h-3 bg-sage" style={{ width: `${f.confidence * 100}%` }} /></div>
                  </div>
                  <p className="mt-3 text-xs text-ink/70">
                    Source: <strong>{f.source === 'ML_SERVICE' ? 'ML forecasting service' : 'Historical-average fallback'}</strong> · generated at {formatClockTime(f.generatedAt)}
                  </p>
                </>
              ) : <p className="mt-3 text-sm">No forecast available yet.</p>}
            </section>

            <section className="border-2 border-forest bg-white p-4" aria-label="Response breakdown">
              <h2 className="mb-1 text-lg font-semibold">Student responses</h2>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={responseData} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} stroke="#1F4B33" isAnimationActive={false}>
                    {responseData.map((r) => <Cell key={r.name} fill={r.color} />)}
                  </Pie>
                  <Tooltip /><Legend />
                </PieChart>
              </ResponsiveContainer>
            </section>
          </div>

          {f?.factors?.length > 0 && (
            <section className="border-2 border-forest bg-white p-4" aria-label="Why this forecast">
              <h2 className="text-lg font-semibold">What drives this forecast</h2>
              <p className="mb-2 text-xs text-ink/60">Relative importance of each factor (explainable forecast).</p>
              <ResponsiveContainer width="100%" height={40 + f.factors.length * 36}>
                <BarChart data={f.factors.map((x) => ({ ...x, pct: Math.round(x.importance * 100) }))} layout="vertical" margin={{ left: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" unit="%" domain={[0, 100]} />
                  <YAxis type="category" dataKey="name" width={200} tick={{ fontSize: 12 }} />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Bar dataKey="pct" fill="#7D9B65" isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </section>
          )}

          <section aria-label="Dish-level demand">
            <h2 className="mb-3 text-xl font-semibold">Dish-level demand</h2>
            <div className="overflow-x-auto border-2 border-forest bg-white">
              <table className="w-full min-w-[560px] text-left text-sm">
                <thead className="bg-forest text-white">
                  <tr><th className="p-3">Dish</th><th className="p-3 text-right">Student interest</th><th className="p-3 text-right">Predicted demand</th><th className="p-3 text-right">Recommended preparation</th></tr>
                </thead>
                <tbody>
                  {d.dishes.map((row) => (
                    <tr key={row.dishId} className="border-t border-forest/20 even:bg-cream/60">
                      <td className="p-3 font-medium">{row.dishName}</td>
                      <td className="p-3 text-right tabular-nums">{nf(row.studentInterest)}</td>
                      <td className="p-3 text-right tabular-nums">{nf(row.predictedDemand)}</td>
                      <td className="p-3 text-right font-semibold tabular-nums text-brick">{nf(row.recommendedPreparation)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-2 text-xs text-ink/60">Interest and predicted demand are separate on purpose: a student not picking a dish does not mean they will not eat it.</p>
            <div className="mt-5 border-2 border-forest bg-white p-4">
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={d.dishes}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="dishName" tick={{ fontSize: 11 }} interval={0} angle={-20} textAnchor="end" height={60} />
                  <YAxis /><Tooltip /><Legend verticalAlign="top" />
                  <Bar dataKey="studentInterest" name="Student interest" fill="#FFF3A1" stroke="#1F4B33" isAnimationActive={false} />
                  <Bar dataKey="predictedDemand" name="Predicted demand" fill="#FF9C4B" isAnimationActive={false} />
                  <Bar dataKey="recommendedPreparation" name="Recommended" fill="#7D9B65" isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
