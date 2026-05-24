# AGENTS.md

Guidance for coding agents (Kiro, Claude Code, Codex, Cursor, …) working on this project. Read this carefully before changing any code.

## 1. Project overview

- **Name:** `fashion-auth` (`com.fashion:fashion-auth:1.0.0`)
- **Purpose:** Core Monolith Backend for the Fashion Marketplace system. Handles Authentication, User Management, Products, Shops, Orders, Reviews, and Notifications.
- **Type:** Pure REST API (no view rendering), JSON in/out.
- **Frontend:** A separate app, defaults to `http://localhost:3000` (configured via `app.frontend-url`).

## 2. Tech stack & dependencies

| Area | Technology / Artifact | Version | Notes |
|---|---|---|---|
| Language | Java | 17 | Set via `<java.version>` in pom |
| Framework | `spring-boot-starter-parent` | 3.2.3 | Parent POM, manages all Spring Boot dependency versions |
| Web | `spring-boot-starter-web` | (managed) | Embedded Tomcat, REST controllers |
| Security | `spring-boot-starter-security` | (managed) | Filter chain, BCrypt (strength 12) |
| OAuth2 | `spring-boot-starter-oauth2-client` | (managed) | Google + Facebook login |
| Persistence | `spring-boot-starter-data-jpa` | (managed) | Hibernate ORM, Spring Data repositories |
| Database driver | `com.mysql:mysql-connector-j` | (managed, runtime) | MySQL 8 connector |
| Mail | `spring-boot-starter-mail` | (managed) | Gmail SMTP (TLS 587) for OTP emails |
| Validation | `spring-boot-starter-validation` | (managed) | Jakarta Bean Validation (`@NotBlank`, `@Email`, etc.) |
| JWT | `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 0.11.5 | HS256 signing, runtime-scoped impl + jackson |
| Boilerplate | `org.projectlombok:lombok` | (managed, optional) | `@Data`, `@Builder`, `@Getter/@Setter`, etc. |
| DevTools | `spring-boot-devtools` | (managed, runtime, optional) | Hot reload in dev |
| Test | `spring-boot-starter-test` | (managed, test scope) | JUnit 5, Mockito, AssertJ, Spring MockMvc |

**Important:** Do not add new dependencies without asking the user first. If a task seems to require a library not listed above (e.g. Redis, MapStruct, Swagger), propose it and wait for approval.

## 3. Directory layout

```
src/main/java/com/fashion/auth/
├── FashionAuthApplication.java      # Entry point
├── config/
│   └── SecurityConfig.java          # Security filter chain, CORS, OAuth2 login, BCrypt
├── controller/
│   ├── AdminToolController.java     # /api/admin/tools/** 
│   ├── AdminUserController.java     # /api/admin/users/** (admin CRUD)
│   ├── AuthController.java          # /api/auth/** (login, register, OTP)
│   ├── CategoryController.java      # /api/categories/**
│   ├── NotificationController.java  # /api/notifications/**
│   ├── OrderController.java         # /api/orders/** (buyer & seller order flow)
│   ├── ProductController.java       # /api/products/**
│   ├── ShopController.java          # /api/shops/**
│   └── UserController.java          # /api/user/** (profile, change-password)
├── dto/
│   ├── AuthDto.java                 # Auth request/response (nested static classes)
│   ├── OrderDto.java, ProductDto.java, ShopDto.java, etc.
│   └── admin/                       # Admin DTOs
├── exception/
│   └── AdminAccessException.java    # Admin authorization error (carries HttpStatus)
├── model/
│   ├── User.java, Shop.java, Category.java
│   ├── Product.java, ProductVariant.java, ProductImage.java, Review.java, ReviewReply.java
│   ├── Order.java, OrderItem.java, OrderStatusHistory.java, CartItem.java, Payment.java
│   ├── ShopPoint.java, ShopRanking.java, PointRule.java, Notification.java, OtpEntry.java
│   └── PostalCode.java
├── repository/
│   └── UserRepository.java, ProductRepository.java, OrderRepository.java, ...
├── security/
│   ├── JwtUtils.java                # Generate/parse/validate JWT
│   ├── JwtAuthFilter.java           # Filter that sets SecurityContext from Bearer token
│   └── OAuth2SuccessHandler.java    # Post-OAuth2 success handler, redirects back to frontend
└── service/
    ├── AuthService.java, OtpService.java
    ├── ProductService.java, OrderService.java, ShopService.java, NotificationService.java
    └── ...

src/main/resources/application.properties
Makefile
pom.xml
.env.example
```

## 4. Build & run commands

There is no Maven Wrapper (`mvnw`) — the project uses the system-installed `mvn`.

```bash
# Compile only (fast check after edits)
mvn -q -DskipTests compile

# Build (compile + test + package)
mvn clean package

# Run the app (dev)
mvn spring-boot:run
# or
make run

