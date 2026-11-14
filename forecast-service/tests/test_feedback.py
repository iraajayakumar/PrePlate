import copy

import pandas as pd
import pytest
from pydantic import ValidationError

from main import SAMPLE
from training.feedback import Feedback, append_feedback, export, read_feedback, summarize
from tests.test_api import BASELINE_RESULT

def example():
    return {
        "meal_date":"2026-11-13", "request": copy.deepcopy(SAMPLE), "forecast": copy.deepcopy(BASELINE_RESULT),
        "actual_attendance": 3812,
        "dishes": [{"dish_id": 12, "unit": "kg", "actual_prepared_quantity": 228,
                    "actual_consumed_quantity": 214, "actual_leftover_quantity": 14, "stockout": False}],
    }

def test_post_meal_feedback_and_export(tmp_path):
    entry = Feedback.model_validate(example())
    path = tmp_path / "feedback.jsonl"
    append_feedback(path, entry)
    out = tmp_path / "observed"
    assert export(path, out) == (1, 1)
    assert pd.read_csv(out / "attendance.csv").actual_attendance.iloc[0] == 3812
    dish = pd.read_csv(out / "dish_demand.csv").iloc[0]
    assert dish.actual_consumed_quantity == 214
    assert dish.avg_consumption_quantity == 205
    summary = summarize(read_feedback(path))
    assert summary["attendance_forecast_mae"] == 53
    assert summary["by_unit"]["kg"]["dish_forecast_mae"] == 3.08
    assert summary["by_unit"]["kg"]["leftover_percent"] == 6.14
    with pytest.raises(ValueError, match="already"):
        append_feedback(path, entry)

def test_stockout_not_scored_as_demand():
    raw = example()
    raw["dishes"][0]["stockout"] = True
    stats = summarize([Feedback.model_validate(raw)])["by_unit"]["kg"]
    assert stats["stockouts"] == 1
    assert stats["dish_forecast_mae"] is None

def test_quantities_must_balance():
    raw = example()
    raw["dishes"][0]["actual_leftover_quantity"] = 13
    with pytest.raises(ValidationError, match="Prepared must equal"):
        Feedback.model_validate(raw)
