# Architecture

The backend is a modular monolith: a single deployable Spring Boot application organized into modules for user/auth, meal, intelligence (demand forecasting), analytics (including waste), along with notification and fact.

The demand forecasting function is intentionally separated into a standalone ML service. The dashboard and refresh endpoints cause the backend to assemble attendance, completed-meal history, dish interest/popularity, and historical consumption, then POST the request to `/forecast/details`. The backend validates and stores the returned meal and dish forecasts (including provenance, units, intervals, and diagnostics) before returning the dashboard to the UI. When history is insufficient or the service is unavailable, the backend persists its own heuristic forecast and records the reason.

MySQL is the single database for the system.

ASCII flow:

Student -> React UI -> REST API -> Spring Boot modules -> MySQL

Admin dashboard/refresh -> ForecastService -> MlForecastClient -> POST /forecast/details -> forecast-service
                                                   -> validate and store forecast rows -> dashboard response -> UI

ForecastService -> backend heuristic (skipped/unavailable/invalid ML response)

## Why not microservices?

Microservices were rejected because the domain is tightly coupled: forecasting depends on attendance, meal responses, menus, and historical consumption patterns, so cross-service data consistency would create expensive coordination and unnecessary distributed-transaction complexity. A modular monolith keeps the application easier to evolve while still isolating major concerns by package/module.

The backend modules are organized as:

- user/auth
- meal
- intelligence
- analytics
- notification
- fact

This design keeps the service deployable as one app while preserving module boundaries and testability.
