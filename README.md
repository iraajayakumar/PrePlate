# PrePlate

Know the demand, prepare smarter, waste less.

Students mark meal attendance and dish preferences. Kitchen admins get demand forecasts and recommended preparation quantities. Waste is measured to close the feedback loop.

## Monorepo structure

| Folder | Purpose |
| --- | --- |
| [frontend/](./frontend/) | React + Vite (JavaScript) + Tailwind CSS |
| [backend/](./backend/) | Spring Boot (Maven, Java) modular monolith |
| [forecast-service/](./forecast-service/) | Python + FastAPI meal and dish forecasting service |
| [database/](./database/) | SQL scripts |
| [docs/](./docs/) | Architecture, conventions, decisions, testing docs |

## Getting started

1. Install Java 17 or newer and MySQL 8.
2. Create the `preplate_db` database and run [`database/schema.sql`](./database/schema.sql), [`database/seed.sql`](./database/seed.sql), and [`database/migration_01_ml_forecast.sql`](./database/migration_01_ml_forecast.sql), in that order, using your MySQL client.
3. In `backend/`, copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`. Set MySQL credentials through `DB_USERNAME` and `DB_PASSWORD` environment variables, or copy `src/main/resources/application-local.properties.example` to `src/main/resources/application-local.properties` and set them there. Both local files are git-ignored.
4. Start the backend from `backend/`:

   ```powershell
   .\mvnw.cmd "-Dspring-boot.run.profiles=local" spring-boot:run
   ```

   The API is available at `http://localhost:8080/api`. The frontend runs at `http://localhost:5173` and is allowed by the default CORS configuration.
5. Start the frontend separately using the instructions in [`frontend/`](./frontend/).

On its first run, the backend seeds demo users and menu data when the database has been initialized with the supplied SQL scripts. The demo admin is `ADMIN001` / `admin123`; demo students use `student123`. These credentials are for local development only. See [`docs/API.md`](./docs/API.md) for routes and [`docs/TESTING.md`](./docs/TESTING.md) to run tests.

## Running the ML service

The backend uses the standalone FastAPI forecasting service at `http://127.0.0.1:8000` by default. From `forecast-service/`, create a Python 3.13 virtual environment, install its dependencies, run its tests, and start Uvicorn:

```powershell
py -3.13 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Keep the ML service running while using the admin dashboard. Backend configuration uses `preplate.forecast.enabled=true`, `preplate.forecast.base-url=http://127.0.0.1:8000`, and `preplate.forecast.timeout-ms=5000`. To manually probe the detailed response, from the repository root run:

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:8000/forecast/details -Method Post `
  -ContentType "application/json" -InFile forecast-service/sample-request.json
```

## Tech stack

- Frontend: React + Vite (JavaScript), Tailwind CSS, TanStack React Query, Recharts, Axios, React Router.
- Backend: Java + Spring Boot (Maven), Spring Data JPA (Hibernate), Bean Validation.
- Database: MySQL 8.
- Forecast service: Python + FastAPI + scikit-learn/XGBoost (separate service, added later).
