# PrePlate

Know the demand, prepare smarter, waste less.

Students mark meal attendance and dish preferences. Kitchen admins get demand forecasts and recommended preparation quantities. Waste is measured to close the feedback loop.

Live URL: https://preplate-frontend.iraajayakumar.workers.dev

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

## Deployment (free tier)

PrePlate can be hosted online at no cost for demos and showcases. Each part of the system runs on its own free service, and all of them deploy from this GitHub repository.

| Component | Platform | Notes |
| --- | --- | --- |
| Frontend (React) | Cloudflare Pages (or Vercel) | Static site, no sleep |
| Backend (Spring Boot) | Render, Docker web service | Free instance: 512 MB RAM, sleeps after about 15 minutes idle |
| Forecast service (FastAPI) | Render, Python web service | Same free-instance limits as the backend |
| Database (MySQL) | TiDB Cloud free tier | MySQL-compatible. Check its current free-tier terms |

Free tiers change over time, so confirm the current limits on each provider's pricing page. These services are suitable for demos, not for production use. The demo accounts are public and the app has no production-grade security, so do not load real student data.

Deploy in this order, because each step needs a value from the previous one: database, forecast service, backend, frontend, then update the backend's allowed origin.

### Deployment values to collect

Keep a note of these as you go:

```
TIDB_HOST, TIDB_USER, TIDB_PASSWORD
ML_URL        (forecast service URL)
BACKEND_URL   (backend URL)
FRONTEND_URL  (frontend URL)
```

### 1. Add the backend Dockerfile

Create `backend/Dockerfile`, commit it, and push:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx320m -XX:+UseSerialGC"
CMD ["java","-jar","app.jar"]
```

The memory cap keeps the JVM inside the 512 MB free-tier limit.

### 2. Database on TiDB Cloud

1. Sign up at [tidbcloud.com](https://tidbcloud.com) and create a free **Starter** cluster.
2. Open **Connect** on the cluster to get the host, port (`4000`), username (it may carry a prefix such as `abc123.root`; copy it in full) and password.
3. In MySQL Workbench, create a connection with those details. In the **SSL** tab set *Use SSL* to **Require**, then test it.
4. Run, in order: `database/schema.sql`, `database/seed.sql`, `database/migration_01_ml_forecast.sql`.
5. Verify with `USE preplate_db; SHOW TABLES;`, which should list 12 tables.

If a script fails on foreign keys, note the error and adjust the schema for TiDB. A paid managed MySQL (for example Aiven's MySQL plan) is the fallback. Aiven has retired its free MySQL plan.

### 3. Forecast service on Render

1. Sign up at [render.com](https://render.com) with GitHub and allow access to this repository.
2. Choose **New + > Web Service** and select the repository.
3. Settings:
   - Name: `preplate-ml`
   - Root Directory: `forecast-service`
   - Runtime: Python 3
   - Build Command: `pip install -r requirements.txt`
   - Start Command: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - Instance Type: Free
   - Environment variable `PYTHON_VERSION`: a 3.12 or 3.13 release (the model bundle checks library versions, so keep the pinned `requirements.txt`)
4. Deploy, then copy the service URL as `ML_URL`.
5. Verify: open `ML_URL/docs` and run the sample under `POST /forecast`. Expect `"forecast_method": "ml"` and 3807 students. If the response shows `fallback` with `model_bundle_incompatible`, the installed library versions differ from the pinned ones.

### 4. Backend on Render

1. Choose **New + > Web Service**, select the same repository.
2. Settings:
   - Name: `preplate-backend`
   - Root Directory: `backend`
   - Runtime: Docker
   - Instance Type: Free
3. Environment variables:

| Name | Value |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<TIDB_HOST>:4000/preplate_db?sslMode=VERIFY_IDENTITY&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | `<TIDB_USER>` |
| `SPRING_DATASOURCE_PASSWORD` | `<TIDB_PASSWORD>` |
| `SERVER_PORT` | `10000` |
| `PREPLATE_FORECAST_ENABLED` | `true` |
| `PREPLATE_FORECAST_BASE_URL` | `<ML_URL>` |
| `PREPLATE_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` for now, replaced in step 6 |

4. Deploy. The first build takes several minutes, and the first start seeds demo users and history.
5. Copy the service URL as `BACKEND_URL`.
6. Verify: `BACKEND_URL/api/sustainability-facts` should return JSON.

### 5. Frontend on Cloudflare Pages

1. At [dash.cloudflare.com](https://dash.cloudflare.com), open **Workers & Pages**, create a **Pages** project and connect it to this repository (use the **Pages** option and **Connect to Git**, not the Workers flow).
2. Build settings:
   - Build command: `npm run build`
   - Build output directory: `dist`
   - Root directory: `frontend`
3. If your screen does not offer a root directory field, leave it blank and use these instead:
   - Build command: `cd frontend && npm install && npm run build`
   - Build output directory: `frontend/dist`
4. Environment variables (add them before the first build, because Vite embeds them at build time):
   - `VITE_API_BASE_URL` = `<BACKEND_URL>/api`
   - `NODE_VERSION` = `22`
5. Deploy and copy the site URL as `FRONTEND_URL`.

**Alternative: Vercel.** Import the repository and set **Root Directory** to `frontend`. Add `frontend/vercel.json` so that refreshing a deep link such as `/food-history` works:

```json
{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
```

Set the same `VITE_API_BASE_URL` variable. Vercel's free plan is for non-commercial use.

### 6. Allow the frontend to call the backend

In the Render backend service, open **Environment** and set `PREPLATE_CORS_ALLOWED_ORIGINS` to `<FRONTEND_URL>` (no trailing slash, no path, for example `https://preplate.pages.dev`). Save, and wait for the automatic redeploy.

### 7. Verify the deployment

1. Open `FRONTEND_URL`. The first load can take about a minute while the backend wakes up.
2. Sign in as a student (`RA2111003010001` / `student123`), confirm a meal and add a dish. Check **Food History**.
3. Sign in as the admin (`ADMIN001` / `admin123`), open the dashboard and select **Refresh forecast**. The source label should read **Trained ML model**.

### Deployment notes and troubleshooting

- **Cold starts:** Free services sleep when idle and take about a minute to wake. Before a demo, open `BACKEND_URL/api/sustainability-facts` and `ML_URL/docs` a couple of minutes early. If the forecast service is still asleep, the dashboard shows the built-in estimate and says so. Refresh once it is awake.
- **Do not ping both Render services to keep them awake.** Render's free instance hours are shared across a workspace, so two always-on services would exhaust them.
- **Demo the 30-minute reminder at any hour:** set the backend env var `PREPLATE_DEV_SIMULATED_NOW` (for example `2026-11-15T13:29:30`) and redeploy.
- **CORS error in the browser console:** repeat step 6 and check the exact origin.
- **"Cannot reach the PrePlate server" in the app:** check `VITE_API_BASE_URL` in the frontend host's settings and redeploy the frontend.
- **Backend fails to start:** read the Render **Logs** tab. The common causes are a wrong database username, password or host, an unreachable TiDB cluster, or the memory limit.
- **Updating:** pushing to GitHub rebuilds each service automatically.

## Tech stack

- Frontend: React + Vite (JavaScript), Tailwind CSS, TanStack React Query, Recharts, Axios, React Router.
- Backend: Java + Spring Boot (Maven), Spring Data JPA (Hibernate), Bean Validation.
- Database: MySQL 8 (TiDB Cloud when deployed).
- Forecast service: Python + FastAPI + scikit-learn/XGBoost (separate service).
