# Backend API

Base URL: `http://localhost:8080/api`

All endpoints except login, registration, and sustainability facts require an `Authorization: Bearer <token>` header. Obtain a token from `POST /auth/login`. Admin routes require an admin account. Request and response JSON use camelCase; enum values are uppercase strings. Dates use `YYYY-MM-DD`; timestamps are ISO-8601 instants.

Errors use this shape:

```json
{
  "status": 400,
  "message": "Invalid request.",
  "timestamp": "2026-11-14T08:10:00Z"
}
```

## Authentication

| Method | Path | Access | Contract |
| --- | --- | --- | --- |
| POST | `/auth/login` | Public | Body: `{ "registrationNumber": "...", "password": "..." }`; returns `{ "token": "...", "user": { ... } }`. |
| POST | `/auth/register` | Public | Body: `registrationNumber`, `fullName`, `department`, `hostelName`, `hostelRoom`, `password`; password must contain at least 6 characters. Returns token and user. |
| POST | `/auth/logout` | Authenticated | Invalidates the bearer token; returns `{ "message": "Logged out" }`. |
| GET | `/auth/me` | Authenticated | Returns the signed-in user. |

## Shared and student routes

| Method | Path | Access | Contract |
| --- | --- | --- | --- |
| GET | `/meal-schedule` | Authenticated | Returns meal serving times and response deadlines. |
| GET | `/sustainability-facts` | Public | Returns available sustainability facts. |
| GET | `/meals/upcoming` | Authenticated | Returns the next open meal of each meal type, current response, dish preferences, and menu. |
| PUT | `/meals/{mealId}/response` | Authenticated | Body: `{ "status": "ATTENDING" }`; status is `ATTENDING`, `NOT_ATTENDING`, or `NOT_SURE`. Closed response windows return a conflict. |
| PUT | `/meals/{mealId}/dish-preferences` | Authenticated | Body: `{ "dishIds": [1, 2] }`. Preferences require an `ATTENDING` response and every dish must belong to that meal. |
| GET | `/student/history?period=TODAY` | Authenticated | `period`: `TODAY`, `LAST_WEEK`, or `LAST_MONTH`. |
| GET | `/student/analytics?scope=THIS_MONTH` | Authenticated | `scope`: `THIS_MONTH` or `ALL_TIME`. |
| GET | `/notifications` | Authenticated | Returns unread count and recent notifications; generates any due time-based notifications before listing. |
| POST | `/notifications/read-all` | Authenticated | Marks the current user's notifications read and returns the number updated. |

## Admin routes

| Method | Path | Access | Contract |
| --- | --- | --- | --- |
| GET | `/admin/dishes` | Admin | Lists dishes. |
| POST | `/admin/dishes` | Admin | Creates a dish; body includes `name`, optional `description`, nutrition values, optional `ingredients`, and `vegetarian`. |
| GET | `/admin/meals?from=YYYY-MM-DD&to=YYYY-MM-DD` | Admin | Lists meals in the inclusive date range. |
| POST | `/admin/meals` | Admin | Creates a meal with `mealDate`, `mealType`, and non-empty `dishIds`. |
| PUT | `/admin/meals/{mealId}` | Admin | Replaces the menu; body: `{ "dishIds": [1, 2] }`. |
| DELETE | `/admin/meals/{mealId}` | Admin | Deletes a meal and returns a message. |
| GET | `/admin/dashboard?mealDate=YYYY-MM-DD&mealType=DINNER` | Admin | Returns response totals and forecast for the selected meal; forecast is generated if absent. |
| POST | `/admin/forecasts/{mealId}/refresh` | Admin | Regenerates the forecast and returns the dashboard. |
| GET | `/admin/consumption/{mealId}` | Admin | Gets dish-level preparation and consumption quantities; `recommendedQuantity` is a decimal or `null`. |
| POST | `/admin/consumption` | Admin | Saves `mealId` and non-empty `records`, each with `dishId`, `preparedQuantity`, and `consumedQuantity`. |
| GET | `/admin/analytics/waste?days=14` | Admin | Returns waste totals, daily trend, and breakdowns by meal type and dish. |

Meal types are `BREAKFAST`, `LUNCH`, `SNACKS`, and `DINNER`. Missing/invalid request values return a 400 response; unauthenticated calls return 401, and student access to admin routes returns 403.

## Forecast service integration

Forecasting calls the ML service by default. Configure `preplate.forecast.enabled`, `preplate.forecast.base-url`, `preplate.forecast.timeout-ms`, and `preplate.forecast.item-unit` as needed. If history is insufficient, the request is invalid, or the ML call fails or returns an invalid response, the backend records the reason and uses its heuristic forecast.

The backend sends eligible-student counts, all four response groups, up to 14 completed same-type meal history points, and per-dish interest, popularity, consumption average, and unit to `POST /forecast/details`. An insufficient completed history or invalid count set skips the remote call. Timeouts, service errors, and invalid responses produce a persisted `HEURISTIC` forecast with a `fallbackReason`.

The dashboard forecast contains the existing attendance values plus `modelVersion`, `forecastMethod`, `dataSource`, and `fallbackReason`. `source` is `ML_SERVICE`, `ML_FALLBACK` (the ML service returned its statistical fallback), or `HEURISTIC` (the backend's own fallback). `confidence` represents the service's target prediction-interval coverage, not a probability that the point estimate is correct. Dish demand and preparation quantities are JSON numbers with up to two decimal places. Dish forecast values also include `unit`, `method`, `note`, `intervalLower`, and `intervalUpper`; the ML service's recommended dish quantity already includes its preparation buffer.
