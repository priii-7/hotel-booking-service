package com.example.hotels;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {
    private final BookingService bookings;
    private final AnalyticsConsumer analytics;

    public BookingController(BookingService bookings, AnalyticsConsumer analytics) {
        this.bookings = bookings;
        this.analytics = analytics;
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public Booking book(@Valid @RequestBody BookingRequest request) {
        return bookings.book(request);
    }

    @GetMapping("/api/analytics/bookings")
    public Map<Long, Long> bookingsPerHotel() {
        return analytics.snapshot();
    }
}
