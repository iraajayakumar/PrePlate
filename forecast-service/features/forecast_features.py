"""One feature implementation for offline training and online inference."""
import pandas as pd

MEALS = ("breakfast", "lunch", "dinner", "snacks")
ATTENDANCE = (
    "eligible_students", "yes_rate", "unsure_rate", "no_response_rate",
    "historical_attendance_rate", "avg_waste_percentage", "dish_count",
    "menu_interest_rate", "menu_popularity",
    *(f"meal_{meal}" for meal in MEALS),
    *(f"day_{day}" for day in range(7)),
)
DEMAND = ATTENDANCE + (
    "dish_interest_rate", "dish_popularity",
)

def summarize_menu(eligible, dishes):
    dishes = list(dishes)
    n = len(dishes)
    return {
        "dish_count": n,
        "menu_interest_rate": sum(d["student_interest"] for d in dishes) / (n * eligible) if n else 0.0,
        "menu_popularity": sum(d["historical_popularity"] for d in dishes) / n if n else 0.0,
    }

def meal_record(request):
    return {
        "meal_type": request.meal_type,
        "day_of_week": request.day_of_week,
        "eligible_students": request.eligible_students,
        **request.responses.model_dump(),
        **request.historical.model_dump(),
        **summarize_menu(request.eligible_students, (d.model_dump() for d in request.dishes)),
    }

def attendance_frame(rows):
    return pd.DataFrame.from_records([
        {
            "eligible_students": n,
            "yes_rate": row["yes"] / n,
            "unsure_rate": row["unsure"] / n,
            "no_response_rate": row["no_response"] / n,
            "historical_attendance_rate": min(1.0, row["avg_attendance"] / n),
            "avg_waste_percentage": row["avg_waste_percentage"],
            "dish_count": row["dish_count"],
            "menu_interest_rate": row["menu_interest_rate"],
            "menu_popularity": row["menu_popularity"],
            **{f"meal_{meal}": int(row["meal_type"] == meal) for meal in MEALS},
            **{f"day_{day}": int(row["day_of_week"] == day) for day in range(7)},
        }
        for row in rows
        for n in (row["eligible_students"],)
    ], columns=list(ATTENDANCE))

def demand_frame(rows):
    # Scale demand by the historical quantity for the same dish and unit.
    # Train on a dimensionless ratio, so kg, grams, and servings are not mixed.
    rows = list(rows)
    frame = attendance_frame(rows)
    frame["dish_interest_rate"] = [r["student_interest"] / r["eligible_students"] for r in rows]
    frame["dish_popularity"] = [r["historical_popularity"] for r in rows]
    return frame[list(DEMAND)]
