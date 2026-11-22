package com.example.hotels;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes to Kafka only AFTER the DB transaction commits, so we never announce a booking that rolled back.
 * Known gap: if the app crashes between commit and send, the event is lost. Production fix: transactional outbox.
 */
@Component
public class BookingEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;

    public BookingEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper,
                                 @Value("${app.kafka.topic}") String topic) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBooking(BookingEvent event) {
        try {
            // key = hotelId so all events for one hotel land on one partition, in order
            kafka.send(topic, String.valueOf(event.hotelId()), mapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish booking event {}: {}", event.bookingId(), e.toString());
        }
    }
}
