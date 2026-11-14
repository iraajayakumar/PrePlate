"""Generate repeatable demo data. No database, accounts, or personal data needed."""
import argparse
from collections import defaultdict, deque
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

from features.forecast_features import MEALS, summarize_menu

# ID, display name, average portion in kg, base take rate.
MENU = {
    "breakfast": [(1,"Idli",.13,.60),(2,"Upma",.16,.43),(3,"Dosa",.17,.68),(4,"Pongal",.16,.58),(5,"Chutney",.034,.68)],
    "lunch": [(11,"Rice",.19,.83),(12,"Dal",.083,.68),(13,"Chana masala",.104,.63),(14,"Sambar",.095,.72),(15,"Mixed veg",.083,.59),(16,"Curd",.075,.50)],
    "dinner": [(21,"Chapati",.12,.78),(22,"Paneer curry",.105,.71),(23,"Vegetable kurma",.104,.62),(24,"Jeera rice",.17,.65),(25,"Rajma",.094,.65),(26,"Salad",.046,.44)],
    "snacks": [(31,"Sundal",.07,.52),(32,"Samosa",.09,.70),(33,"Poha",.12,.51),(34,"Fruit",.10,.65),(35,"Sandwich",.12,.65)],
}
MEAL_RATE = dict(zip(MEALS, (.65, .77, .72, .49)))
HISTORY_WINDOW = 8
WARMUP_DAYS = 42

def generate(days=420, seed=42, start=date(2025, 1, 1)):
    if days < 60:
        raise ValueError("Generate at least 60 recorded days")
    rng = np.random.default_rng(seed)
    attendance_history = defaultdict(lambda: deque(maxlen=HISTORY_WINDOW))
    waste_history = defaultdict(lambda: deque(maxlen=HISTORY_WINDOW))
    dish_history = defaultdict(lambda: deque(maxlen=HISTORY_WINDOW))
    meals, dish_rows = [], []
    for i in range(-WARMUP_DAYS, days):
        day = start + timedelta(days=i)
        weekend = day.weekday() >= 5
        campus_signal = rng.normal(0, .042) + .034 * np.sin(2 * np.pi * i / 49)
        eligible = max(1200, int(rng.normal(4750 - 230 * weekend, 185)))
        for m, meal in enumerate(MEALS):
            count = rng.integers(3, len(MENU[meal]) + 1)
            menu = [MENU[meal][j] for j in sorted(rng.choice(len(MENU[meal]), count, replace=False))]
            menu_appeal = np.mean([d[3] for d in menu]) - .6
            appetite = MEAL_RATE[meal] - .085 * weekend + campus_signal + .30 * menu_appeal
            response_rate = np.clip(rng.normal(.80 - .07 * weekend, .085), .48, .98)
            yes_intent = np.clip(appetite + rng.normal(0, .035), .20, .89)
            probabilities = [response_rate * yes_intent, response_rate * (.90 - yes_intent),
                             response_rate * .10, 1 - response_rate]
            yes, no, unsure, silent = rng.multinomial(eligible, probabilities)
            prop = np.clip([
                .94 + .21 * campus_signal,
                .105 + .46 * campus_signal,
                .49 + .52 * campus_signal,
                appetite - .11,
            ], .02, .99)
            actual = sum(rng.binomial([yes, no, unsure, silent], prop))
            meal_key = (meal, weekend)
            history_att = attendance_history[meal_key]
            history_waste = waste_history[meal_key]
            record = {
                "meal_id": (i + WARMUP_DAYS) * len(MEALS) + m + 1,
                "meal_date": day.isoformat(), "meal_type": meal, "day_of_week": day.weekday(),
                "eligible_students": eligible, "yes": yes, "no": no,
                "unsure": unsure, "no_response": silent,
                "avg_attendance": round(float(np.mean(history_att)), 4) if history_att else 0.0,
                "avg_waste_percentage": round(float(np.mean(history_waste)), 4) if history_waste else 0.0,
                "actual_attendance": actual,
            }
            dishes = []
            for dish_id, name, portion, appeal in menu:
                # Interest is a response signal, not a count of consumers.
                appeal_change = float(rng.normal(0, .085))
                voters = rng.binomial(yes + unsure, np.clip(.80 * appeal + appeal_change, .02, .98))
                take_rate = np.clip(appeal + .63 * appeal_change + rng.normal(0,.023), .02, .98)
                takers = rng.binomial(actual, take_rate)
                used = round(takers * portion * np.clip(rng.normal(1,.036), .75, 1.25), 2)
                prepared = round(used * (1 + np.clip(rng.normal(.065,.034), .004,.21)), 2)
                leftover = round(prepared - used, 2)
                prior = dish_history[dish_id]
                dish = {
                    "dish_id": dish_id, "dish_name": name, "unit": "kg",
                    "student_interest": voters,
                    "historical_popularity": round(float(np.mean([p[1] for p in prior])), 6) if prior else 0.0,
                    "avg_consumption_quantity": round(float(np.mean([p[0] for p in prior])), 4) if prior else 0.0,
                    "actual_consumed_quantity": used, "actual_prepared_quantity": prepared,
                    "actual_leftover_quantity": leftover, "stockout": False,
                }
                dishes.append(dish)
                prior.append((used, takers / actual if actual else 0.0))
            record.update(summarize_menu(eligible, dishes))
            total_prepared = sum(d["actual_prepared_quantity"] for d in dishes)
            total_left = sum(d["actual_leftover_quantity"] for d in dishes)
            history_waste.append(100 * total_left / total_prepared if total_prepared else 0.0)
            history_att.append(actual)
            if i >= 0:
                meals.append(record)
                dish_rows.extend({**record, **dish} for dish in dishes)
    return pd.DataFrame(meals), pd.DataFrame(dish_rows)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--days", type=int, default=420)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--start", type=date.fromisoformat, default=date(2025,1,1))
    parser.add_argument("--output", type=Path, default=Path("data"))
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    meals, dishes = generate(args.days, args.seed, args.start)
    meals.to_csv(args.output / "attendance.csv", index=False)
    dishes.to_csv(args.output / "dish_demand.csv", index=False)
    print(f"Wrote {len(meals):,} meals and {len(dishes):,} dish rows to {args.output} (synthetic, seed {args.seed}).")

if __name__ == "__main__":
    main()
