import { ASSET_PATHS, MEAL_LABELS, MEAL_TYPES } from '../../utils/constants';

export default function MealTabs({ selected, onSelect, meals = [] }) {
  const byType = Object.fromEntries(meals.map((m) => [m.mealType, m]));
  return (
    <div role="tablist" aria-label="Choose a meal" className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      {MEAL_TYPES.map((type) => {
        const isSelected = type === selected;
        const status = byType[type]?.response?.status;
        return (
          <button key={type} type="button" role="tab" aria-selected={isSelected} onClick={() => onSelect(type)}
            className={`relative h-16 overflow-hidden transition sm:h-20 ${isSelected ? 'shadow-[5px_5px_0_0_var(--color-tangerine)]' : 'shadow-[5px_5px_0_0_#000] hover:shadow-[5px_5px_0_0_var(--color-tangerine)]'}`}>
            <img src={ASSET_PATHS.mealImages[type]} alt="" className={`h-full w-full object-cover transition ${isSelected ? '' : 'grayscale hover:grayscale-0'}`} />
            <span className="text-outline-shadow absolute inset-0 flex items-center justify-center text-lg font-bold uppercase tracking-wide text-white sm:text-xl">
              {MEAL_LABELS[type]}
            </span>
            {status === 'ATTENDING' && (
              <span className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-sage text-xs font-bold text-white" aria-label="attendance confirmed">✓</span>
            )}
          </button>
        );
      })}
    </div>
  );
}
