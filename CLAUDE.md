# CarbonHub Backend

Spring Boot 3.5 / Java 21 REST API for corporate carbon-footprint tracking. Domain-organized packages under `com.piedpiper.carbonhub` (`auth`, `emision`, `empresa`, `invitacion`, `limite`, `notification`, `user`).

## Coding conventions — read before writing code

**[docs/CONVENTIONS.md](docs/CONVENTIONS.md) is the single source of truth** for structure, naming, controllers, services, JPA entities, DTOs, validation, error handling, mappers, and tests. Follow it. If you need to deviate, say so explicitly and explain why — don't do it silently.

@docs/CONVENTIONS.md

## Build

```bash
./mvnw test          # full suite; JAVA_HOME must point at a JDK 21
```

## Notes

- Code is **Spanish**: domain field names, user-facing messages, test names, commits, and PRs. `docs/CONVENTIONS.md` is written in English for brevity — that does not make the codebase English.
- This file is a thin pointer on purpose. Conventions live in `docs/CONVENTIONS.md` only, so Claude, Codex, and ChatGPT never drift apart. Add rules there, not here.
