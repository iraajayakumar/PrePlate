import { MEAL_LABELS, MEAL_TYPES } from '../../utils/constants';

export default function MealTypeTabs({ value, onChange }) {
  return (
    <div role="tablist" aria-label="Meal type" className="inline-flex flex-wrap gap-2">
      {MEAL_TYPES.map((type) => (
        <button key={type} type="button" role="tab" aria-selected={type === value} onClick={() => onChange(type)}
          className={`px-4 py-2 text-sm font-medium transition ${type === value ? 'bg-forest text-white shadow-[3px_3px_0_0_var(--color-tangerine)]' : 'bg-cream hover:bg-butter'}`}>
          {MEAL_LABELS[type]}
        </button>
      ))}
    </div>
  );
}
