package com.example.hotels;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Counts bookings per hotel from the event stream. In-memory for the demo; swap for a table or ClickHouse. */
@Component
public class AnalyticsConsumer {
    private final Map<Long, LongAdder> counts = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public AnalyticsConsumer(ObjectMapper mapper) { this.mapper = mapper; }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "analytics")
    public void onMessage(String payload) throws Exception {
        BookingEvent e = mapper.readValue(payload, BookingEvent.class);
        counts.computeIfAbsent(e.hotelId(), k -> new LongAdder()).increment();
    }

    public Map<Long, Long> snapshot() {
        return counts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
    }
}
