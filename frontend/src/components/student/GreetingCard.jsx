import ShadowCard from '../common/ShadowCard';
import { MEAL_TYPES, MEAL_LABELS } from '../../utils/constants';
import { formatDateTimeLong } from '../../utils/formatters';

export default function GreetingCard({ firstName, now, schedule = [] }) {
  const byType = Object.fromEntries(schedule.map((s) => [s.mealType, s]));
  return (
    <ShadowCard tone="peach" className="p-5 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <h1 className="text-3xl font-semibold sm:text-4xl">
          Hi <span className="text-brick">{firstName}</span> !
        </h1>
        <time className="text-sm font-medium text-brick" dateTime={now.toISOString()}>{formatDateTimeLong(now)}</time>
      </div>
      <p className="-mt-1 text-lg font-medium">What would you like to eat today?</p>
      <hr className="my-3 border-brick" />
      <p className="font-semibold">Confirm your plate before the deadline !</p>
      <dl className="mt-1 flex flex-wrap gap-x-6 gap-y-1 text-xs sm:text-sm">
        {MEAL_TYPES.map((type) => (
          <div key={type} className="flex gap-1">
            <dt className="font-medium">{MEAL_LABELS[type]} :</dt>
            <dd className="text-brick">{byType[type]?.deadlineLabel ?? '--'}</dd>
          </div>
        ))}
      </dl>
    </ShadowCard>
  );
}
