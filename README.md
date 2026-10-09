# BonsaiMarket development foundation

Stack: Java 21, Spring Boot 4.1.1, Maven, MySQL 8.4, Flyway, Spring Security, Vue 3, Vite and Vue Router. The legacy Vue
marketplace is mounted through Vue Router. Home, Products, Categories and Stores read the public Spring catalog API;
Admin and Seller product lists show the public catalog as read-only demo data.

## Requirements

- Java 21
- Node.js and npm compatible with Vite 8
- Docker with Docker Compose for local MySQL

## Start locally

1. Copy `.env.example` to `.env` and set private `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD` values. `.env` is ignored by
   Git. Docker Compose reads it automatically.
2. Start MySQL: `docker compose up -d mysql`.
3. Export the same database credentials to the backend process and enable the dev profile. In PowerShell:

   ```powershell
   $env:DB_USERNAME = 'bonsai'
   $env:DB_PASSWORD = '<DB_PASSWORD from .env>'
   $env:SPRING_PROFILES_ACTIVE = 'dev'
   .\mvnw.cmd spring-boot:run
   ```

   On macOS/Linux, use `./mvnw spring-boot:run` after exporting the variables. `DB_URL` defaults to
   `jdbc:mysql://localhost:3306/bonsai_market`; set it when the database is elsewhere. The backend does not read `.env`
   automatically. If the Windows wrapper fails, use a local Maven installation with `mvn spring-boot:run`.
4. In another terminal, run `cd frontend`, then `npm run dev` with the existing project dependencies.
5. Check `http://localhost:8080/actuator/health` for `UP`, then `/api/categories`, `/api/products`, `/api/products/1`,
   `/api/stores` and `/api/stores/1`. Vite proxies browser `/api` requests to Spring on port 8080. Only the health
   actuator endpoint is exposed.

The default profile runs only `db/migration` (V1–V8). The `dev` profile additionally runs
`db/dev/V9__seed_development_data.sql`. Flyway applies each version once. Hibernate is set to `ddl-auto: validate`;
Flyway owns the schema. Use a fresh local database when changing profile history, and add new migrations rather than
editing applied migrations.

## Development seed

**DEV ONLY — DO NOT USE IN PRODUCTION.** Profile `dev` creates these demo accounts, all with BCrypt hash of
`Password123!`:

| Role     | Email                       |
|----------|-----------------------------|
| ADMIN    | `admin@bonsaimarket.local`  |
| SELLER   | `seller@bonsaimarket.local` |
| CUSTOMER | `user@bonsaimarket.local`   |

It also creates 5 categories, 1 seller store, 20 products, 1 customer cart with 3 items, 75 orders with 189 items, 12
reviews, and 4 wishlist entries. Order dates span approximately 85 days before first migration. These accounts cannot
log in yet: authentication is a later task.

Run `node scripts/check-dev-seed.mjs` to check seed arithmetic without MySQL. After starting MySQL, run the queries in
`db/verify_dev_seed.sql` to verify actual row counts and order totals.

## Build

```text
./mvnw clean test
cd frontend
npm run build
```

On Windows use `.\mvnw.cmd clean test`. The Spring smoke test excludes database auto configuration so `clean test` can
run without MySQL; application startup is the check for Flyway and JPA validation. Public catalog reads exist. Login,
checkout, payment, admin and seller write workflows are intentionally absent.

## AI chatbot MVP

Marketplace mounts `AIExpertChat.vue`, using `POST /api/ai/chat` on Spring and the server-side Google Gemini API only.
Set `GEMINI_API_KEY`, `GEMINI_MODEL` and `AI_ENABLED=true` in the **Spring process environment**. In IntelliJ IDEA on
Windows use **Run → Edit Configurations → BonsaiMarketApplication → Modify options → Environment variables**;
also set the database variables and `SPRING_PROFILES_ACTIVE=dev`. Docker Compose `.env` is not loaded by IntelliJ.
The default is disabled; missing AI configuration returns safe JSON, without making a provider call.

Create a key in [Google AI Studio](https://aistudio.google.com/apikey). Choose an accessible Gemini model supporting
`generateContent` text and JSON structured output; no model ID is assumed. Use its bare `gemini-...` ID. Keys must stay
in the server environment. Follow [AI_CHATBOT_SETUP.md](AI_CHATBOT_SETUP.md) for complete setup, limits, costs and testing.
FAQ retrieval uses keywords, catalog retrieval uses bounded JPA queries, and catalog answers render facts from server
data after AI selects IDs. Chat is text-only and stateless at the API. Review FAQ content before using it as official advice.
No OpenAI or Groq key is needed. Gemini quotas or costs depend on the model and plan. Full application startup requires
MySQL; mocked offline tests do not verify a live provider or database. See [AI_GEMINI_MIGRATION_REPORT.md](AI_GEMINI_MIGRATION_REPORT.md)
for migration evidence. The historical AI implementation report remains unchanged.
