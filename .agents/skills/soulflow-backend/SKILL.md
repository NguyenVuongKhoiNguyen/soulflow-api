---
name: soulflow-backend
description: >-
  Use this skill to understand the stack, architecture, database schemas, and conventions of the FlowerShop backend Spring Boot application. Read this skill before making any backend changes.
---

# Backend Project Skill

## Stack

* **Language**: Java 17
* **Framework**: Spring Boot 3.3.5
* **Build System**: Maven (wrapper: `./mvnw`)
* **Database**: SQL Server
* **ORM/Data Access**: Spring Data JPA
* **Migrations**: Flyway
* **Mapping**: MapStruct 1.5.5
* **Security**: Spring Security + JJWT 0.11.5
* **Other**: Redis (Caching), MinIO (Image Storage), Spring AI (Ollama), WebSocket, Mail

## Architecture

```text
HTTP request
 -> Controller (extends BaseService)
 -> Service (Impl implements Interface)
 -> Repository (Spring Data JPA)
 -> Database
```

## Repository Map

```text
src/main/java/com/poly/
├── config/         Application and Security configurations
├── controllers/    HTTP endpoints (inherit from BaseService)
├── exceptions/     Global exception handling and custom exceptions
├── models/
│   ├── entities/   Database entities
│   ├── enums/      Enumerations
│   ├── mappers/    MapStruct mappers
│   ├── repositories/ Spring Data JPA interfaces
│   ├── requests/   Request DTOs
│   ├── responses/  Response DTOs
│   └── services/   Business logic interfaces and `impl/` implementations
```

## Entry Points and Configuration

* **Application Entry**: `src/main/java/com/poly/FlowerShopApplication.java`
* **Configuration**: `src/main/resources/application.properties`
* **Security Config**: `src/main/java/com/poly/config/SecurityConfig.java` & `JwtFilter.java`
* **Migrations**: `src/main/resources/db/migration/`

## API Conventions

* Controllers map specific paths (e.g. `/admin`, `/user`, `/product`).
* Requests and Responses use dedicated DTOs (e.g., `ProductRequest`, `ProductResponse`).
* Paginating endpoints return a wrapped `PageResponse<T>` and accept standard params (`keyword`, `pageNumber`, `pageSize`, `sortOrder`, etc.).
* Standard Spring `@RestController` pattern using `@PostMapping`, `@GetMapping`, etc.
* Cross-cutting validation is defined but controllers do not typically use `@Valid`.
* `AdminController` endpoints generally consume and produce JSON, with exceptions for multipart file uploads.

## Service Layer

* **Unusual Convention**: Controllers extend `BaseService`. `BaseService` is an abstract class with `@Autowired` fields for ALL services. This allows controllers to call any service directly without constructor injection (e.g. `productService.save(...)`).
* Service interfaces are in `models/services/` and implementations are in `models/services/impl/`.
* Heavy use of Spring Caching annotations (`@Cacheable`, `@CachePut`, `@Caching`, `@CacheEvict`) in service implementations.

## Persistence

* Repositories extend `JpaRepository<Entity, Long>`.
* **Soft Delete**: Entities have a `Boolean deleted` column mapped to `del_if`. Repositories use a custom JPQL `@Modifying` query to soft delete: `UPDATE Entity e SET e.deleted = true WHERE e.id = :id`.
* Dynamic queries use multiline JPQL texts with null checks (`:keyword IS NULL OR ...`).
* Pre-pagination checks run as separate modifying queries (e.g., `checkAndExpireBeforePagination`) to update expired statuses before fetching rows.

## Database

* **Engine**: SQL Server
* **Schema & Migrations**: Managed by Flyway (`src/main/resources/db/migration/V1__create_initial_schema.sql`). 
* Look at Flyway migrations for exact table structures; JPA `ddl-auto` is set to `none`.

## Authentication and Authorization

```text
login endpoint (implicitly handled/configured outside standard flow or via custom service)
 -> JwtFilter extracts Bearer token
 -> JwtUtil validates token and extracts username + roles
 -> SecurityContextHolder populated with SimpleGrantedAuthority
 -> SecurityConfig applies authorization rules
```

* Endpoints under `/admin/**` require `ADMIN` role.
* Endpoints under `/user/**` require `USER` role.
* Most other paths (`/product/**`, `/images/**`, `/ws/**`) are `permitAll()`.
* Stateless session policy. Password hashing via BCrypt.

## Error Handling

* Managed globally by `GlobalExceptionHandler` (`@RestControllerAdvice`).
* `EntityNotFoundException` -> 404 Not Found.
* `BusinessException` -> 422 Unprocessable Entity.
* `ForbiddenException` -> 403 Forbidden.
* Validation and Binding exceptions -> 400 Bad Request with field error mapping.

## Common Change Patterns

```text
New CRUD feature:
1. Entity (with `del_if` boolean for soft delete) -> Repository
2. Request/Response DTOs in `models/requests/` and `models/responses/`
3. MapStruct Mapper (abstract class extending `@Mapper`, fetching dependencies if needed in `@AfterMapping`)
4. Service interface and Impl (with `@Cacheable`/`@CacheEvict`)
5. Controller (injects via inheriting `BaseService` if you add the new service to `BaseService`, or directly)

Database change:
1. Update Flyway migration (add a `V2__...sql` file).
2. Update Entity and Repository/Service to reflect the change.
```

## Reusable Code

* `BaseService`: Base class for Controllers to access all services.
* `GlobalExceptionHandler`: Centralized error formats.
* `PageResponse<T>`: Standard pagination wrapper.
* MapStruct mappers (`toBasicResponse`, `toDetailResponse`) and `@AfterMapping` logic.

## Development Commands

* **Run app**: `./mvnw spring-boot:run`
* **Test**: `./mvnw clean test`
* **Docker Environment**: `docker-compose up -d` brings up the entire stack (SQL Server, Redis, MinIO, Ollama, API). 
* **Database Init**: Docker compose auto-initializes the SQL Server database (`flower_shop`) and Ollama models.

## Known Gotchas

* Controllers *do not* inject services via constructor. Instead, they extend `BaseService`. If you create a new service, you should add an `@Autowired` field for it in `BaseService`.
* Mappers are `abstract class`es, not interfaces, so they can `@Autowired` repositories to fetch related entities in `@AfterMapping` methods (e.g. recovering the `deleted` flag for existing entities).
* Entities often use a `del_if` column mapped to `Boolean deleted;` for soft deletion, but the `hibernate` `@SQLDelete` and `@Where` annotations are not used. Instead, soft deletion and filtering is handled manually in JPQL queries.

## Agent Rules

* Read this file before exploring backend code.
* Follow the `BaseService` inheritance pattern for Controllers.
* Use MapStruct abstract classes and `@AfterMapping` to populate specific fields.
* Follow the existing multiline JPQL query pattern for repository methods.
* Apply caching annotations appropriately to new service methods.
* Make minimal localized changes and reuse existing DTOs and utilities.
* Data seeding is done via Java `ApplicationRunner` classes in the `config` folder, not via Flyway SQL scripts.
* Never expose or hardcode secrets.

