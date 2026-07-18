# Course Timetable Planner API

[![CI](https://github.com/YOUR_USERNAME/course-timetable-planner/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/course-timetable-planner/actions/workflows/ci.yml)

A Spring Boot REST API for scheduling courses, instructors, and rooms
— with automatic conflict detection so no instructor or room is ever
double-booked. Modelled directly on the timetabling work of an
academic coordinator managing multiple departments: juggling course
codes, instructor availability, and room capacity across a term
without double-booking anyone.

## The core idea

Everything in this project exists in service of one rule: **an
instructor or a room can never have two schedule entries on the same
day with overlapping times.** `POST /api/schedule-entries` runs that
check before saving anything and returns `409 Conflict` — with the
exact entries it clashed with — the moment a slot would double-book
someone.

## Features

- Full CRUD for departments, instructors, rooms, and courses
- Schedule entries (course + instructor + room + day + time) with
  automatic conflict detection on both create and update
- A dry-run `POST /api/schedule-entries/check-conflicts` endpoint —
  validate a proposed slot without saving it
- Filtered, paginated, sorted listing of schedule entries
  (`?instructorId=&roomId=&courseId=&dayOfWeek=`)
- Centralized exception handling: 404 for missing resources, 409 for
  scheduling conflicts (with the conflicting entries attached), 400
  for validation failures
- OpenAPI/Swagger UI documentation generated from the code
- Two Spring profiles: `dev` (H2 in-memory, auto-seeded with sample
  data) for a zero-setup local run, and the default (PostgreSQL) for
  Docker/production
- Dockerfile + docker-compose for a real Postgres deployment
- JUnit 5 tests: a pure unit test of the overlap algorithm, a
  Mockito-based unit test of the conflict-detection service, and a
  full-stack MockMvc integration test through real JPA repositories

## Tech stack

Java 17 · Spring Boot 3 · Spring Data JPA · PostgreSQL · H2 · springdoc-openapi · Docker · JUnit 5 · Mockito

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
│   ├── service/           CRUD services + ConflictDetectionService
│   ├── controller/        REST controllers
│   ├── exception/         Custom exceptions + GlobalExceptionHandler
│   └── config/             OpenAPI metadata + dev-profile data seeder
└── src/test/java/com/portfolio/timetable/
    ├── model/               pure overlap-logic test
    ├── service/              Mockito-based conflict detection test
    └── controller/            full-stack MockMvc integration test
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

This starts the app on H2 in-memory storage and seeds 2 departments, 2
instructors, 2 rooms, 2 courses, and 3 conflict-free schedule entries.
Open Swagger UI at **http://localhost:8080/swagger-ui.html** and try
it immediately — including POSTing a slot that clashes with the seeded
data, to see the 409 response.

**Run the tests:**

```bash
mvn test
```

**Real Postgres via Docker:**

```bash
docker compose up --build
```

This builds the app image, starts a Postgres container, and connects
them together. The API is on `http://localhost:8080`, empty (no dev
seeding on this profile) — create your own data through the API.

## Trying the conflict detection

With the `dev` profile running, Dr. Rao (instructor) already has a
Monday 09:00–11:00 slot in room 101. Try creating an overlapping one:

```bash
curl -X POST http://localhost:8080/api/schedule-entries \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1,
    "instructorId": 1,
    "roomId": 1,
    "dayOfWeek": "MONDAY",
    "startTime": "10:00",
    "endTime": "12:00"
  }'
```

Expect `409 Conflict` with a body listing exactly which existing entry
it clashed with. Change `startTime` to `"11:00"` (back-to-back, no
overlap) and it succeeds — the boundary case is deliberate: two slots
that touch but don't overlap are allowed.

To validate a slot without committing to it, use the dry-run endpoint:

```bash
curl -X POST http://localhost:8080/api/schedule-entries/check-conflicts \
  -H "Content-Type: application/json" \
  -d '{ "courseId": 1, "instructorId": 1, "roomId": 1, "dayOfWeek": "MONDAY", "startTime": "10:00", "endTime": "12:00" }'
```

It returns the same conflict list `POST /api/schedule-entries` would
have rejected on, but nothing is saved either way.

## The conflict algorithm

`ScheduleEntry.overlaps(other)` is the whole rule: same day, and
`thisStart < otherEnd && otherStart < thisEnd`. `ConflictDetectionService`
pulls every existing entry for the candidate's instructor and the
candidate's room on that day, dedupes them (an entry can match both
lookups), excludes the entry being updated if this is an update, and
checks each remaining one with `overlaps`. It's deliberately kept as
its own class, independent of Spring's web or persistence concerns,
so `ScheduleEntryOverlapsTest` and `ConflictDetectionServiceTest` can
exercise it with zero database or HTTP involved.

## A note on verification

I wasn't able to compile or run this project inside my own sandbox —
Spring Boot 3 needs `jakarta.*` packages that aren't available through
apt here, and Maven Central isn't reachable from this environment. I
reviewed every file by hand (brace/paren balance, record field order
against every call site, Spring Data method-name-to-query resolution)
but this is the one project in the portfolio series I couldn't
actually execute before handing it to you. Please run `mvn test`
first thing — if anything fails, tell me the exact output and I'll
fix it.

## Possible extensions

- Flyway migrations instead of `ddl-auto: update`, for real schema
  version control
- Spring Security with role-based access (only a coordinator can
  create/delete; instructors can only view)
- A `/api/schedule-entries/weekly-view?instructorId=` endpoint that
  returns a full week's grid instead of a flat paginated list
- Recurring exceptions (holidays, one-off room changes) layered on
  top of the base weekly schedule

## License

MIT
