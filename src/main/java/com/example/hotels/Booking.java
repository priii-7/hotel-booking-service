package com.example.hotels;

import java.time.LocalDate;
import java.util.UUID;

public record Booking(UUID id, long hotelId, String guestEmail, LocalDate checkIn, LocalDate checkOut) {}
