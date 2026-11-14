import ShadowCard from '../common/ShadowCard';
import DishImage from './DishImage';

/** One dish row. `canAdd` is true only after the student confirmed attendance for the meal. */
export default function DishCard({ dish, canAdd, inPlate, onToggle, disabled }) {
  const infos = [
    `${dish.calories} kcal`,
    `${dish.proteinG} g protein`,
    `${dish.carbsG} g carbs`,
    `${dish.fatG} g fat`,
    dish.vegetarian ? 'Veg' : 'Non-veg',
  ];

  return (
    <ShadowCard tone="cream" className="flex items-center gap-4 p-3 sm:p-4">
      <DishImage imageFile={dish.imageFile} name={dish.name} />
      <div className="min-w-0 flex-1">
        <h3 className="truncate text-lg font-medium sm:text-xl">{dish.name}</h3>
        <ul className="mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-xs sm:text-sm">
          {infos.map((info) => <li key={info}>{info}</li>)}
        </ul>
        {dish.ingredients?.length > 0 && (
          <p className="mt-1 truncate text-xs text-ink/60" title={dish.ingredients.join(', ')}>{dish.ingredients.join(', ')}</p>
        )}
      </div>
      {canAdd && (
        <button type="button" onClick={() => onToggle(dish.id)} disabled={disabled} aria-pressed={inPlate}
          className={`shrink-0 px-3 py-2 text-sm font-medium shadow-[3px_3px_0_0_#000] transition disabled:opacity-50 ${inPlate ? 'bg-sage text-white' : 'bg-white hover:bg-butter'}`}>
          {inPlate ? 'Added ✓' : 'Add to plate'}
        </button>
      )}
    </ShadowCard>
  );
}
