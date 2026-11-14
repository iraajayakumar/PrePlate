import ShadowCard from '../common/ShadowCard';
import { ASSET_PATHS, MEAL_LABELS, RESPONSE_STATUS } from '../../utils/constants';
import { formatCountdown, round1, sumNutrition } from '../../utils/formatters';

/**
 * "My Plate" panel. Purely presentational: all data and handlers come from props.
 * States: no response / not sure -> confirm; attending -> plate; not attending -> change.
 */
export default function MyPlate({ meal, msToDeadline, plateDishes, onRespond, onRemoveDish, busy }) {
  const mealLabel = MEAL_LABELS[meal.mealType];
  const status = meal.response?.status ?? null;
  const countdown = formatCountdown(msToDeadline);
  const totals = sumNutrition(plateDishes);
  const confirmButton = 'w-full bg-forest px-4 py-3 text-lg font-medium text-white shadow-[4px_4px_0_0_var(--color-brick)] transition hover:brightness-110 disabled:opacity-60';

  return (
    <ShadowCard tone="butter" className="p-5 text-center">
      <h2 className="flex items-center justify-center gap-2 text-3xl font-semibold">
        My <span className="text-sage">Plate</span>
        <img src={ASSET_PATHS.icons.plate} alt="" className="h-8 w-8" />
      </h2>

      {(status === null || status === RESPONSE_STATUS.NOT_SURE) && (
        <div className="mt-3 space-y-3">
          <p className="font-semibold">{status === RESPONSE_STATUS.NOT_SURE ? 'Marked as "not sure" for now' : 'Waiting for your response !'}</p>
          <p className="text-sm">Confirm your attendance to start adding dishes for {mealLabel.toUpperCase()}</p>
          <p className="text-5xl font-medium tabular-nums" aria-label="Time left to respond">{countdown}</p>
          <button type="button" disabled={busy} onClick={() => onRespond(RESPONSE_STATUS.ATTENDING)} className={confirmButton}>
            Confirm attendance
          </button>
          <div className="flex justify-center gap-4 text-sm">
            <button type="button" disabled={busy} onClick={() => onRespond(RESPONSE_STATUS.NOT_ATTENDING)} className="underline hover:text-brick">I won't be there</button>
            {status !== RESPONSE_STATUS.NOT_SURE && (
              <button type="button" disabled={busy} onClick={() => onRespond(RESPONSE_STATUS.NOT_SURE)} className="underline hover:text-brick">Not sure yet</button>
            )}
          </div>
          <p className="text-sm font-medium">Your response helps the kitchen prepare the right quantity and reduce food waste.</p>
        </div>
      )}

      {status === RESPONSE_STATUS.ATTENDING && (
        <div className="mt-3 space-y-3 text-left">
          <p className="text-center text-sm font-semibold text-forest">✓ Attendance confirmed for {mealLabel}</p>
          {plateDishes.length === 0 ? (
            <div className="text-center">
              <p className="font-semibold">Your plate is empty !</p>
              <p className="text-xs">Add dishes of your choice to fill the plate.</p>
            </div>
          ) : (
            <>
              <ul className="space-y-1.5">
                {plateDishes.map((dish) => (
                  <li key={dish.id} className="flex items-center justify-between gap-2 bg-white/70 px-3 py-1.5 text-sm">
                    <span className="truncate">{dish.name}</span>
                    <span className="flex items-center gap-2">
                      <span className="text-xs text-ink/60">{dish.calories} kcal</span>
                      <button type="button" aria-label={`Remove ${dish.name}`} onClick={() => onRemoveDish(dish.id)} className="font-bold text-brick hover:scale-110">×</button>
                    </span>
                  </li>
                ))}
              </ul>
              <div className="bg-white/70 p-3 text-center text-sm" aria-label="Estimated nutrition of your plate">
                <p className="font-semibold">Your selected {mealLabel.toLowerCase()} (estimate)</p>
                <p>{Math.round(totals.calories)} kcal · P {round1(totals.proteinG)} g · C {round1(totals.carbsG)} g · F {round1(totals.fatG)} g</p>
              </div>
            </>
          )}
          <p className="text-center text-xs">Time left to edit: <strong className="tabular-nums">{countdown}</strong></p>
          <div className="text-center">
            <button type="button" disabled={busy} onClick={() => onRespond(RESPONSE_STATUS.NOT_ATTENDING)} className="text-sm underline hover:text-brick">I can't make it after all</button>
          </div>
        </div>
      )}

      {status === RESPONSE_STATUS.NOT_ATTENDING && (
        <div className="mt-3 space-y-3">
          <p className="font-semibold">You won't be having {mealLabel}</p>
          <p className="text-sm">Thanks for letting the kitchen know. You can change this until the deadline.</p>
          <p className="text-4xl font-medium tabular-nums">{countdown}</p>
          <button type="button" disabled={busy} onClick={() => onRespond(RESPONSE_STATUS.ATTENDING)} className={confirmButton}>
            Changed my mind - confirm attendance
          </button>
        </div>
      )}
    </ShadowCard>
  );
}
