"""Compare models on future meals and export a serving bundle plus evaluation."""
import argparse
import hashlib
import json
import math
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from xgboost import XGBRegressor

from features.forecast_features import ATTENDANCE, DEMAND, attendance_frame, demand_frame, summarize_menu
from models.bundle import DEFAULT_BUNDLE, FORMAT_VERSION, MODEL_VERSION, model_dependencies
from services.baseline import attendance_baseline, demand_baseline

COVERAGE = .9
BASELINE = "statistical_fallback"
HISTORY = "historical_average"

def models():
    return {
        "linear_regression": LinearRegression(),
        "random_forest": RandomForestRegressor(n_estimators=150, min_samples_leaf=7, random_state=42, n_jobs=2),
        "xgboost": XGBRegressor(n_estimators=180, max_depth=4, learning_rate=.045, subsample=.9,
                                colsample_bytree=.9, n_jobs=2, random_state=42, tree_method="hist"),
    }

def metrics(y, pred, mask=None):
    if mask is not None:
        y, pred = y[mask], pred[mask]
    if not len(y):
        return None
    error = pred - y
    nonzero = y != 0
    return {
        "n": len(y), "mae": round(float(np.abs(error).mean()), 4),
        "rmse": round(float(np.sqrt(np.mean(error**2))), 4),
        "mape_percent": round(float(100 * np.mean(np.abs(error[nonzero] / y[nonzero]))), 4)
        if nonzero.any() else None,
        "mape_n": int(nonzero.sum()),
    }

def split_dates(meals):
    dates = sorted(meals["meal_date"].unique())
    if len(dates) < 40:
        raise ValueError("At least 40 unique dates are required for four chronological splits")
    a, b, c = (int(len(dates) * p) for p in (.60, .75, .85))
    return dict(zip(("train", "validation", "calibration", "test"),
                    (dates[:a], dates[a:b], dates[b:c], dates[c:])))

def conformal_radius(scores):
    scores = np.sort(scores)
    k = min(len(scores), math.ceil((len(scores) + 1) * COVERAGE))
    return float(scores[k - 1])

def as_quantity(raw, scale, eligible=None):
    result = np.maximum(0, raw) * scale
    return np.clip(np.rint(result), 0, eligible) if eligible is not None else np.round(result, 2)

def split_metadata(splits):
    return {k: {"start": v[0], "end": v[-1], "days": len(v)} for k, v in splits.items()}

