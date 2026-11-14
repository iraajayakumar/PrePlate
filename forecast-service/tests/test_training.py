from datetime import date

import numpy as np
import pandas as pd
import pytest

from main import ROOT
from training.generate import generate, HISTORY_WINDOW
from training.train import load_training_data, split_dates

@pytest.fixture(scope="module")
def demo_data():
    return generate(days=75, seed=11)

def test_generated_data_consistency(demo_data):
    meals, dishes = demo_data
    assert len(meals) == 300
    assert meals.meal_id.is_unique
    assert not dishes.duplicated(["meal_id","dish_id"]).any()
    assert (meals[["yes","no","unsure","no_response"]].sum(axis=1) == meals.eligible_students).all()
    assert (meals.actual_attendance <= meals.eligible_students).all()
    assert (dishes.student_interest <= dishes.eligible_students).all()
    assert np.allclose(dishes.actual_consumed_quantity + dishes.actual_leftover_quantity,
                       dishes.actual_prepared_quantity)
    assert meals.day_of_week.tolist() == [date.fromisoformat(s).weekday() for s in meals.meal_date]

def test_history_only_uses_prior_dates(demo_data):
    meals, dishes = demo_data
    lunches = meals[(meals.meal_type == "lunch") & (meals.day_of_week < 5)]
    row = lunches.iloc[-1]
    prior = lunches[lunches.meal_date < row.meal_date].tail(HISTORY_WINDOW)
    assert row.avg_attendance == pytest.approx(prior.actual_attendance.mean(), abs=.0001)
    dal = dishes[dishes.dish_id == 12]
    row = dal.iloc[-1]
    prior = dal[dal.meal_date < row.meal_date].tail(HISTORY_WINDOW)
    assert row.avg_consumption_quantity == pytest.approx(prior.actual_consumed_quantity.mean(), abs=.0001)

def test_generator_reproducible_prefix(demo_data):
    first, second = generate(days=60, seed=11)
    pd.testing.assert_frame_equal(first, demo_data[0].iloc[:len(first)])
    pd.testing.assert_frame_equal(second, demo_data[1].iloc[:len(second)])

def test_splits_preserve_order_and_group_dates(demo_data):
    splits = list(split_dates(demo_data[0]).values())
    assert all(max(earlier) < min(later) for earlier,later in zip(splits,splits[1:]))
    assert sum(map(len,splits)) == 75

def test_training_inputs_reconstruct_menu_from_dish_rows():
    meals, dishes = load_training_data(ROOT / "data")
    assert len(meals) == 1680
    assert len(dishes) == 7117
    counts = dishes.groupby("meal_id").size()
    assert meals.set_index("meal_id").dish_count.equals(counts)
