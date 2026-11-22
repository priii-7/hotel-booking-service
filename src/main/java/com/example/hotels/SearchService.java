package com.example.hotels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Cache-aside search. Redis is an optimisation only: any Redis failure falls through to Postgres. */
@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final Duration TTL = Duration.ofSeconds(60);

    private final HotelRepository repo;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public SearchService(HotelRepository repo, StringRedisTemplate redis, ObjectMapper mapper) {
        this.repo = repo;
        this.redis = redis;
        this.mapper = mapper;
    }

    public List<Hotel> search(String city, BigDecimal maxPrice, BigDecimal minRating, int page, int size) {
        String key = "search:%s:%s:%s:%d:%d".formatted(city.toLowerCase(), maxPrice, minRating, page, size);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return mapper.readValue(cached, new TypeReference<List<Hotel>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read failed, falling back to DB: {}", e.toString());
        }
        List<Hotel> result = repo.search(city, maxPrice, minRating, size, page * size);
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.toString());
        }
        return result;
    }
}
