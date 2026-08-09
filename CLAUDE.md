# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 3.3 / Java 17 backend for a multi-vendor ecommerce platform. Stateless JWT auth, Postgres via Flyway-managed schema, Stripe for payments. There is no test suite, no frontend, and no README in this repo currently — this is the API only (the sibling frontend is not part of this working directory).

## Commands

```bash
# Build (skip tests, since there are none yet)
mvn -B package -DskipTests

# Run locally (needs Postgres reachable at the datasource URL in application.yml)
mvn spring-boot:run

# Run a single test class / method (once tests exist)
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName

# Docker build (multi-stage: maven build -> jre runtime image)
docker build -t ecommerce-backend .
```

Postgres must be running locally at `jdbc:postgresql://localhost:5432/ecommerce` (db/user/pass all `ecommerce` by default, see `application.yml`). Flyway runs automatically on startup and owns the schema; Hibernate is `ddl-auto: validate` only — **never** rely on Hibernate to create/alter tables. Any schema change must be a new versioned migration in `src/main/resources/db/migration/` (e.g. `V2__*.sql`), not an edit to `V1__init_schema.sql`.

Key env vars (all have dev-only defaults in `application.yml`, override in real deployments): `JWT_SECRET`, `CORS_ORIGIN`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`.

## Architecture

Standard layered Spring MVC: `controller` → `service` → `repository` (Spring Data JPA) → Postgres. DTOs (Java records) are used for all request/response bodies — entities are never returned directly from controllers.

**Roles & routing.** Three roles: `ADMIN`, `VENDOR`, `CUSTOMER` (see `entity/Role.java`). `SecurityConfig` maps role to URL prefix: `/api/admin/**` requires `ROLE_ADMIN`, `/api/vendor/**` requires `ROLE_VENDOR`, `/api/cart|orders|payments/**` require any authenticated user, `/api/auth/**` and `GET /api/products|categories/**` are public. When adding a new endpoint, put it under the right prefix rather than adding ad-hoc `@PreAuthorize` checks — the URL-based rule is the source of truth.

**Auth flow.** `JwtAuthFilter` runs once per request, reads `Authorization: Bearer <token>`, and populates `SecurityContextHolder` with the email as principal and `ROLE_<role>` as the sole authority (no DB lookup in the filter itself — trusts the JWT claims). `CurrentUserService.getCurrentUser()` is the standard way for controllers/services to resolve the actual `User` entity from that context; use it instead of re-parsing the JWT or the security context directly.

**Vendor ownership pattern.** Vendors don't get a distinct set of REST resources per action — instead, vendor-only controllers (`VendorProductController`, `VendorOrderController`) look up the caller's `Vendor` row via `vendorRepository.findByUserId(userId)` and then verify the target entity belongs to that vendor before allowing mutation (see `ProductService.updateForVendor` / `deleteForVendor`). New vendor-scoped features should follow this same "load vendor for current user, then check ownership" check in the service layer, not the controller.

**Vendor approval gate.** New vendor accounts are created with `approved = false` (`AuthService.register`). An unapproved vendor can log in but `ProductService.createForVendor` blocks listing products until an admin hits `POST /api/admin/vendors/{id}/approve`. Keep this gate in mind for any new vendor-facing write path.

**Checkout / order / payment flow** (the core business transaction, spans `CartService` → `OrderService` → `PaymentService`):
1. `OrderService.checkout` (`@Transactional`) turns the user's `Cart` into a `PENDING` `Order`, decrements `Product.stock` per line item, snapshots `priceCentsAtPurchase` onto each `OrderItem` (so later price changes don't affect past orders), and clears the cart. Throws if stock is insufficient or cart is empty.
2. `PaymentService.createPaymentIntent` creates a Stripe `PaymentIntent` for a `PENDING` order and persists a `Payment` row keyed by `stripePaymentIntentId`.
3. Stripe calls back to `POST /api/payments/webhook` (public route, secured by verifying the `Stripe-Signature` header against the raw request body in `PaymentService.processWebhookEvent`, not JWT). On `payment_intent.succeeded` the order flips to `PAID`; on failure the order stays `PENDING` (stock was already decremented at checkout and is intentionally not restored, so a failed payment just means the customer retries) — see comments in `PaymentService.markFailed`.

When touching this flow, preserve the ordering: stock reservation happens at checkout time (not at payment time), and price/vendor are snapshotted onto `OrderItem` at order-creation time.

**Multi-vendor order splitting.** A single `Order` can contain items from multiple vendors; `OrderItem.vendor` records which vendor each line belongs to so a vendor can query/fulfill just their own items within someone else's order (see `idx_order_items_vendor` and `VendorOrderController`).

**Error handling.** Business-rule failures use `ApiException(HttpStatus, message)`, caught centrally by `GlobalExceptionHandler` and rendered as a consistent JSON error body (`timestamp`, `status`, `error`, `message`). Throw `ApiException` from services (not controllers) with the appropriate `HttpStatus` rather than introducing new exception types or handling errors ad hoc in controllers.

**Entities** use Lombok (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`) — don't hand-write boilerplate accessors/builders. Money is always stored as `*_cents` (`long`), never floating point.