def train_task(df, splits, task):
    attendance = task == "attendance"
    target = "actual_attendance" if attendance else "actual_consumed_quantity"
    fallback = attendance_baseline if attendance else demand_baseline
    frame = attendance_frame if attendance else demand_frame
    sets = {}
    for key, dates in splits.items():
        part = df.loc[df.meal_date.isin(dates)].reset_index(drop=True)
        eligible = part.eligible_students.to_numpy(dtype=float)
        scale = eligible if attendance else part.avg_consumption_quantity.to_numpy(dtype=float)
        keep = scale > 0
        if not keep.any():
            raise ValueError(f"{task} split {key} has no rows with a usable training scale")
        sets[key] = (part, frame(part.to_dict("records")), part[target].to_numpy(dtype=float),
                     scale, keep, eligible if attendance else None,
                     np.array([fallback(row) for row in part.to_dict("records")]))
    _, x, y, scale, keep, _, _ = sets["train"]
    estimators = models()
    for estimator in estimators.values():
        estimator.fit(x.loc[keep], y[keep] / scale[keep])

    def predictions(key, name):
        part, x, y, scale, keep, eligible, baseline = sets[key]
        if name == BASELINE:
            return baseline
        if name == HISTORY:
            if attendance:
                return np.clip(np.rint(part.avg_attendance.to_numpy()), 0, eligible)
            return np.round(scale, 2)
        result = baseline.copy().astype(float)
        limits = eligible[keep] if eligible is not None else None
        result[keep] = as_quantity(estimators[name].predict(x.loc[keep]), scale[keep], limits)
        return result

    def comparison(key, name):
        part, _, y, scale, keep, _, _ = sets[key]
        pred = predictions(key, name)
        if attendance:
            return metrics(y, pred)
        # Report absolute errors per unit. Rank candidates by dimensionless MAE.
        return {"normalized": metrics(y[keep] / scale[keep], pred[keep] / scale[keep]),
                "by_unit": {str(u): metrics(y, pred, part.unit.to_numpy() == u)
                            for u in sorted(part.unit.unique())},
                "cold_start_rows": int((~keep).sum())}

    validation = {name: comparison("validation", name) for name in (HISTORY, BASELINE, *estimators)}
    def score(name):
        entry = validation[name]
        return entry["mae"] if attendance else entry["normalized"]["mae"]
    # The plain historical average is a reference, not a serving policy.
    winner = min((BASELINE, *estimators), key=score)
    test = {name: comparison("test", name) for name in validation}

    # Calibrate residuals on a separate period after validation/model selection.
    _, _, y, scale, keep, _, _ = sets["calibration"]
    radius = conformal_radius(np.abs(y[keep] - predictions("calibration", winner)[keep]) / scale[keep])
    part, x, y, scale, keep, _, _ = sets["test"]
    selected = predictions("test", winner)
    coverage = float(np.mean(np.abs(y[keep] - selected[keep]) <= radius * scale[keep]))
    # Global diagnostic only. It is not an explanation of an individual meal.
    # Shuffle grouped indicator families together to keep one-hot rows valid.
    rng = np.random.default_rng(42)
    estimator = estimators.get(winner)
    importance = {}
    if estimator is not None:
        x = x.loc[keep].reset_index(drop=True)
        base = np.mean(np.abs(y[keep] - selected[keep]) / scale[keep])
        families = {}
        for col in x:
            family = "meal_type" if col.startswith("meal_") else "day_of_week" if col.startswith("day_") else col
            families.setdefault(family, []).append(col)
        for family, cols in families.items():
            shuffled = x.copy()
            shuffled[cols] = x[cols].to_numpy()[rng.permutation(len(x))]
            cap = part.eligible_students.to_numpy()[keep] if attendance else None
            pred = as_quantity(estimator.predict(shuffled), scale[keep], cap)
            importance[family] = round(float(np.mean(np.abs(y[keep] - pred) / scale[keep]) - base), 6)
    report = {
        "selected": winner, "selection_metric": "MAE (students)" if attendance else "MAE (consumed / historical average)",
        "validation": validation, "test": test,
        "interval": {"target_coverage": COVERAGE, "observed_test_coverage": round(coverage, 4),
                     "n_test": int(keep.sum()), "normalized_radius": radius},
        "global_permutation_importance": dict(sorted(importance.items(), key=lambda item: -item[1])),
    }
    return {"name": winner, "estimator": estimator, "radius": radius}, report

def load_training_data(data_dir):
    meals = pd.read_csv(data_dir / "attendance.csv", dtype={"meal_date": str})
    dishes = pd.read_csv(data_dir / "dish_demand.csv", dtype={"meal_date": str})
    if meals.empty or dishes.empty or meals.meal_id.duplicated().any() or dishes.duplicated(["meal_id","dish_id"]).any():
        raise ValueError("Training requires non-empty tables with unique meal and meal/dish identifiers")
    dates = pd.to_datetime(meals.meal_date, format="%Y-%m-%d")
    counts = meals[["yes","no","unsure","no_response"]]
    if (not dates.dt.dayofweek.eq(meals.day_of_week).all() or
        not meals.meal_type.isin(["breakfast","lunch","dinner","snacks"]).all() or
        not np.isfinite(meals.select_dtypes("number")).all().all() or
        not counts.ge(0).all().all() or not counts.sum(axis=1).eq(meals.eligible_students).all() or
        not meals.eligible_students.gt(0).all() or not meals.actual_attendance.ge(0).all() or
        not meals.actual_attendance.le(meals.eligible_students).all() or
        not meals.avg_attendance.ge(0).all() or not meals.avg_waste_percentage.between(0,100).all()):
        raise ValueError("Invalid meal features, response totals, dates, or attendance")
    dish_fields = ["meal_id","dish_id","unit","student_interest","historical_popularity",
                   "avg_consumption_quantity","actual_consumed_quantity","actual_prepared_quantity",
                   "actual_leftover_quantity","stockout"]
    dishes = dishes[dish_fields].merge(meals[["meal_id","eligible_students"]], on="meal_id", how="left",
                                      validate="many_to_one")
    stockout = dishes.stockout.astype("string").str.lower().map({"true": True, "false": False, "1": True, "0": False})
    values = dishes[["actual_consumed_quantity","actual_prepared_quantity","actual_leftover_quantity"]]
    if (not np.isfinite(dishes.select_dtypes("number")).all().all() or
        not dishes.unit.fillna("").str.strip().ne("").all() or
        not dishes.student_interest.ge(0).all() or not dishes.student_interest.le(dishes.eligible_students).all() or
        not dishes.historical_popularity.between(0,1).all() or not dishes.avg_consumption_quantity.ge(0).all() or
        not values.ge(0).all().all() or not np.allclose(values.iloc[:,0] + values.iloc[:,2], values.iloc[:,1], rtol=0, atol=.011) or
        stockout.isna().any()):
        raise ValueError("Invalid dish features or quantities. Prepared must equal consumed plus leftover.")
    dishes["stockout"] = stockout.astype(bool)
    summaries = {k: summarize_menu(int(g.eligible_students.iloc[0]), g.to_dict("records"))
                 for k,g in dishes.groupby("meal_id", sort=False)}
    menu_cols = ["dish_count","menu_interest_rate","menu_popularity"]
    meals = meals.drop(columns=menu_cols, errors="ignore")
    for col in menu_cols:
        meals[col] = meals.meal_id.map(lambda k: summaries.get(k, {}).get(col, 0))
    dishes = dishes.drop(columns="eligible_students").merge(meals, on="meal_id", validate="many_to_one")
    return meals.sort_values(["meal_date","meal_id"]), dishes.sort_values(["meal_date","meal_id","dish_id"])

