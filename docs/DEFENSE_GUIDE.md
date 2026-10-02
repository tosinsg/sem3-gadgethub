# GadgetHub – Defense & Presentation Guide

## 1. 30-second pitch
"GadgetHub is a Spring Boot REST backend for selling gadgets and accessories. Customers browse products, place orders, and cancel them. The key logic is in the service layer: stock is checked and deducted atomically when an order is placed, and restored on cancellation."

## 2. Architecture (draw this on slide 2)
Client → **Controller** (HTTP in/out) → **Service** (business rules, transactions) → **Repository** (database) → **H2**
Rule: controllers never touch repositories; every @GetMapping/@PostMapping calls a service.

## 3. Defense points (why I built it this way)
1. **Thin controllers, smart services** – controllers only map HTTP; all logic lives in services, so it is testable and reusable.
2. **@Transactional order placement** – order + stock changes succeed or fail together. If item 3 is out of stock, items 1 and 2 are rolled back.
3. **Price snapshot** – OrderItem stores `unitPrice`, so later price edits never change old orders.
4. **BigDecimal for money** – double causes rounding errors (0.1 + 0.2).
5. **DTOs for orders** – returns a clean response, avoids infinite JSON loops and lazy-loading problems.
6. **Validation + global exception handler** – bad input → 400, missing → 404, business conflict → 409, always one consistent JSON shape.
7. **Optimistic locking (@Version)** – two buyers grabbing the last item at the same time: one wins, the other gets a 409 instead of overselling.
8. **Order status rules** – only PLACED orders can be shipped or cancelled; a shipped order can't be cancelled.
9. **Data protection** – a product with order history can't be deleted; duplicate emails are rejected.
10. **Two-level testing** – unit tests (Mockito) check logic in isolation; integration tests (MockMvc + H2) check the whole HTTP→DB flow.
11. **H2 in-memory** – zero setup for demo; switching to MySQL = change datasource URL + driver in application.properties/pom.xml.
12. **Constructor injection** – no @Autowired fields; dependencies are explicit and easy to mock.

## 4. Presentation script (keep it clean – 5 steps, no clutter)
**Step 1 – Intro (30s):** pitch above + architecture slide.

**Step 2 – Show Spring Boot code (2 min).** Open in this order, say one sentence each:
- `ProductController` → "thin, just maps URLs to the service"
- `ProductService` → "findAll filters, delete is protected"
- `OrderController` → "POST /api/orders"
- `OrderService.placeOrder` → **the star**: check stock → deduct → total → save, all in one transaction
- `GlobalExceptionHandler` → "clean error responses"

**Step 3 – Package the JAR (1 min).**
```
mvn clean install
```
Point at: `BUILD SUCCESS` and `target/gadgethub-1.0.0.jar`. Then `java -jar target/gadgethub-1.0.0.jar` and open `/api/products` in the browser.

**Step 4 – Tests (1 min).** Show the output lines: `Tests run: 15, Failures: 0, Errors: 0`. Say: "4 order-logic unit tests, 4 product unit tests, 7 end-to-end API tests." Highlight `placingOrderReducesStock_andCancelRestoresIt` and `insufficientStock_returns409`.

**Step 5 – GitHub (1 min).** Show the repo, the commit history, README, and that `src/test` is pushed.
```
git init
git add .
git commit -m "GadgetHub: Spring Boot backend with services, tests, packaging"
git branch -M main
git remote add origin https://github.com/<your-username>/gadgethub.git
git push -u origin main
```
Tip: make several small commits (model, services, controllers, tests) so history looks real and progressive.

## 5. Likely questions & answers
- **Why services if controllers can call repositories?** Separation of concerns: HTTP handling vs business rules. Services are reusable and unit-testable without a web server.
- **What does @Transactional do here?** Wraps the method in one DB transaction; a RuntimeException rolls everything back (stock and order).
- **What happens if stock is insufficient?** `BusinessRuleException` → HTTP 409 with a clear message; nothing is saved.
- **Why 409 and not 400?** The request is well-formed but conflicts with current state (stock). 400 is for malformed/invalid input.
- **How do you stop overselling under concurrency?** `@Version` optimistic locking on Product; the loser gets a 409 and retries.
- **Why DTOs?** Control what the API exposes, avoid entity cycles and lazy-loading errors.
- **Why H2? Is this production-ready?** H2 is for development/demo. Production = MySQL/PostgreSQL by changing config; code stays the same thanks to JPA.
- **How is `mvn package` different from `mvn clean install`?** `package` builds the JAR in `target/`; `install` also copies it into your local Maven repo. `clean` deletes `target/` first. Both run tests.
- **What's an "executable JAR"?** Spring Boot's Maven plugin bundles dependencies + embedded Tomcat, so `java -jar` is all you need.
- **What would you add next?** Spring Security + JWT login, payment integration, pagination, product images, MySQL, Docker.
- **Unit vs integration test?** Unit = one class with mocked dependencies (fast). Integration = real Spring context, real H2, real HTTP calls via MockMvc.
- **How does the total get calculated?** Sum of `unitPrice × quantity` per line using BigDecimal, inside `OrderService.placeOrder`.

## 6. Voice-note script (~40 seconds)
"Hi, quick update on GadgetHub. The backend is done in Spring Boot: controllers for products, customers and orders, each calling a service layer where the real logic lives. Placing an order checks stock, deducts it, and calculates the total in a single transaction, and cancelling restores the stock. I've packaged the JAR with mvn clean install, 15 tests pass, and everything is pushed to GitHub. Link is below. Happy to answer any questions."

## 7. Slide outline (max 6 slides)
1. Title + one-line pitch  2. Architecture diagram  3. Key features / endpoints  4. Order logic flow (the transaction)  5. Testing + build results (screenshots)  6. GitHub + future improvements
