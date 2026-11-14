# PrePlate demo model evaluation

**Synthetic data only.** These results verify the modeling and serving workflow. They do not establish accuracy, interval coverage, or waste reduction on real campus meals.

Model bundle: `preplate-ml-v1-e1581e0c93`

The dataset contains **1,680 meal records** and **7,117 dish records**. Every row for a date stays in the same split.

| Period | Dates | Days |
| --- | --- | ---: |
| Train | 2025-01-01 to 2025-09-09 | 252 |
| Validation | 2025-09-10 to 2025-11-11 | 63 |
| Calibration | 2025-11-12 to 2025-12-23 | 42 |
| Test | 2025-12-24 to 2026-02-24 | 63 |

Both forecasts selected **Linear Regression** by validation MAE. For demand, selection uses MAE after dividing each quantity by the dish’s historical average, so dishes and units can be compared. The final test period was not used to fit or select the models. For example, XGBoost has a slightly lower attendance MAE on the test period, but that result did not change the selection.

## Final test results

### Attendance (students)

| Model | MAE | RMSE | MAPE |
| --- | ---: | ---: | ---: |
| Historical average | 264.54 | 342.66 | 9.67% |
| Phase 1 fallback | 170.49 | 216.74 | 6.33% |
| Linear Regression (selected) | 74.09 | 95.10 | 2.72% |
| Random Forest | 77.54 | 101.73 | 2.87% |
| XGBoost | 72.62 | 94.56 | 2.71% |

The selected model lowered test MAE by **56.5%** versus the Phase 1 fallback. The interval targets 90% coverage and covered **90.1%** of 252 synthetic test observations.

### Dish consumption (kg per dish)

| Model | MAE | RMSE | MAPE |
| --- | ---: | ---: | ---: |
| Historical average | 30.16 | 40.59 | 15.98% |
| Phase 1 fallback | 27.26 | 36.55 | 14.60% |
| Linear Regression (selected) | 15.56 | 22.24 | 7.65% |
| Random Forest | 17.28 | 24.00 | 8.58% |
| XGBoost | 15.79 | 21.75 | 7.69% |

The selected model lowered test MAE by **42.9%** versus the Phase 1 fallback. The interval targets 90% coverage and covered **88.2%** of 1,037 synthetic test observations.

## Reading the report

- MAE is the average absolute error. RMSE gives more weight to large misses. MAPE excludes zero actuals.
- See `evaluation.json` for all validation scores, calibration metadata, global input-importance diagnostics, and data fingerprints.
- The reported leftover rate describes the synthetic outcome data. It is not an estimate of waste prevented by PrePlate.
- All historical aggregates roll forward as earlier meals complete. This evaluates one-meal-ahead forecasts, not multi-day forecasts with frozen history.
- Prediction intervals use residuals from a separate calibration period. Their performance should be rechecked on observed data before relying on them.
