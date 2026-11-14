# PrePlate ML

Standalone forecasting service for PrePlate. It predicts meal attendance and
dish consumption, then recommends a preparation quantity. It runs without
Express, React, Prisma, or a database. The Phase 1 request and response shapes
for `POST /forecast` are unchanged.

## Run it on Windows

1. Stop the old starter server with **Ctrl+C** if it is using port 8000.
2. Extract the new ZIP and open the **preplate-ml** folder in VS Code.
3. Run these commands in the PowerShell terminal. The launcher command uses
   Python 3.12, which is the version used to build and test this release.

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m uvicorn main:app --port 8000
```

On macOS/Linux, use `python3.12 -m venv .venv` and replace
`.\.venv\Scripts\python.exe` with `.venv/bin/python`. There is no need to
activate the virtual environment. Run the commands from the project root.

Open [Swagger UI](http://127.0.0.1:8000/docs). Expand **POST /forecast**, select
**Try it out**, then **Execute** using the prefilled valid lunch request.
Expect HTTP 200 with `forecast_method: "ml"`. You can also use the included
`sample-request.json` or import `examples/preplate.postman_collection.json`.
The bundled model is trained on **simulated data only**.

| Endpoint | Use |
| --- | --- |
| `GET /health` | Same liveness response as Phase 1. A healthy service can still be in fallback mode. |
| `POST /forecast` | Stable compact contract for the future Express caller. |
| `POST /forecast/details` | Same request, with model names, data provenance, intervals, per-dish method and applied buffer. |

Use `examples/sample-ml-response.json` and
`examples/sample-fallback-response.json` as fixtures. The response indicates
`ml` if at least one returned forecast came from a trained model. The detailed
endpoint distinguishes mixed results. Missing, unreadable, incompatible or
failed models use the statistical fallback. The original sample returns 3,865
students and 227.93 kg recommended in fallback mode with default settings.

### Contract conventions

- The response counts must add up to `eligible_students`. `no_response` is a
  real group, not missing data. `no` is implicit in the response proportions.
- `day_of_week` uses Monday = 0 and Sunday = 6.
- Keep dish IDs unique within a meal. Student interest is a signal, not a count
  of people who will actually take a dish.
- Send a dish's historical quantities in the same unit in which you want the
  result. The model scales a unitless prediction by the dish's historical
  consumption. It does not convert between units.
- A dish with no consumption history returns the Phase 1 zero placeholder, not
  evidence of zero demand. The details response identifies it explicitly.
  Use a manual kitchen estimate for that dish.
- Prediction is separate from the preparation policy. The default is a fixed
  5% buffer applied to predicted consumption. In PowerShell, set the variables
  below before starting Uvicorn to enable an uncertainty-based buffer. It
  uses the upper prediction bound when available, with a 5% floor and 25% cap.
  In the absence of an interval, it uses the floor.

```powershell
$env:PREPLATE_BUFFER_MODE = "uncertainty"
$env:PREPLATE_SAFETY_BUFFER = "0.05"
$env:PREPLATE_MAX_SAFETY_BUFFER = "0.25"
```

Use `PREPLATE_MODEL_PATH` to point to a different local model bundle. Restart
Uvicorn to load it. Only load Joblib artifacts you created or trust.

## Generate data and train from scratch

The data and model below are already included. Run these commands to reproduce
them. If using a Unix shell, use `.venv/bin/python`.

```powershell
.\.venv\Scripts\python.exe -m training.generate --days 420 --seed 42
.\.venv\Scripts\python.exe -m training.train
```

`data/attendance.csv` contains one row per meal. `data/dish_demand.csv`
contains one row per dish served at a meal. The generator models different
meal types, weekday/weekend patterns, menu combinations, response behavior,
attendance, portions, and leftovers. It uses a 42-day unexported warmup and
up to 8 earlier comparable meals or dish appearances per historical average.
Targets and outcome quantities are not used as features. In the future,
populate the request's historical values from only completed earlier meals.

Training compares the simple historical average, the serving fallback, Linear
Regression, Random Forest, and XGBoost. It keeps entire dates together and
uses successive 60% train, 15% validation, 10% interval calibration, and 15%
test periods. Attendance is modeled as a fraction of eligible students.
Dish consumption is modeled relative to the dish's own historical average,
so the model needs no assumptions about physical units. The dish model uses
pre-meal signals, not actual attendance for the forecasted meal. Rows with
known stockouts are excluded from demand training and scoring because observed
consumption in those cases may understate demand.

`reports/evaluation.json` contains per-model MAE, RMSE and MAPE, interval
coverage, global feature diagnostics, and observed leftover statistics.
Zero actuals are omitted from MAPE only. Feature diagnostics show which
inputs the model relied on across the test data, not why a specific meal was
predicted a certain way. The 90% interval target and synthetic test coverage
are not promises of coverage for real meals or a changing campus schedule.
See [the evaluation summary](reports/EVALUATION.md) for the demo results.

## Record post-meal feedback and retrain

The local workflow is manual and does not need the rest of the app. Save the
original forecast request (and optionally its response) before the meal. After
the meal, record actual attendance and each dish's prepared, consumed and
leftover quantities, using the same units and indicating any stockout.
`examples/post-meal-feedback.json` is a demo record. Use a separate demo log
so it does not become part of observed training data.

```powershell
.\.venv\Scripts\python.exe -m training.feedback --log data/demo-feedback.jsonl add examples/post-meal-feedback.json
.\.venv\Scripts\python.exe -m training.feedback --log data/demo-feedback.jsonl summary
```

For real records, use a separate log and omit `--log` to use
`data/feedback.jsonl`. Its `summary` command reports attendance/dish MAE
against saved forecasts, leftovers by unit, and stockouts. Once there are
enough completed dates, export and retrain separately from the demo bundle.

```powershell
.\.venv\Scripts\python.exe -m training.feedback export --output data/observed
.\.venv\Scripts\python.exe -m training.train --data data/observed --source observed --models models/observed --reports reports/observed
```

The trainer requires at least 40 distinct dates and usable dish records in
each split. That is a technical minimum, not evidence the dataset is adequate
for deployment. Inspect the new report against the baseline before choosing
the new bundle. The feedback exporter preserves the historical values from the
saved pre-meal request. It does not recalculate them from the outcomes. The
standalone app does not collect real outcomes automatically or schedule
retraining. Both can be wired to the product later.
