"""Record post-meal feedback locally and export observed-only retraining inputs."""
import argparse
import json
import math
import os
from datetime import date
from pathlib import Path

import pandas as pd
from pydantic import BaseModel, Field, model_validator

from api.schemas import ForecastRequest, ForecastResponse
from features.forecast_features import meal_record

class DishOutcome(BaseModel):
    dish_id: int
    unit: str = Field(min_length=1)
    actual_prepared_quantity: float = Field(ge=0, allow_inf_nan=False)
    actual_consumed_quantity: float = Field(ge=0, allow_inf_nan=False)
    actual_leftover_quantity: float = Field(ge=0, allow_inf_nan=False)
    stockout: bool

class Feedback(BaseModel):
    meal_date: date
    request: ForecastRequest
    forecast: ForecastResponse | None = None
    actual_attendance: int = Field(ge=0)
    dishes: list[DishOutcome]

    @model_validator(mode="after")
    def check_observations(self):
        if self.request.day_of_week != self.meal_date.weekday():
            raise ValueError("Request day_of_week must match meal_date")
        if self.actual_attendance > self.request.eligible_students:
            raise ValueError("Actual attendance cannot exceed eligible students")
        menu = {d.dish_id: d.unit for d in self.request.dishes}
        observed = {d.dish_id: d.unit for d in self.dishes}
        if menu != observed or len(observed) != len(self.dishes):
            raise ValueError("Report outcomes for each request dish once, using the same unit")
        if self.forecast is not None:
            forecasted = {d.dish_id: d.unit for d in self.forecast.dish_forecasts}
            if (self.forecast.meal_id != self.request.meal_id or forecasted != menu or
                    len(self.forecast.dish_forecasts) != len(menu)):
                raise ValueError("Saved forecast must match the request's meal, dishes, and units")
        for d in self.dishes:
            if not math.isclose(d.actual_prepared_quantity, d.actual_consumed_quantity + d.actual_leftover_quantity,
                                rel_tol=0, abs_tol=.011):
                raise ValueError("Prepared must equal consumed plus leftover")
        return self

def read_feedback(path):
    if not path.exists():
        return []
    return [Feedback.model_validate_json(line)
            for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]

def append_feedback(path, entry):
    if any(e.request.meal_id == entry.request.meal_id for e in read_feedback(path)):
        raise ValueError("This meal_id already has a feedback record")
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as stream:
        stream.write(entry.model_dump_json() + "\n")

def export(path, output):
    entries = read_feedback(path)
    if not entries:
        raise ValueError("No feedback to export")
    meals, dishes = [], []
    for entry in sorted(entries, key=lambda e: (e.meal_date, e.request.meal_id)):
        request = entry.request
        meals.append({"meal_id": request.meal_id, "meal_date": entry.meal_date.isoformat(),
                      **meal_record(request), "actual_attendance": entry.actual_attendance})
        outcomes = {d.dish_id: d for d in entry.dishes}
        for d in request.dishes:
            dishes.append({"meal_id": request.meal_id, **d.model_dump(), **outcomes[d.dish_id].model_dump()})
    output.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(meals).to_csv(output / "attendance.csv", index=False)
    dish_columns = ["meal_id","dish_id","unit","student_interest","historical_popularity","avg_consumption_quantity",
                    "actual_consumed_quantity","actual_prepared_quantity","actual_leftover_quantity","stockout"]
    pd.DataFrame(dishes, columns=dish_columns).to_csv(output / "dish_demand.csv", index=False)
    return len(meals), len(dishes)

def summarize(entries):
    att_errors, per_unit = [], {}
    for entry in entries:
        if entry.forecast is not None:
            att_errors.append(abs(entry.forecast.predicted_attendance - entry.actual_attendance))
        predicted = {d.dish_id: d.predicted_quantity for d in entry.forecast.dish_forecasts} if entry.forecast else {}
        for d in entry.dishes:
            group = per_unit.setdefault(d.unit, {"prepared": 0., "leftover": 0., "errors": [], "stockouts": 0})
            group["prepared"] += d.actual_prepared_quantity
            group["leftover"] += d.actual_leftover_quantity
            group["stockouts"] += d.stockout
            if not d.stockout and d.dish_id in predicted:
                group["errors"].append(abs(predicted[d.dish_id] - d.actual_consumed_quantity))
    def mae(values):
        return round(sum(values) / len(values), 4) if values else None
    def unit_stats(group):
        prepared, leftover, errors = group["prepared"], group["leftover"], group["errors"]
        return {"prepared": round(prepared, 2), "leftover": round(leftover, 2),
                "leftover_percent": round(100 * leftover / prepared, 2) if prepared else None,
                "dish_forecast_mae": mae(errors), "dish_forecasts_scored": len(errors), "stockouts": group["stockouts"]}
    return {"meals": len(entries), "attendance_forecast_mae": mae(att_errors),
            "attendance_forecasts_scored": len(att_errors), "by_unit": {u: unit_stats(g) for u,g in per_unit.items()}}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, default=Path(os.getenv("PREPLATE_FEEDBACK_LOG", "data/feedback.jsonl")))
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("add", help="Append one post-meal JSON record").add_argument("record", type=Path)
    sub.add_parser("export", help="Write training tables without synthetic data").add_argument(
        "--output", type=Path, default=Path("data/observed"))
    sub.add_parser("summary", help="Show forecast error, leftovers and stockouts")
    args = parser.parse_args()
    if args.command == "add":
        entry = Feedback.model_validate_json(args.record.read_text(encoding="utf-8"))
        append_feedback(args.log, entry)
        print(f"Saved outcomes for meal {entry.request.meal_id}.")
    elif args.command == "export":
        meals, dishes = export(args.log, args.output)
        print(f"Exported {meals} meals and {dishes} dish outcomes to {args.output}.")
    else:
        print(json.dumps(summarize(read_feedback(args.log)), indent=2))

if __name__ == "__main__":
    main()
