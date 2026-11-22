package com.example.hotels;

import java.math.BigDecimal;

public record Hotel(long id, String name, String city, BigDecimal pricePerNight, BigDecimal rating) {}
