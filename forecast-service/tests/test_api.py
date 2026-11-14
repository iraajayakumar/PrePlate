import copy
import json

import joblib
import pytest
from fastapi.testclient import TestClient

from main import SAMPLE, ROOT, create_app
from services.forecast import Settings

BASELINE_RESULT = {
    "meal_id": 142,
    "predicted_attendance": 3865,
    "dish_forecasts": [{"dish_id": 12, "predicted_quantity": 217.08,
                        "recommended_quantity": 227.93, "unit": "kg"}],
    "model_version": "preplate-baseline-v1", "forecast_method": "fallback",
}

@pytest.fixture
def client():
    with TestClient(create_app(ROOT / "models" / "forecast.joblib", Settings())) as test_client:
        yield test_client

def test_health(client):
    assert client.get("/health").json() == {"status": "ok", "service": "preplate-ml"}

def test_forecast_contract_and_ml(client):
    response = client.post("/forecast", json=SAMPLE)
    assert response.status_code == 200
    out = response.json()
    assert set(out) == set(BASELINE_RESULT)
    assert out["forecast_method"] == "ml"
    assert out["meal_id"] == 142
    assert 0 <= out["predicted_attendance"] <= 5000
    dish = out["dish_forecasts"][0]
    assert set(dish) == set(BASELINE_RESULT["dish_forecasts"][0])
    assert dish["dish_id"] == 12 and dish["unit"] == "kg"
    assert dish["recommended_quantity"] == round(dish["predicted_quantity"] * 1.05, 2)
    report = json.loads((ROOT / "reports" / "evaluation.json").read_text())
    assert out["model_version"] == report["version"]

def test_details_contain_estimates_and_uncertainty(client):
    details = client.post("/forecast/details", json=SAMPLE).json()
    assert details["forecast"] == client.post("/forecast", json=SAMPLE).json()
    assert details["data_source"] == "synthetic"
    point = details["forecast"]["predicted_attendance"]
    assert 0 <= details["attendance_interval"]["lower"] <= point <= details["attendance_interval"]["upper"] <= 5000
    assert details["dish_details"][0]["interval"]["target_coverage"] == .9

def test_openapi_includes_valid_example(client):
    data = client.get("/openapi.json").json()
    spec = data["paths"]["/forecast"]["post"]["requestBody"]["content"]["application/json"]
    sample = spec["examples"]["lunch"]["value"]
    assert client.post("/forecast", json=sample).status_code == 200

@pytest.mark.parametrize("problem", ["missing","corrupt","incompatible","inference"])
def test_fallback(problem, tmp_path):
    path = tmp_path / "forecast.joblib"
    reason = "model_bundle_missing"
    if problem == "corrupt":
        path.write_text("not a joblib bundle")
        reason = "model_bundle_unreadable"
    elif problem in {"incompatible", "inference"}:
        bundle = joblib.load(ROOT / "models" / "forecast.joblib")
        if problem == "incompatible":
            bundle["format"] = -1
            reason = "model_bundle_incompatible"
        else:
            bundle["attendance"]["estimator"] = BrokenEstimator()
            reason = "model_inference_failed"
        joblib.dump(bundle, path)
    with TestClient(create_app(path, Settings())) as fallback:
        assert fallback.post("/forecast", json=SAMPLE).json() == BASELINE_RESULT
        details = fallback.post("/forecast/details", json=SAMPLE).json()
        assert details["fallback_reason"] == reason
        assert details["attendance_interval"] is None

class BrokenEstimator:
    def predict(self, frame):
        raise RuntimeError("simulated broken estimator")

@pytest.mark.parametrize("change", ["bad_counts","bad_interest","bad_day","bad_history","duplicate_dish"])
def test_invalid_inputs(change, client):
    payload = copy.deepcopy(SAMPLE)
    if change == "bad_counts":
        payload["responses"]["yes"] += 1
    elif change == "bad_interest":
        payload["dishes"][0]["student_interest"] = 5001
    elif change == "bad_day":
        payload["day_of_week"] = 7
    elif change == "bad_history":
        payload["historical"]["avg_attendance"] = "Infinity"
    else:
        payload["dishes"].append(payload["dishes"][0].copy())
    assert client.post("/forecast", json=payload).status_code == 422

def test_empty_menu_and_no_dish_history(client):
    payload = copy.deepcopy(SAMPLE)
    payload["dishes"] = []
    out = client.post("/forecast", json=payload).json()
    assert out["dish_forecasts"] == []
    payload = copy.deepcopy(SAMPLE)
    payload["dishes"][0]["avg_consumption_quantity"] = 0
    details = client.post("/forecast/details", json=payload).json()
    assert details["forecast"]["forecast_method"] == "ml"
    dish = details["dish_details"][0]
    assert dish["method"] == "fallback" and dish["interval"] is None
    assert "placeholder" in dish["note"]

def test_same_dish_demand_in_different_units(client):
    payload = copy.deepcopy(SAMPLE)
    expected = client.post("/forecast", json=payload).json()["dish_forecasts"][0]
    payload["dishes"][0].update(unit="g", avg_consumption_quantity=205000)
    got = client.post("/forecast", json=payload).json()["dish_forecasts"][0]
    assert got["unit"] == "g"
    assert got["predicted_quantity"] / 1000 == pytest.approx(expected["predicted_quantity"], abs=.01)

def test_uncertainty_buffer_is_capped():
    with TestClient(create_app(ROOT / "models" / "forecast.joblib", Settings(.05,.09,"uncertainty"))) as client:
        details = client.post("/forecast/details", json=SAMPLE).json()
        forecast = details["forecast"]["dish_forecasts"][0]
        assert details["dish_details"][0]["buffer_percent"] == 9
        assert forecast["recommended_quantity"] == round(forecast["predicted_quantity"] * 1.09, 2)
