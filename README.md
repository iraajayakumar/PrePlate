# PrePlate

Know the demand, prepare smarter, waste less.

Students mark meal attendance and dish preferences. Kitchen admins get demand forecasts and recommended preparation quantities. Waste is measured to close the feedback loop.

## Monorepo structure

| Folder | Purpose |
| --- | --- |
| [frontend/](./frontend/) | React + Vite (JavaScript) + Tailwind CSS |
| [backend/](./backend/) | Spring Boot (Maven, Java) modular monolith |
| [forecast-service/](./forecast-service/) | Placeholder for the teammate's Python/FastAPI ML service |
| [database/](./database/) | SQL scripts |
| [docs/](./docs/) | Architecture, conventions, decisions, testing docs |

## Getting started

Detailed setup steps will be added later in [docs/](./docs/).

## Tech stack

- Frontend: React + Vite (JavaScript), Tailwind CSS, TanStack React Query, Recharts, Axios, React Router.
- Backend: Java + Spring Boot (Maven), Spring Data JPA (Hibernate), Bean Validation.
- Database: MySQL 8.
- Forecast service: Python + FastAPI + scikit-learn/XGBoost (separate service, added later).
