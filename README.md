# Course Timetable Planner API

[![CI](https://github.com/YOUR_USERNAME/course-timetable-planner/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/course-timetable-planner/actions/workflows/ci.yml)

A Spring Boot REST API **and** a server-rendered web UI for scheduling
courses, instructors, and rooms — with automatic conflict detection so
no instructor or room is ever double-booked, and role-based access so
only a coordinator can change anything. Modelled directly on the
timetabling work of an academic coordinator managing multiple
departments.

## The core idea

Everything exists in service of one rule: **an instructor or a room
can never have two schedule entries on the same day with overlapping
times.** `POST /api/schedule-entries` (and the "Add to schedule" form
in the web UI) run that check before saving anything and return
`409 Conflict` — with the exact entries it clashed with — the moment a
slot would double-book someone.

On top of that: **only a coordinator can create, update, or delete**
anything. An instructor can log in and see the full timetable, but
every mutating action is blocked with a clear "access denied," both in
the API (403 JSON) and the web UI (a proper error page, and the
create/delete controls simply don't render for that role).

## Features

- Full CRUD for departments, instructors, rooms, and courses
- Schedule entries (course + instructor + room + day + time) with
  automatic conflict detection on both create and update
- A dry-run `POST /api/schedule-entries/check-conflicts` endpoint
- Filtered, paginated, sorted listing of schedule entries
- **Role-based access control**: COORDINATOR (full access) vs.
  INSTRUCTOR (view-only), enforced with `@PreAuthorize` at the
  **service layer** — so the same rule applies identically whether the
  request came through the REST API or the web UI, not duplicated
  logic in two places
- **A Thymeleaf web UI**: login page, a real weekly calendar grid (day
  columns × hourly rows, not just a flat table) with an add-entry
  form, and list/create pages for departments, instructors, rooms,
  and courses — coordinator-only controls simply don't render for an
  instructor, and are also blocked server-side if attempted directly
- Both session-based form login (for the browser) and HTTP Basic (for
  curl/Postman/Swagger) work on the same endpoints
- Centralized exception handling: 404, 409, 400, and 403 all map to
  clean, structured responses
- OpenAPI/Swagger UI documentation, with a configured Basic Auth
  "Authorize" button
- Two Spring profiles: `dev` (H2 in-memory, auto-seeded) for zero-setup
  local runs, and the default (PostgreSQL) for Docker/production
- Dockerfile + docker-compose for a real Postgres deployment
- JUnit 5 tests at three levels: a pure unit test of the overlap
  algorithm, a pure unit test of the calendar-grid-building logic (8
  tests, zero Spring dependency), a Mockito-based unit test of the
  conflict-detection service, and a full-stack MockMvc integration
  test — including a test that specifically proves an instructor gets
  403 on write attempts

## Tech stack

Java 17 · Spring Boot 3 · Spring Security 6 · Spring Data JPA · Thymeleaf · PostgreSQL · H2 · springdoc-openapi · Docker · JUnit 5 · Mockito

## How the security model actually works

Two hardcoded in-memory users (see `SecurityConfig`):

| Username      | Password          | Role        | Can do                          |
|---------------|--------------------|-------------|----------------------------------|
| `coordinator` | `coordinator123`  | COORDINATOR | Everything — create/update/delete |
| `instructor`  | `instructor123`   | INSTRUCTOR  | View only                        |

The enforcement that actually matters lives on the **service layer**,
not the controllers:

```java
@PreAuthorize("hasRole('COORDINATOR')")
public Response create(Request request) { ... }
```

Every `create`, `update`, and `delete` method across all five services
carries this annotation. Because both the REST controllers and the
Thymeleaf web controllers call the *same* service methods, the rule is
enforced once and applies everywhere automatically — there's no way to
route around it through a different controller.

`GlobalExceptionHandler` (scoped to the REST `controller` package)
catches the resulting `AccessDeniedException` and returns a clean
`403` JSON body. The Thymeleaf web controllers rely on
`SecurityConfig`'s `.exceptionHandling(...).accessDeniedPage(...)`
instead, redirecting to a proper HTML error page — same underlying
exception, different presentation for API vs. browser clients.

In the templates, `sec:authorize="hasRole('COORDINATOR')"` hides
create/delete controls from instructors entirely — but that's a UX
nicety, not the actual security boundary. Even if someone crafted a
raw POST request bypassing the UI, the `@PreAuthorize` check still
blocks it.

**On the in-memory users:** hardcoding two demo accounts keeps this
project focused on the conflict-detection and access-control logic
rather than user management. A real deployment would replace
`SecurityConfig`'s `UserDetailsService` bean with one backed by a
persisted `User` entity/repository (with a registration flow, password
reset, etc.) — the `@PreAuthorize` rules wouldn't need to change at
all, since they check roles, not how those roles were assigned.

## Project structure

```
course-timetable-planner/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .github/workflows/ci.yml
├── src/main/java/com/portfolio/timetable/
│   ├── TimetableApplication.java
│   ├── model/          Department, Instructor, Room, Course, ScheduleEntry
│   ├── dto/              Request/Response records per entity + error shapes
│   ├── repository/       Spring Data JPA repositories
│   ├── service/           CRUD services (@PreAuthorize here) + ConflictDetectionService
│   ├── controller/        REST controllers (JSON)
│   ├── web/                Thymeleaf MVC controllers (HTML) + CalendarGridBuilder
│   ├── exception/         Custom exceptions + GlobalExceptionHandler (REST only)
│   └── config/             SecurityConfig, OpenApiConfig, dev-profile data seeder
├── src/main/resources/
│   ├── templates/          Thymeleaf pages (login, schedule, departments, ...)
│   ├── static/css/          Shared stylesheet
│   └── application*.yml
└── src/test/java/com/portfolio/timetable/
    ├── model/               pure overlap-logic test
    ├── web/                  pure calendar-grid-logic test (no Spring)
    ├── service/              Mockito-based conflict detection test (bypasses Spring, no security involved)
    └── controller/            full-stack MockMvc test, including role-enforcement
```

## Prerequisites

- JDK 17 or later
- Maven 3.8+
- Docker (optional — only needed for the Postgres/production path)
- VS Code with the **Extension Pack for Java**

## Running it

**Fastest path — no Docker, no Postgres install, sample data included:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Open **http://localhost:8080/login** and sign in as `coordinator` /
`coordinator123` to see the full UI, or `instructor` / `instructor123`
to see the view-only experience. Sample data (2 departments, 2
instructors, 2 rooms, 2 courses, 3 conflict-free schedule entries) is
seeded automatically.

**Run the tests:**

```bash
mvn test
```

**Real Postgres via Docker:**

```bash
docker compose up --build
```

Same login flow, but starts with an empty database — create your own
departments/instructors/rooms/courses first (as coordinator) before a
schedule entry has anything to reference.

## The weekly calendar grid

The `/schedule` page renders a real calendar: day columns (Monday
through Sunday) × hourly rows (08:00–20:00), built by
`CalendarGridBuilder`. Two things worth knowing about how it's built:

- **Two courses can legitimately run at the same time in different
  rooms** — the conflict detector only forbids the same instructor or
  room being double-booked, not the calendar slot itself. So each grid
  cell holds a *list* of entries, not at most one, and cells don't use
  HTML `rowspan` to visually stretch across hours (a rowspan-based
  layout can't cleanly represent two different-duration entries
  overlapping in the same column). Instead, every entry is anchored to
  its starting hour and shows its own exact time range as text inside
  its cell. It's a small trade-off — no visual "this class is 2 hours
  tall" block — in exchange for correctly handling concurrency and
  being something I could actually unit test and verify.
- **Nothing outside 08:00–20:00 is silently dropped.** An entry that
  starts before 08:00 or at/after 20:00 goes into a small fallback
  table below the grid instead, so an unusual time never just
  disappears from the page.

`CalendarGridBuilder` has zero Spring or JPA dependency — it's pure
`java.time` arithmetic over plain DTOs — so unlike most of this
project, I could actually compile and run its test suite for real in
my own environment before handing it to you (8/8 passing,
`CalendarGridBuilderTest`).

## Trying the conflict detection

Log in as `coordinator`, go to **Schedule**, and try adding a slot
that overlaps Dr. Rao's existing Monday 09:00–11:00 entry in room 101.
The page reloads with a red error banner listing exactly what it
clashed with. Change the start time to `11:00` (back-to-back, no
overlap) and it succeeds — that boundary case is deliberate.

Via the API directly:

```bash
curl -u coordinator:coordinator123 -X POST http://localhost:8080/api/schedule-entries \
  -H "Content-Type: application/json" \
  -d '{"courseId": 1, "instructorId": 1, "roomId": 1, "dayOfWeek": "MONDAY", "startTime": "10:00", "endTime": "12:00"}'
```

## Trying the role restriction

Log out and log back in as `instructor` / `instructor123`. The
"Add to schedule" form and every "Delete" button disappear from every
page — but even without the UI, the server itself blocks it:

```bash
curl -u instructor:instructor123 -X POST http://localhost:8080/api/schedule-entries \
  -H "Content-Type: application/json" \
  -d '{"courseId": 1, "instructorId": 1, "roomId": 1, "dayOfWeek": "TUESDAY", "startTime": "09:00", "endTime": "10:00"}'
```

Expect `403 Forbidden`.

## A note on verification

I wasn't able to compile or run the Spring Boot parts of this project
inside my own sandbox — Spring Boot 3 needs `jakarta.*` packages that
aren't available through apt here, and Maven Central isn't reachable
from this environment. I reviewed those files by hand (brace/paren
balance, DTO field order against every call site, Spring Data
method-name-to-query resolution, the Thymeleaf/Spring Security
integration points) but couldn't actually execute them before handing
this to you. **Please run `mvn test` first thing** — if anything
fails, tell me the exact output and I'll fix it. One area worth extra
attention when you test manually: confirm the CSRF-protected forms
(login, logout, every create/delete form) actually submit correctly —
that's the part of this stack I'm least able to verify without a real
browser.

The one exception is `CalendarGridBuilder` (and its test): since it
has zero Spring/JPA dependency, I was able to install a plain JDK,
JUnit 5, and the `jakarta.validation` API via apt, actually compile
the real project files, and run the real test suite for that piece —
8/8 passing, for real, not just reviewed. If the calendar grid doesn't
render correctly, the bug is far more likely in the Thymeleaf template
(`schedule.html`) than in the placement logic itself.

## Possible extensions

- Replace the in-memory users with a persisted `User` entity +
  registration flow
- Flyway migrations instead of `ddl-auto: update`
- A third role, e.g. DEPARTMENT_HEAD, scoped to only their own
  department's data
- Filter the calendar grid by instructor or room (currently shows
  everything at once)
- Recurring exceptions (holidays, one-off room changes)

## License

MIT
