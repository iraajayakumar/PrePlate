import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { studentApi } from '../../api/studentApi';
import { getErrorMessage } from '../../api/axiosClient';
import ShadowCard from '../../components/common/ShadowCard';
import { EmptyState, ErrorState, FactLoader } from '../../components/common/StateViews';
import ParticipationDonut from '../../components/student/ParticipationDonut';
import { ANALYTICS_SCOPES, HISTORY_PERIODS, MEAL_LABELS, RESPONSE_LABELS } from '../../utils/constants';
import { formatMealDate } from '../../utils/formatters';

const STATUS_STYLES = {
  ATTENDING: 'bg-sage text-white',
  NOT_ATTENDING: 'bg-brick text-white',
  NOT_SURE: 'bg-butter text-forest border border-forest',
};

function SegmentedControl({ options, value, onChange, label }) {
  return (
    <div role="tablist" aria-label={label} className="inline-flex flex-wrap gap-2">
      {options.map((o) => (
        <button key={o.value} type="button" role="tab" aria-selected={o.value === value} onClick={() => onChange(o.value)}
          className={`px-4 py-2 text-sm font-medium transition ${o.value === value ? 'bg-forest text-white shadow-[3px_3px_0_0_var(--color-tangerine)]' : 'bg-cream hover:bg-butter'}`}>
          {o.label}
        </button>
      ))}
    </div>
  );
}

export default function FoodHistoryPage() {
  const [period, setPeriod] = useState('TODAY');
  const [scope, setScope] = useState('THIS_MONTH');

  const historyQuery = useQuery({ queryKey: ['studentHistory', period], queryFn: () => studentApi.getHistory(period) });
  const analyticsQuery = useQuery({ queryKey: ['studentAnalytics', scope], queryFn: () => studentApi.getAnalytics(scope) });

  return (
    <div className="space-y-10">
      <section aria-labelledby="selection-history">
        <h1 id="selection-history" className="mb-3 text-3xl font-semibold">Food History</h1>
        <SegmentedControl options={HISTORY_PERIODS} value={period} onChange={setPeriod} label="History period" />
        <div className="mt-5">
          {historyQuery.isLoading && <FactLoader label="Loading your history" />}
          {historyQuery.isError && <ErrorState message={getErrorMessage(historyQuery.error)} onRetry={() => historyQuery.refetch()} />}
          {historyQuery.isSuccess && (
            <div className="grid gap-6 md:grid-cols-2">
              {historyQuery.data.categories.map((category) => (
                <ShadowCard key={category.mealType} tone="cream" className="p-4">
                  <h2 className="mb-2 text-xl font-semibold text-forest">{MEAL_LABELS[category.mealType]}</h2>
                  {category.entries.length === 0 ? (
                    <EmptyState title="Nothing here yet" message="No selections in this period." />
                  ) : (
                    <ul className="space-y-3">
                      {category.entries.map((entry) => (
                        <li key={entry.mealId} className="bg-white/80 p-3">
                          <div className="flex items-center justify-between gap-2">
                            <span className="text-sm font-medium">{formatMealDate(entry.mealDate)}</span>
                            <span className={`px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[entry.status]}`}>{RESPONSE_LABELS[entry.status]}</span>
                          </div>
                          <p className="mt-1 text-sm text-ink/80">
                            {entry.status === 'ATTENDING'
                              ? entry.dishNames.length > 0 ? entry.dishNames.join(', ') : 'No specific dishes picked (most items)'
                              : '-'}
                          </p>
                        </li>
                      ))}
                    </ul>
                  )}
                </ShadowCard>
              ))}
            </div>
          )}
        </div>
      </section>

      <section aria-labelledby="participation">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 id="participation" className="text-2xl font-semibold">Meal participation</h2>
          <SegmentedControl options={ANALYTICS_SCOPES} value={scope} onChange={setScope} label="Analytics period" />
        </div>
        {analyticsQuery.isLoading && <FactLoader label="Loading analytics" />}
        {analyticsQuery.isError && <ErrorState message={getErrorMessage(analyticsQuery.error)} onRetry={() => analyticsQuery.refetch()} />}
        {analyticsQuery.isSuccess && (
          <>
            <div className="grid grid-cols-2 gap-5 lg:grid-cols-4">
              {analyticsQuery.data.participation.map((p) => <ParticipationDonut key={p.mealType} {...p} />)}
            </div>
            <p className="mt-3 text-xs text-ink/60">
              "Selected" means you told PrePlate you would attend. "Attended" counts only meals where attendance was recorded, because a selection does not confirm that you actually ate.
            </p>
            <h3 className="mb-2 mt-8 text-xl font-semibold">Your most selected dishes</h3>
            {analyticsQuery.data.topDishes.length === 0 ? (
              <EmptyState title="No dish picks yet" message="Add dishes to your plate to see your favourites here." />
            ) : (
              <ol className="max-w-xl space-y-2">
                {analyticsQuery.data.topDishes.map((dish, i) => {
                  const max = analyticsQuery.data.topDishes[0].timesSelected || 1;
                  return (
                    <li key={dish.dishName} className="flex items-center gap-3 text-sm">
                      <span className="w-5 text-right font-semibold text-brick">{i + 1}</span>
                      <span className="w-40 truncate">{dish.dishName}</span>
                      <span className="h-3 flex-1 bg-cream"><span className="block h-3 bg-sage" style={{ width: `${(dish.timesSelected / max) * 100}%` }} /></span>
                      <span className="w-8 text-right tabular-nums">{dish.timesSelected}</span>
                    </li>
                  );
                })}
              </ol>
            )}
          </>
        )}
      </section>
    </div>
  );
}