# Run in debug mode (JDWP listens on port 5005, suspend=y)
make debug

# Run tests only
mvn test
```

The app listens on `http://localhost:8080`. On first start, Hibernate will auto-create/update the MySQL schema based on the entities (`ddl-auto=update`).

> **Long-running:** `mvn spring-boot:run` and `make debug` are foreground processes — **do not** run them via the regular `executeBash` tool. Use `controlBashProcess` (action `start`) or ask the user to run them manually.

### Prerequisites to run locally
1. Java 17+ installed and on `PATH`
2. Maven 3.8+ installed and on `PATH`
3. MySQL 8 running with database `fashion_marketplace` created
4. `.env` file at project root (copy from `.env.example` and fill in values)

## 5. Environment configuration

The app reads environment variables from a `.env` file at the repo root (loaded via `spring.config.import=optional:file:.env[.properties]`). See `.env.example`. Important variables:

- **DB:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — requires a running MySQL with the `fashion_marketplace` database created.
- **Mail (OTP):** `MAIL_USERNAME`, `MAIL_PASSWORD` — use a Gmail App Password.
- **JWT:** `JWT_SECRET` (≥ 32 chars for HS256), `JWT_EXPIRATION_MS` (default 86400000 = 24h).
- **OAuth2:** `GOOGLE_CLIENT_ID/SECRET/REDIRECT_URI`, `FACEBOOK_CLIENT_ID/SECRET/REDIRECT_URI`.
- **Frontend:** `FRONTEND_URL` (default `http://localhost:3000`) — used for CORS allow-origin and OAuth2 redirect.
- **OTP:** `OTP_TTL_MINUTES` (default 10).

> **Never commit** `.env`, never log secrets, and never echo these values in tool responses. `.env` is already in `.gitignore`.

## 6. API endpoints

All requests/responses are JSON. Endpoints under `/api/auth/**`, `/login/oauth2/**`, `/oauth2/**`, and `/error` are **public**; everything else requires `Authorization: Bearer <jwt>`.

### Public — `/api/auth`
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Log in with `username` (either email **or** fullName) + `password` |
| POST | `/api/auth/register` | Register (requires an OTP that was sent first) |
| POST | `/api/auth/send-otp` | Generate and email a 6-digit OTP |
| POST | `/api/auth/verify-otp` | Check an OTP **without** consuming it |

### User — `/api/user`
| Method | Path | Description |
|---|---|---|
| GET  | `/api/user/profile` | Read profile from JWT |
| PUT  | `/api/user/profile` | Update fullName / phone / avatarUrl |
| POST | `/api/user/change-password` | Change password (old + new + confirm) |

### Products, Shops & Orders (Main Business APIs)
| Prefix | Description |
|---|---|
| `/api/products` | Manage products (CRUD for sellers), listing, search, and batch retrieval. |
| `/api/shops` | Manage shop info, seller registration, and shop rankings. |
| `/api/orders` | Place orders, buyer order history, seller order management (confirm, ship, deliver). |
| `/api/categories` | Product categories retrieval. |
| `/api/notifications` | User notifications. |

### Admin — `/api/admin/users` (`admin` role only)
| Method | Path | Description |
|---|---|---|
| GET    | `/api/admin/users?q=&role=&isActive=` | List + filter |
| GET    | `/api/admin/users/{id}` | Detail |
| POST   | `/api/admin/users` | Create a user |
| PUT    | `/api/admin/users/{id}` | Update |
| PATCH  | `/api/admin/users/{id}/status` | Activate/deactivate |
| DELETE | `/api/admin/users/{id}` | Delete |

An admin **cannot self-demote, self-deactivate, or self-delete** the currently logged-in admin (enforced in `AdminUserController`).

### OAuth2
- Start the flow: `GET /oauth2/authorization/google` or `/oauth2/authorization/facebook`.
- On success, the server redirects to `${FRONTEND_URL}/oauth2/redirect?token=<jwt>`.
- On failure: redirects with `?error=oauth2_failed` (or `email_not_provided`).

## 7. Code conventions

- **Packages:** put new classes under `com.fashion.auth.<layer>` matching the right layer (controller / service / repository / model / dto / security / config / exception).
- **Lombok:** use `@Data` for DTOs, `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder` for entities. Do **not** drop `@Builder.Default` from fields with default values — they will be lost when built via the builder.
- **Validation:** use `jakarta.validation` annotations on DTOs (`@NotBlank`, `@Email`, `@Size`, …) plus `@Valid` on controllers.
- **Error responses:** return `ResponseEntity` with a `MessageResponse(message)` body. Common statuses: 400 for business errors, 401/403 for admin errors (via `AdminAccessException`).
- **End-user messages:** user-facing messages are written in **Vietnamese** (see existing controllers) to stay consistent with the rest of the system.
- **Logging:** use SLF4J (`LoggerFactory.getLogger`). **Never** log passwords, JWTs, OTPs, or `passwordHash`.
- **Time:** use `LocalDateTime.now()` for `createdAt`/`updatedAt`. `User.@PreUpdate` already maintains `updatedAt`, but several controllers still set it manually — keep the existing pattern when editing.
- **IDs:** `User.id` is a UUID string (`@UuidGenerator`, `char(36)`). Do not generate IDs anywhere else.
- **OAuth2 marker:** users created via OAuth2 have a `passwordHash` starting with `"{oauth2}"` — `AuthService.login` uses this prefix to block password login for OAuth accounts.

