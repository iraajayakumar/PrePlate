# Architecture

The backend is a modular monolith: a single deployable Spring Boot application organized into modules for user/auth, meal, intelligence (demand forecasting), analytics (including waste), along with notification and fact.

The demand forecasting function is intentionally separated into a standalone ML service that receives HTTP calls and can gracefully fall back to a historical-average heuristic when unavailable. The React frontend talks only to the backend over REST/JSON APIs.

MySQL is the single database for the system.

ASCII flow:

Student -> React UI -> REST API -> Spring Boot modules -> MySQL

Spring Boot -> (HTTP) -> Forecast service (optional)

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
