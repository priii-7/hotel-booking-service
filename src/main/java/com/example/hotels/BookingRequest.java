package com.example.hotels;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingRequest(
        @NotNull Long hotelId,
        @NotBlank @Email String guestEmail,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull LocalDate checkOut) {}