## 8. Security notes when editing

- **CORS:** `SecurityConfig` only allows `frontendUrl`. A few controllers still carry `@CrossOrigin(origins = "*")` at the class level — that's the current pattern, but **do not** introduce more wildcard origins; prefer the centralized config in `SecurityConfig`.
- **Passwords:** always go through `passwordEncoder.encode(...)` (BCrypt 12). Never return `passwordHash` in a response.
- **JWT:** never log tokens. `JwtUtils` signs with HS256; if you change the algorithm, update the filter too.
- **Admin:** every admin endpoint **must** start with `requireAdmin(token)`. When adding a new admin endpoint, copy the pattern from `AdminUserController` (check header → validate token → load user → check `isActive` → check `role == admin`).
- **OTP store:** currently a `ConcurrentHashMap` in `OtpService` — **lost on restart**, **does not scale across instances**. Don't assume OTPs survive a restart or instance switch; if you need persistence, propose Redis first before changing it.
- **Mail:** SMTP credentials come from env. Don't actually send mail from unit tests (stub `JavaMailSender`).

## 9. Database

- The schema is auto-generated by Hibernate (`ddl-auto=update`), but the actual source of truth for the legacy structure is `FinalGraduateDB.sql`. 
- **Do not** switch it to `create`/`create-drop` without asking the user — it will wipe dev data.
- Main table: `users` (see `User.java`).
- When adding a field to an entity, consider `nullable`, `length`, and any indexes needed for new queries.

## 10. Testing

- There is currently **no** `src/test/java` directory. When adding a feature or fixing a bug, write tests with JUnit 5 + Spring Boot Test.
- Available test utilities (from `spring-boot-starter-test`): JUnit 5, Mockito, AssertJ, Spring MockMvc, `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`.
- Mock `JavaMailSender` and `UserRepository` for service-layer unit tests.
- Use `@WebMvcTest` for controller-layer tests (no DB needed).
- Run: `mvn test`.
- **Do not** add additional test frameworks (TestNG, Spock, etc.) without asking the user.

## 11. Recommended agent workflow

1. **Read before editing.** Existing controllers/services include older versions kept as comments — that's history, not dead code to clean up. Ask the user before removing them.
2. **Respect existing patterns** (manual Bearer token checks in `UserController`/`AdminUserController`, Vietnamese messages, `MessageResponse`, `ResponseEntity`, …). Don't refactor to a different pattern (e.g. `@AuthenticationPrincipal`, `@ControllerAdvice`) unless the user asks.
3. **After every code change:** run `mvn -q -DskipTests compile` to make sure the build still passes. If the change touches business logic, run `mvn test`.
4. **Don't create spec/markdown files** unless explicitly requested.
5. **Don't commit on the user's behalf.** Only commit when asked, and never commit `.env`.
6. **Don't add new dependencies** to `pom.xml` without user approval. Work with what's already available.
7. **Don't introduce new design patterns** (e.g. `@ControllerAdvice`, global exception handler, MapStruct mappers) unless the user explicitly requests it.

## 12. Known caveats / technical debt

- **Monolith Complexity:** The project has scaled significantly beyond its initial scope as an Auth Service. Code now handles extensive e-commerce domains, which may lead to bloated packages.
- **JSON Column Mappings:** Certain columns (e.g., `productSnapshot` in `OrderItem.java`) are stored as JSON strings in the database. When mutating these fields, use `ObjectMapper` to correctly serialize and deserialize the data.
- **Database Triggers:** The MySQL schema relies on background triggers (e.g., `trg_order_delivered` to update shop sales, `trg_update_shop_rating` to recalculate shop points). Java service logic must not duplicate or conflict with these database-level computations.
- Bearer token validation logic is duplicated in controllers — could be extracted into a filter or aspect.
- `OtpService` keeps state in memory → not suitable for multi-instance production.
- `AdminUserController.requireAdmin` parses the JWT manually instead of using `@PreAuthorize("hasRole('admin')")`.
- Some error responses leak internal `e.getMessage()` directly — be careful when changing exception flows.
- No `README.md`, no CI config, no Dockerfile yet.

When touching any of the items above, ask the user before refactoring on your own.
