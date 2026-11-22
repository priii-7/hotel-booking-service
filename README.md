# Hotel Search & Booking Service

Backend for searching hotels and booking rooms without ever overbooking, even under concurrent load.
Java 17, Spring Boot 3, PostgreSQL, Redis, Kafka, Docker, GitHub Actions.

## Run
```bash
docker compose up --build        # app on :8080
mvn verify                       # tests (needs Docker for Testcontainers)
k6 run loadtest/search.js        # load test (put your numbers below)
```

## API
```bash
curl "localhost:8080/api/hotels?city=Delhi&minRating=4&maxPrice=10000&page=0&size=10"
curl "localhost:8080/api/hotels/1/availability?checkIn=2027-01-15&checkOut=2027-01-18"
curl -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' \
  -d '{"hotelId":1,"guestEmail":"a@b.com","checkIn":"2027-01-15","checkOut":"2027-01-18"}'
curl localhost:8080/api/analytics/bookings
```
Responses: 201 booked, 409 sold out, 404 unknown hotel/dates, 400 bad input, 429 rate limited.

## Architecture
```
client -> RateLimitFilter -> Controllers
   search:  SearchService -> Redis (60s TTL, cache-aside) -> Postgres
   booking: BookingService -> Postgres (one transaction) -> after commit -> Kafka "booking-events" -> AnalyticsConsumer
```

## Key design decisions
| Problem | Solution | Why |
|---|---|---|
| Two users book the last room | Inventory per (hotel, night). Conditional `UPDATE ... WHERE booked_rooms < total_rooms`, check row count == nights | Atomic at the DB. No app-level locks, so it works across many app instances |
| Multi-night stay, one night full | Throw, transaction rolls back all increments | All-or-nothing |
| Deadlocks on overlapping stays | `SELECT ... ORDER BY stay_date FOR UPDATE` first | Every transaction locks rows in the same order |
| Last-resort safety | `CHECK (booked_rooms <= total_rooms)` | Even a code bug cannot overbook |
| Redis down | Cache errors are caught and logged, request falls through to Postgres | Cache is an optimisation, not a dependency |
| Stale cache | 60s TTL; availability endpoint is never cached | Search results tolerate staleness, availability does not |
| Event for a rolled-back booking | Publish with `AFTER_COMMIT` | Never announce what did not happen |
| Event ordering per hotel | Kafka key = hotelId | Same partition, ordered |

## Known limitations (and what I'd do next)
- Event lost if the app crashes between commit and Kafka send -> transactional outbox.
- Rate limiter is per-instance, in-memory -> Redis token bucket or API gateway.
- No payment, hold/expiry or cancellation flow -> add a `PENDING` state with TTL, release inventory on cancel.
- No idempotency key on POST /bookings -> add `Idempotency-Key` header + unique column.
- Analytics are in-memory -> persist to a table.
- Search is one SQL query -> at scale use read replicas / Elasticsearch, partition inventory by date.

## Results
Search endpoint load test (k6, 200 concurrent users, 45s ramp+hold).
Everything ran on one laptop in Docker, including k6 itself:
~1,550 req/s, median 48 ms, p95 104 ms, 99.96% success (43 failed of 116,483)
Concurrency test: 40 threads competing for 5 rooms -> exactly 5 bookings, 35 rejected.

