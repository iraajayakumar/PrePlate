"""Forecast inference and preparation policy. No database connections."""
import logging
import math
from dataclasses import dataclass
from pathlib import Path

import joblib

from api.schemas import (DishDiagnostics, DishForecast, ForecastDetails, ForecastRequest,
                         ForecastResponse, PredictionInterval)
from features.forecast_features import ATTENDANCE, DEMAND, attendance_frame, demand_frame, meal_record
from models.bundle import FORMAT_VERSION, model_dependencies
from services.baseline import attendance_baseline, demand_baseline

LOG = logging.getLogger("uvicorn.error")
FALLBACK = "statistical_fallback"

def load_bundle(path: Path):
    if not path.is_file():
        return None, "model_bundle_missing"
    try:
        # Joblib artifacts are executable Python serialization. Only load a
        # trusted bundle from the service's local configuration.
        bundle = joblib.load(path)
        if bundle["format"] != FORMAT_VERSION or bundle["features"] != {
            "attendance": ATTENDANCE, "dish_demand": DEMAND
        } or bundle["dependencies"] != model_dependencies():
            LOG.warning("PrePlate model bundle is incompatible. Retrain with this environment.")
            return None, "model_bundle_incompatible"
        if not isinstance(bundle["version"], str) or bundle["data_source"] not in {"synthetic","observed"}:
            raise ValueError("Invalid bundle metadata")
        for key in ("attendance", "dish_demand"):
            entry = bundle[key]
            if entry["name"] not in {FALLBACK,"linear_regression","random_forest","xgboost"}:
                raise ValueError("Unknown estimator")
            if entry["name"] != FALLBACK and not callable(getattr(entry["estimator"], "predict", None)):
                raise ValueError("Invalid estimator")
            if not math.isfinite(entry["radius"]) or entry["radius"] < 0:
                raise ValueError("Invalid interval radius")
        return bundle, None
    except Exception:
        LOG.exception("PrePlate could not load the local model bundle. Using the fallback.")
        return None, "model_bundle_unreadable"

@dataclass(frozen=True)
class Settings:
    buffer: float = .05
    max_buffer: float = .25
    buffer_mode: str = "fixed"

    def __post_init__(self):
        if not (0 <= self.buffer <= self.max_buffer <= 1):
            raise ValueError("Expected 0 <= safety buffer <= maximum safety buffer <= 1")
        if self.buffer_mode not in {"fixed", "uncertainty"}:
            raise ValueError("Buffer mode must be fixed or uncertainty")

    def prep(self, quantity, interval):
        rate = self.buffer
        if self.buffer_mode == "uncertainty" and interval is not None and quantity > 0:
            rate = min(self.max_buffer, max(rate, (interval.upper - quantity) / quantity))
        return round(quantity * (1 + rate), 2), round(100 * rate, 2)

def estimate(raw, scale):
    raw = float(raw)
    if not math.isfinite(raw):
        raise ValueError("Model returned a non-finite prediction")
    value = max(0.0, raw) * scale
    if not math.isfinite(value):
        raise ValueError("Model prediction exceeds numeric range")
    return value

def bounds(point, radius, scale, cap=None):
    delta = radius * scale
    if not math.isfinite(delta) or not math.isfinite(point + delta):
        return None
    lower, upper = max(0, point - delta), point + delta
    if cap is not None:
        return PredictionInterval(lower=math.floor(lower), upper=min(cap, math.ceil(upper)))
    return PredictionInterval(lower=math.floor(lower * 100) / 100, upper=math.ceil(upper * 100) / 100) if upper < 1e305 else None

class ForecastEngine:
    def __init__(self, bundle=None, reason="model_bundle_missing", settings=None):
        self.bundle = bundle
        self.reason = reason
        self.settings = settings or Settings()

    def predict(self, request: ForecastRequest) -> ForecastDetails:
        meal = meal_record(request)
        dishes = [{**meal, **dish.model_dump()} for dish in request.dishes]
        try:
            return self._forecast(request, meal, dishes, self.bundle, self.reason)
        except Exception:
            if self.bundle is None:
                raise
            LOG.exception("PrePlate inference failed. Recomputing the request with the fallback.")
            return self._forecast(request, meal, dishes, None, "model_inference_failed")

    def _forecast(self, request, meal, dishes, bundle, reason):
        n = request.eligible_students
        attendance = attendance_baseline(meal)
        a_model = bundle["attendance"] if bundle else None
        d_model = bundle["dish_demand"] if bundle else None
        a_algo = a_model["name"] if a_model else FALLBACK
        if a_algo != FALLBACK:
            attendance = min(n, round(estimate(a_model["estimator"].predict(attendance_frame([meal]))[0], n)))
        a_interval = bounds(attendance, a_model["radius"], n, n) if a_model else None

        valid_dishes = [d for d in dishes if d["avg_consumption_quantity"] > 0]
        ml_demand = d_model and d_model["name"] != FALLBACK
        if valid_dishes and ml_demand:
            raw = iter(d_model["estimator"].predict(demand_frame(valid_dishes)))
        any_ml = a_algo != FALLBACK
        forecasts, diagnostics = [], []
        for d in dishes:
            scale = d["avg_consumption_quantity"]
            algorithm = FALLBACK
            if scale > 0 and ml_demand:
                algorithm = d_model["name"]
                quantity = round(estimate(next(raw), scale), 2)
                any_ml = True
            else:
                quantity = demand_baseline(d)
            interval = bounds(quantity, d_model["radius"], scale) if d_model and scale > 0 else None
            prep, buffer_pct = self.settings.prep(quantity, interval)
            note = "No dish consumption history. Zero is a placeholder, not estimated zero demand." if scale == 0 else None
            forecasts.append(DishForecast(dish_id=d["dish_id"], predicted_quantity=quantity,
                                          recommended_quantity=prep, unit=d["unit"]))
            diagnostics.append(DishDiagnostics(dish_id=d["dish_id"], method="fallback" if algorithm == FALLBACK else "ml",
                                               algorithm=algorithm, interval=interval, buffer_percent=buffer_pct, note=note))
        forecast = ForecastResponse(meal_id=request.meal_id, predicted_attendance=attendance, dish_forecasts=forecasts,
                                    model_version=bundle["version"] if bundle else "preplate-baseline-v1",
                                    forecast_method="ml" if any_ml else "fallback")
        return ForecastDetails(forecast=forecast, data_source=bundle["data_source"] if bundle else None,
                               attendance_algorithm=a_algo, attendance_interval=a_interval,
                               dish_details=diagnostics,
                               fallback_reason=reason if not bundle else None if any_ml else "no_ml_forecast_for_request")