def save_joblib(payload, path):
    temp = path.with_suffix(".joblib.tmp")
    try:
        joblib.dump(payload, temp, compress=3)
        temp.replace(path)
    finally:
        temp.unlink(missing_ok=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data", type=Path, default=Path("data"))
    parser.add_argument("--models", type=Path, default=Path("models"))
    parser.add_argument("--reports", type=Path, default=Path("reports"))
    parser.add_argument("--source", choices=["synthetic","observed"], default="synthetic")
    args = parser.parse_args()
    meals, dishes = load_training_data(args.data)
    splits = split_dates(meals)
    attendance, a_report = train_task(meals, splits, "attendance")
    # Stockouts censor true demand; do not train, calibrate, or score on them.
    eligible_dishes = dishes.loc[~dishes.stockout]
    demand, d_report = train_task(eligible_dishes, splits, "dish_demand")
    dependencies = model_dependencies()
    input_hashes = {name: hashlib.sha256((args.data / name).read_bytes()).hexdigest()
                    for name in ("attendance.csv","dish_demand.csv")}
    # Attach a data fingerprint so different training snapshots are identifiable.
    snapshot = hashlib.sha256("".join(input_hashes.values()).encode()).hexdigest()[:10]
    version = f"{MODEL_VERSION}-{snapshot}"
    test_dishes = dishes.loc[dishes.meal_date.isin(splits["test"])]
    waste_by_unit = {}
    for unit, group in test_dishes.groupby("unit"):
        made, left = group[["actual_prepared_quantity","actual_leftover_quantity"]].sum()
        waste_by_unit[unit] = {"prepared": round(float(made),2), "leftover": round(float(left),2),
                               "leftover_percent": round(float(100 * left / made),2) if made else None}
    report = {
        "version": version, "data_source": args.source, "input_sha256": input_hashes,
        "dependencies": dependencies, "splits": split_metadata(splits),
        "rows": {"meals": len(meals), "dishes": len(dishes), "excluded_stockouts": int(dishes.stockout.sum())},
        "attendance": a_report, "dish_demand": d_report, "observed_test_leftovers_by_unit": waste_by_unit,
    }
    args.models.mkdir(parents=True, exist_ok=True)
    args.reports.mkdir(parents=True, exist_ok=True)
    save_joblib({"format": FORMAT_VERSION, "version": version, "dependencies": dependencies,
                 "features": {"attendance": ATTENDANCE, "dish_demand": DEMAND},
                 "data_source": args.source, "attendance": attendance, "dish_demand": demand},
                args.models / DEFAULT_BUNDLE)
    (args.reports / "evaluation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Attendance: {a_report['selected']} | Dish demand: {d_report['selected']} | {version}")
    print(f"Saved {args.models / DEFAULT_BUNDLE} and {args.reports / 'evaluation.json'}")

if __name__ == "__main__":
    main()
