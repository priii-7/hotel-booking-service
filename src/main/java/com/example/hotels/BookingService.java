package com.example.hotels;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;

    public BookingService(JdbcTemplate jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    /**
     * Books one room for every night in [checkIn, checkOut). All-or-nothing.
     * 1) Lock the inventory rows in a fixed order (stay_date ASC) so overlapping bookings cannot deadlock.
     * 2) Conditional UPDATE only increments rows that still have capacity. If any night is full the
     *    row count is short, we throw, and the transaction rolls back every partial increment.
     * 3) CHECK (booked_rooms <= total_rooms) in the schema is a last-resort safety net.
     */
    @Transactional
    public Booking book(BookingRequest req) {
        LocalDate in = req.checkIn();
        LocalDate out = req.checkOut();
        long nights = ChronoUnit.DAYS.between(in, out);
        if (nights < 1 || nights > 30) {
            throw new IllegalArgumentException("Stay must be between 1 and 30 nights");
        }

        List<LocalDate> locked = jdbc.query(
                "SELECT stay_date FROM room_inventory WHERE hotel_id = ? AND stay_date >= ? AND stay_date < ? " +
                "ORDER BY stay_date FOR UPDATE",
                (rs, i) -> rs.getDate("stay_date").toLocalDate(),
                req.hotelId(), Date.valueOf(in), Date.valueOf(out));
        if (locked.size() != nights) {
            throw new NotFoundException("Hotel not found or dates outside bookable range");
        }

        int updated = jdbc.update(
                "UPDATE room_inventory SET booked_rooms = booked_rooms + 1 " +
                "WHERE hotel_id = ? AND stay_date >= ? AND stay_date < ? AND booked_rooms < total_rooms",
                req.hotelId(), Date.valueOf(in), Date.valueOf(out));
        if (updated != nights) {
            throw new SoldOutException("No rooms available for all requested nights");
        }

        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO bookings (id, hotel_id, guest_email, check_in, check_out) VALUES (?, ?, ?, ?, ?)",
                id, req.hotelId(), req.guestEmail(), Date.valueOf(in), Date.valueOf(out));

        Booking booking = new Booking(id, req.hotelId(), req.guestEmail(), in, out);
        events.publishEvent(new BookingEvent(id, req.hotelId(), req.guestEmail(), in, out));
        return booking;
    }
}
