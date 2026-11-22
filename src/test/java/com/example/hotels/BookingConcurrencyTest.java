package com.example.hotels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.data.redis.timeout=200ms",
        "spring.kafka.producer.properties.max.block.ms=300"
})
@Testcontainers
class BookingConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired BookingService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void neverOverbooksUnderConcurrentRequests() throws Exception {
        jdbc.update("UPDATE room_inventory SET total_rooms = 5, booked_rooms = 0 WHERE hotel_id = 1");
        LocalDate in = LocalDate.now().plusDays(5);
        LocalDate out = in.plusDays(3);

        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    service.book(new BookingRequest(1L, "guest" + n + "@example.com", in, out));
                    ok.incrementAndGet();
                } catch (SoldOutException e) {
                    soldOut.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        assertEquals(5, ok.get(), "exactly 5 rooms exist, so exactly 5 bookings must succeed");
        assertEquals(threads - 5, soldOut.get());
        Integer maxBooked = jdbc.queryForObject(
                "SELECT MAX(booked_rooms) FROM room_inventory WHERE hotel_id = 1 AND stay_date >= ? AND stay_date < ?",
                Integer.class, Date.valueOf(in), Date.valueOf(out));
        assertEquals(5, maxBooked);
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE hotel_id = 1 AND check_in = ?", Integer.class, Date.valueOf(in));
        assertEquals(5, rows);
    }

    @Test
    void sellingOutOneNightRollsBackTheWholeStay() {
        jdbc.update("UPDATE room_inventory SET total_rooms = 10, booked_rooms = 0 WHERE hotel_id = 2");
        LocalDate in = LocalDate.now().plusDays(20);
        LocalDate out = in.plusDays(3);
        // middle night is full
        jdbc.update("UPDATE room_inventory SET total_rooms = 1, booked_rooms = 1 WHERE hotel_id = 2 AND stay_date = ?",
                Date.valueOf(in.plusDays(1)));

        assertThrows(SoldOutException.class,
                () -> service.book(new BookingRequest(2L, "x@example.com", in, out)));

        Integer touched = jdbc.queryForObject(
                "SELECT COUNT(*) FROM room_inventory WHERE hotel_id = 2 AND stay_date IN (?, ?) AND booked_rooms > 0",
                Integer.class, Date.valueOf(in), Date.valueOf(in.plusDays(2)));
        assertEquals(0, touched, "partial increments must be rolled back");
    }
}
