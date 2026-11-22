package com.example.hotels;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HotelRepository {
    private final JdbcTemplate jdbc;

    public HotelRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Hotel> search(String city, BigDecimal maxPrice, BigDecimal minRating, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, name, city, price_per_night, rating FROM hotels WHERE lower(city) = lower(?)");
        List<Object> args = new ArrayList<>();
        args.add(city);
        if (maxPrice != null) { sql.append(" AND price_per_night <= ?"); args.add(maxPrice); }
        if (minRating != null) { sql.append(" AND rating >= ?"); args.add(minRating); }
        sql.append(" ORDER BY rating DESC, id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(),
                (rs, i) -> new Hotel(rs.getLong("id"), rs.getString("name"), rs.getString("city"),
                        rs.getBigDecimal("price_per_night"), rs.getBigDecimal("rating")),
                args.toArray());
    }

    /** Rooms free on every night in [checkIn, checkOut). Returns 0 if any night has no inventory row. */
    public int roomsAvailable(long hotelId, LocalDate checkIn, LocalDate checkOut) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        return jdbc.query(
                "SELECT COUNT(*) AS n, COALESCE(MIN(total_rooms - booked_rooms), 0) AS free " +
                "FROM room_inventory WHERE hotel_id = ? AND stay_date >= ? AND stay_date < ?",
                rs -> {
                    rs.next();
                    return rs.getLong("n") == nights ? rs.getInt("free") : 0;
                },
                hotelId, Date.valueOf(checkIn), Date.valueOf(checkOut));
    }
}
