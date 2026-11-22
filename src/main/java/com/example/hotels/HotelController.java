package com.example.hotels;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotelController {
    private final SearchService search;
    private final HotelRepository repo;

    public HotelController(SearchService search, HotelRepository repo) {
        this.search = search;
        this.repo = repo;
    }

    @GetMapping("/api/hotels")
    public List<Hotel> search(@RequestParam String city,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @RequestParam(required = false) BigDecimal minRating,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page must be >= 0 and size between 1 and 50");
        }
        return search.search(city, maxPrice, minRating, page, size);
    }

    /** Never cached: availability must be fresh. */
    @GetMapping("/api/hotels/{id}/availability")
    public Map<String, Object> availability(@PathVariable long id,
                                            @RequestParam LocalDate checkIn,
                                            @RequestParam LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        return Map.of("hotelId", id, "checkIn", checkIn, "checkOut", checkOut,
                "roomsAvailable", repo.roomsAvailable(id, checkIn, checkOut));
    }
}
