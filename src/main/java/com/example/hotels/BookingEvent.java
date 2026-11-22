package com.example.hotels;

import java.time.LocalDate;
import java.util.UUID;

public record BookingEvent(UUID bookingId, long hotelId, String guestEmail, LocalDate checkIn, LocalDate checkOut) {}
