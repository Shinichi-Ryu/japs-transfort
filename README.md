# Japs Transfort - Distributor/Reseller System

Web system for managing a distributor's products, reseller tiers, and resellers.
Built with Spring Boot, Thymeleaf, and PostgreSQL.

## Status
Phase 1 (admin side): login and roles, Categories, Products, Tiers, Resellers.
Later phases: orders and stock, payments and commissions, reports, online deployment.

## Requirements
- Java 25 (or 17+)
- PostgreSQL 18 (16+ works)
- Git
- Maven is included through the wrapper (`mvnw`, `mvnw.cmd`)

## 1. Set up the database (once)
Open PowerShell and run (enter your `postgres` password when asked):

```
psql -U postgres -c "CREATE DATABASE distributor_db;"
psql -U postgres -d distributor_db -f Database\schema.sql
```

Run `schema.sql` only once. Running it again fails because the tables already exist.

## 2. Set environment variables
Secrets are never stored in the code. Set them in the same PowerShell window you run the app from:

```
$env:DB_PASSWORD="your postgres password"
$env:ADMIN_SEED_PASSWORD="password for the first admin login"
```

- `DB_PASSWORD` is needed every time you run the app.
- `ADMIN_SEED_PASSWORD` is only used the first time, to create the admin account.
  Once the admin exists it is not needed again.
- Check `src/main/resources/application-dev.properties` for the database URL and
  user (the dev profile connects to the local `distributor_db`).

## 3. Run the app
```
.\mvnw.cmd spring-boot:run
```
Wait for `Started JapsTransfortApplication`, then open http://localhost:8080

Stop the app with Ctrl+C.

## 4. First login
- Email: `admin@japstransfort.com`
- Password: the value you set in `ADMIN_SEED_PASSWORD`

## Admin pages
| Page | URL |
|---|---|
| Dashboard | /dashboard |
| Categories | /admin/categories |
| Products | /admin/products |
| Reseller Tiers | /admin/tiers |
| Resellers | /admin/resellers |

## Project structure
```
src/main/java/com/japstransfort/
  config/       security, data seeder
  controller/   web controllers (MVC)
  dto/          form objects with validation
  entity/       database entities
  repository/   Spring Data repositories
  service/      business logic
src/main/resources/
  templates/    Thymeleaf pages
  static/       CSS
  application*.properties   dev and prod configuration
Database/schema.sql         database schema
```

## Rules used in this project
- Business logic stays in the service layer.
- Money uses `BigDecimal`.
- Hibernate only validates the schema (`ddl-auto=validate`); it never changes it.
- Secrets come from environment variables, never from the code.
- Records with history are deactivated, not deleted, where the table supports it.

## Deploying online (Phase 5, planned)
Hosted PostgreSQL plus a hosted app, using the `prod` profile and environment
variables on the host. Details will be added in that phase.
