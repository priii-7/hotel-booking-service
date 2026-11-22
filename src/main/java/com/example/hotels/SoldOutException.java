package com.example.hotels;

public class SoldOutException extends RuntimeException {
    public SoldOutException(String msg) { super(msg); }
}
