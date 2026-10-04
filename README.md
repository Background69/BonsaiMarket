# BonsaiMarket development foundation

Stack: Java 21, Spring Boot 4.1.1, Maven, MySQL 8.4, Flyway, Spring Security, Vue 3, Vite and Vue Router. The frontend currently exposes six named route placeholders. Legacy Vue pages remain in the repository but are not connected to this shell.

## Requirements

- Java 21
- Node.js and npm compatible with Vite 8
- Docker with Docker Compose for local MySQL

## Start locally

1. Copy `.env.example` to `.env` and set private `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD` values. `.env` is ignored by Git. Docker Compose reads it automatically.
2. Start MySQL: `docker compose up -d mysql`.
3. Export the same database credentials to the backend process and enable the dev profile. In PowerShell:

   ```powershell
   $env:DB_USERNAME = 'bonsai'
   $env:DB_PASSWORD = '<DB_PASSWORD from .env>'
   $env:SPRING_PROFILES_ACTIVE = 'dev'
   .\mvnw.cmd spring-boot:run
   ```

   On macOS/Linux, use `./mvnw spring-boot:run` after exporting the variables. `DB_URL` defaults to `jdbc:mysql://localhost:3306/bonsai_market`; set it when the database is elsewhere. The backend does not read `.env` automatically. If the Windows wrapper fails, use a local Maven installation with `mvn spring-boot:run`.
4. In another terminal, run `cd frontend`, `npm install`, then `npm run dev`.
5. Check `http://localhost:8080/actuator/health` for `UP`. Only the health actuator endpoint is exposed.

The default profile runs only `db/migration` (V1–V8). The `dev` profile additionally runs `db/dev/V9__seed_development_data.sql`. Flyway applies each version once. Hibernate is set to `ddl-auto: validate`; Flyway owns the schema. Use a fresh local database when changing profile history, and add new migrations rather than editing applied migrations.

## Development seed

**DEV ONLY — DO NOT USE IN PRODUCTION.** Profile `dev` creates these demo accounts, all with BCrypt hash of `Password123!`:

| Role | Email |
|---|---|
| ADMIN | `admin@bonsaimarket.local` |
| SELLER | `seller@bonsaimarket.local` |
| CUSTOMER | `user@bonsaimarket.local` |

It also creates 5 categories, 1 seller store, 20 products, 1 customer cart with 3 items, 75 orders with 189 items, 12 reviews, and 4 wishlist entries. Order dates span approximately 85 days before first migration. These accounts cannot log in yet: authentication is a later task.

Run `node scripts/check-dev-seed.mjs` to check seed arithmetic without MySQL. After starting MySQL, run the queries in `db/verify_dev_seed.sql` to verify actual row counts and order totals.

## Build

```text
./mvnw clean test
cd frontend
npm run build
```

On Windows use `.\mvnw.cmd clean test`. The Spring smoke test excludes database auto configuration so `clean test` can run without MySQL; application startup is the check for Flyway and JPA validation. Business API, login, checkout, payment, admin and seller workflows are intentionally absent in this foundation.
