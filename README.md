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

## Set up and run locally (Windows)

Follow these steps from a fresh machine to clone the project, initialize MySQL, and run the complete application. Keep the backend, forecast service, and frontend running in separate terminals.

### 1. Install prerequisites

- Git
- Java 17 or newer
- MySQL Server 8.0 and MySQL Workbench (or another MySQL client)
- Node.js 22 LTS (includes npm)
- Python 3.12 for the forecasting service

### 2. Clone the repository

Open PowerShell and run:

```powershell
git clone https://github.com/iraajayakumar/PrePlate.git
Set-Location PrePlate
```

### 3. Create and initialize the MySQL database

Start MySQL Server. In MySQL Workbench, create a local connection using **Database > Manage Connections > New** (or the `+` beside MySQL Connections). For a default local installation, use:

- Hostname: `127.0.0.1`
- Port: `3306`
- Username: your MySQL account (often `root`)
- Password: the password configured for that account

Select **Test Connection**, save the connection, and open it. In a query tab, create the application database:

```sql
CREATE DATABASE IF NOT EXISTS preplate_db;
```

Open and execute these repository scripts in order, using that same connection:

1. [`database/schema.sql`](./database/schema.sql)
2. [`database/seed.sql`](./database/seed.sql)
3. [`database/migration_01_ml_forecast.sql`](./database/migration_01_ml_forecast.sql)

The migration is a one-time step for a database that has not already had it applied. It adds the ML forecast metadata and decimal quantity columns required by the current backend.

For better local credential hygiene, you can create a dedicated MySQL account from Workbench instead of using `root`:

```sql
CREATE USER 'preplate_app'@'localhost' IDENTIFIED BY 'choose-a-local-password';
GRANT ALL PRIVILEGES ON preplate_db.* TO 'preplate_app'@'localhost';
```

Use the username and password you chose in the backend configuration below.

### 4. Configure and start the backend

In PowerShell, from the repository root, copy the example configuration only if you do not already have a local `application.properties`:

```powershell
$backendConfig = "backend\src\main\resources\application.properties"
if (-not (Test-Path $backendConfig)) {
    Copy-Item "backend\src\main\resources\application.properties.example" $backendConfig
}
```

Set the MySQL credentials in this PowerShell window; replace the sample username and password with your own. These environment variables are local to the current terminal and are not committed:

```powershell
$env:DB_USERNAME = "preplate_app"
$env:DB_PASSWORD = "choose-a-local-password"
```

Start the backend:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The REST API runs at `http://localhost:8080/api`. Leave this terminal open. The backend seeds demo users and menus on startup when `preplate.seed.enabled=true` (the default).

### 5. Start the ML forecasting service

Open a second PowerShell terminal at the repository root. The ML integration uses Python 3.12; the service is optional for app startup, but the backend uses its heuristic forecast when the service is not running.

```powershell
Set-Location forecast-service
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Keep the terminal open while using ML forecasts. The service documentation is in [`forecast-service/README.md`](./forecast-service/README.md). To manually send the sample request from the repository root, use:

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:8000/forecast/details -Method Post `
  -ContentType "application/json" -InFile forecast-service/sample-request.json
```

### 6. Install and start the frontend

Open a third PowerShell terminal at the repository root:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Open `http://localhost:5173` in your browser. The frontend's default API URL is `http://localhost:8080/api`; to override it, copy `frontend\.env.example` to `frontend\.env.local` and set `VITE_API_BASE_URL`. The `.env.local` file is git-ignored.

For local sign-in, the seeded admin is `ADMIN001` / `admin123`; demo students use `student123`. These demo credentials are for development only. See [`docs/API.md`](./docs/API.md) for API routes and [`docs/TESTING.md`](./docs/TESTING.md) for test commands.

## Running only the ML service

The forecast service can also be started and tested independently. Follow step 5 above to create the Python 3.12 virtual environment, install dependencies, run tests, and launch Uvicorn. Its interactive API documentation is available at `http://127.0.0.1:8000/docs`.

## Tech stack

- Frontend: React + Vite (JavaScript), Tailwind CSS, TanStack React Query, Recharts, Axios, React Router.
- Backend: Java + Spring Boot (Maven), Spring Data JPA (Hibernate), Bean Validation.
- Database: MySQL 8.
- Forecast service: Python + FastAPI + scikit-learn/XGBoost (separate service, added later).
