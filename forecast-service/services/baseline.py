"""The Phase 1 statistical fallback, preserved for the original inputs."""
def attendance_baseline(row):
    n = row["eligible_students"]
    history_rate = min(1.0, row["avg_attendance"] / n)
    current_rate = (row["yes"] + 0.5 * row["unsure"] + history_rate * row["no_response"]) / n
    return round(max(0, min(n, n * (0.7 * current_rate + 0.3 * history_rate))))

def demand_baseline(row):
    history = row["avg_attendance"]
    scale = attendance_baseline(row) / history if history > 0 else 0.0
    return round(max(0.0, row["avg_consumption_quantity"] * scale), 2)
