GENERAL
- English identifiers only. No abbreviations except: id, dto, url, api, db.
- Dates in JSON: LocalDate "YYYY-MM-DD"; timestamps are ISO-8601 UTC instants (e.g. 2026-11-14T08:30:00Z). All JSON keys are camelCase.
- Enums are UPPER_SNAKE_CASE strings: MealType = BREAKFAST|LUNCH|SNACKS|DINNER; AttendanceStatus = ATTENDING|NOT_ATTENDING|NOT_SURE; Role = STUDENT|ADMIN.
DATABASE (MySQL)
- Tables: snake_case, plural (meal_responses). Columns: snake_case. PK column: id (BIGINT AUTO_INCREMENT). FK columns: <entity>_id. Booleans: is_<x> or plain adjective. Constraints: pk_/uk_/fk_/idx_ prefixes.
BACKEND (Java / Spring)
- Base package com.preplate, then one package per module: com.preplate.<module>.<layer> with layers controller, service, repository, entity, dto.
- Classes PascalCase; methods/fields camelCase; constants UPPER_SNAKE_CASE.
- Suffixes: XxxController, XxxService, XxxRepository, XxxRequest / XxxResponse (DTO records), XxxException. Entities have no suffix (User, Meal, Dish).
- REST paths: lowercase kebab-case, plural nouns, under /api (e.g. /api/dish-preferences). Admin endpoints live under /api/admin/**.
- Controllers contain no business logic; services hold logic and are @Transactional where they write; entities are never returned from controllers (always DTO records).
- Tests: unit tests <Class>Test (tagged @Tag("unit")); integration tests <Feature>IntegrationTest (tagged @Tag("integration")). Test method names: shouldDoXWhenY.
FRONTEND (React)
- Components and pages: PascalCase .jsx files, one component per file, default export, filename = component name. Hooks: useXxx.js. API modules: xxxApi.js exporting one object. Utilities: camelCase .js. Constants: UPPER_SNAKE_CASE.
- Folders lowercase: api, components/{layout,common,student,admin}, context, hooks, pages/{auth,student,admin}, utils, test.
- Styling only with Tailwind utility classes and the theme tokens in src/index.css. Brand colours: sage #7D9B65, brick #B83636, tangerine #FF9C4B, butter #FFF3A1.
- Server state through React Query (query keys are arrays like ['upcomingMeals']); never fetch inside components with raw axios.
GIT
- Conventional Commits: feat(scope):, fix(scope):, chore:, docs:, test:, refactor:. Scopes: frontend, backend, database, docs, forecast.
