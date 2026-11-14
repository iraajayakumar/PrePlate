# Decisions

| Decision | Details |
| --- | --- |
| Backend is Spring Boot (Java) | The backend is implemented with Spring Boot (Java) instead of the originally planned Node/Express + Prisma stack. |
| ORM is Spring Data JPA | Spring Data JPA (Hibernate) is the persistence layer. |
| Database is MySQL 8 | MySQL 8 is used locally instead of PostgreSQL. |
| Redis caching is out of scope | Redis caching is intentionally deferred for now. |
| Authentication is intentionally simple | Dummy users and registration are used; token state is held in memory. Security hardening is out of scope for this submission. |
| Forecast service is separate | The backend calls a separate forecast service over HTTP and falls back to a heuristic when it is unreachable. |
| Spring Boot version | Spring Boot 4.1.2 was selected because Spring Initializr did not offer a stable 3.5.x release. |
